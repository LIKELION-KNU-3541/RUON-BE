"""스캔된 성분의 INCI 기능을 필요할 때만 조회해 DB와 현재 응답에 반영한다."""

import json
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from threading import Lock
from urllib.parse import quote

import requests

from app.config import settings
from app.db import get_conn


logger = logging.getLogger(__name__)
BASE_URL = "https://inciapi.com/v1/ingredients"
ROOT_DIR = Path(__file__).resolve().parents[2]
CACHE_PATH = ROOT_DIR / "data" / ".inci_functions_api_cache.json"
_cache_lock = Lock()


def _primary_name(inci_name: str) -> str:
    return inci_name.split(",", 1)[0].strip() if inci_name else ""


def _normalize_functions(values) -> list[str]:
    functions = []
    seen = set()
    for value in values or []:
        if not isinstance(value, str):
            continue
        function_name = value.strip()
        key = function_name.casefold()
        if not function_name or key in seen:
            continue
        seen.add(key)
        functions.append(function_name)
    return functions


def _load_cache() -> dict:
    with _cache_lock:
        if not CACHE_PATH.exists():
            return {}
        try:
            with CACHE_PATH.open("r", encoding="utf-8") as file:
                return json.load(file)
        except (OSError, json.JSONDecodeError):
            logger.warning("INCI 기능 캐시를 읽지 못해 빈 캐시로 시작합니다: %s", CACHE_PATH)
            return {}


def _save_cache(cache: dict) -> None:
    with _cache_lock:
        CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
        temporary_path = CACHE_PATH.with_suffix(".tmp")
        with temporary_path.open("w", encoding="utf-8") as file:
            json.dump(cache, file, ensure_ascii=False, indent=2)
        temporary_path.replace(CACHE_PATH)


def _fetch_functions(inci_name: str) -> list[str] | None:
    response = requests.get(
        f"{BASE_URL}/{quote(inci_name, safe='')}",
        headers={"X-API-Key": settings.INCI_API_KEY},
        timeout=settings.INCI_API_TIMEOUT_SECONDS,
    )
    if response.status_code == 404:
        return None
    response.raise_for_status()
    ingredient = response.json().get("ingredient") or {}
    return _normalize_functions(ingredient.get("functions"))


def _lookup_functions(names: list[str], cache: dict) -> dict[str, list[str] | None]:
    results = {}
    missing_names = []
    for name in names:
        key = name.casefold()
        if key in cache:
            results[name] = cache[key]
        else:
            missing_names.append(name)

    if not missing_names:
        return results

    worker_count = max(1, min(settings.INCI_ENRICH_MAX_WORKERS, len(missing_names)))
    with ThreadPoolExecutor(max_workers=worker_count) as executor:
        futures = {executor.submit(_fetch_functions, name): name for name in missing_names}
        for future in as_completed(futures):
            name = futures[future]
            try:
                functions = future.result()
            except (requests.RequestException, ValueError, TypeError) as error:
                # 외부 보강 장애 때문에 OCR/RAG 분석 전체가 실패하면 안 된다.
                logger.warning("INCI 기능 조회 실패: inciName=%s, error=%s", name, error)
                continue
            results[name] = functions
            cache[name.casefold()] = functions

    _save_cache(cache)
    return results


def enrich_missing_functions(rows: list[dict]) -> None:
    """조회된 행의 빈 inci_functions만 보강하고 rows에도 즉시 값을 채운다."""
    if not settings.INCI_API_KEY:
        logger.info("INCI_API_KEY가 없어 실시간 기능 보강을 건너뜁니다.")
        return

    rows_by_key = {}
    for row in rows:
        if row.get("inci_functions"):
            continue
        name = _primary_name(row.get("inci_name"))
        if name:
            key = name.casefold()
            group = rows_by_key.setdefault(key, {"name": name, "rows": []})
            group["rows"].append(row)

    if not rows_by_key:
        return

    names = [group["name"] for group in rows_by_key.values()]
    results = _lookup_functions(names, _load_cache())
    updates = []
    for group in rows_by_key.values():
        name = group["name"]
        related_rows = group["rows"]
        functions = results.get(name)
        if not functions:
            continue
        functions_text = ", ".join(functions)
        for row in related_rows:
            row["inci_functions"] = functions_text
            updates.append((functions_text, row["id"]))

    if not updates:
        return

    with get_conn() as conn:
        cursor = conn.cursor()
        try:
            cursor.executemany(
                """
                UPDATE ingredients
                   SET inci_functions = %s
                 WHERE id = %s
                   AND (inci_functions IS NULL OR TRIM(inci_functions) = '')
                """,
                updates,
            )
            conn.commit()
        finally:
            cursor.close()
