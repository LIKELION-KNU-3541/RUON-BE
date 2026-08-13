"""INCI API의 functions만 ingredients.inci_functions에 저장한다.

임신 안전성, 알레르기, 자극도, 안전성 점수 등 다른 컬럼은 변경하지 않는다.
동일한 INCI 이름은 한 번만 조회하며 API 결과를 로컬 파일에 캐시한다.

사용 예시:
    python scripts/enrich_functions_only.py --limit 10
"""

import argparse
import json
import os
import time
from collections import defaultdict
from pathlib import Path
from urllib.parse import quote

import mariadb
import requests
from dotenv import load_dotenv


ROOT_DIR = Path(__file__).resolve().parent.parent
CACHE_PATH = ROOT_DIR / "data" / ".inci_functions_api_cache.json"
BASE_URL = "https://inciapi.com/v1/ingredients"

load_dotenv(ROOT_DIR / ".env")


def parse_args():
    parser = argparse.ArgumentParser(
        description="INCI API의 기능 정보만 RAG DB에 보강합니다."
    )
    parser.add_argument(
        "--api-key",
        default=os.getenv("INCI_API_KEY"),
        help="생략하면 rag-service/.env의 INCI_API_KEY를 사용합니다.",
    )
    parser.add_argument("--db-host", default="127.0.0.1")
    parser.add_argument("--db-port", type=int, default=3307)
    parser.add_argument("--db-user", default="rag_user")
    parser.add_argument("--db-password", default="rag_password")
    parser.add_argument("--db-name", default="cosmetic_rag")
    parser.add_argument(
        "--limit",
        type=int,
        default=10,
        help="이번 실행에서 허용할 신규 API 요청 수입니다. 캐시 조회는 포함하지 않습니다.",
    )
    parser.add_argument("--sleep", type=float, default=0.3)
    return parser.parse_args()


def load_cache():
    if not CACHE_PATH.exists():
        return {}
    try:
        with CACHE_PATH.open("r", encoding="utf-8") as file:
            return json.load(file)
    except (OSError, json.JSONDecodeError) as error:
        raise RuntimeError(f"API 캐시 파일을 읽지 못했습니다: {CACHE_PATH}") from error


def save_cache(cache):
    CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = CACHE_PATH.with_suffix(".tmp")
    with temporary_path.open("w", encoding="utf-8") as file:
        json.dump(cache, file, ensure_ascii=False, indent=2)
    temporary_path.replace(CACHE_PATH)


def normalize_primary_name(inci_name):
    """쉼표로 이름이 여러 개면 API 조회에는 첫 번째 이름을 사용한다."""
    if not inci_name:
        return ""
    return inci_name.split(",", 1)[0].strip()


def normalize_functions(functions):
    """문자열 기능명만 남기고 대소문자 기준 중복을 제거한다."""
    normalized = []
    seen = set()
    for value in functions or []:
        if not isinstance(value, str):
            continue
        function_name = value.strip()
        key = function_name.casefold()
        if not function_name or key in seen:
            continue
        seen.add(key)
        normalized.append(function_name)
    return normalized


def fetch_functions(inci_name, api_key):
    encoded_name = quote(inci_name, safe="")
    response = requests.get(
        f"{BASE_URL}/{encoded_name}",
        headers={"X-API-Key": api_key},
        timeout=15,
    )
    if response.status_code == 404:
        return None
    if response.status_code == 429:
        raise RuntimeError("INCI API 요청 한도를 초과했습니다(429).")
    response.raise_for_status()
    ingredient = response.json().get("ingredient") or {}
    return normalize_functions(ingredient.get("functions"))


def update_functions(cursor, row_ids, functions):
    """inci_functions 하나만 갱신하며 이미 채워진 행은 덮어쓰지 않는다."""
    if not functions:
        return 0

    functions_text = ", ".join(functions)
    updated_count = 0
    for row_id in row_ids:
        cursor.execute(
            """
            UPDATE ingredients
               SET inci_functions = ?
             WHERE id = ?
               AND (inci_functions IS NULL OR TRIM(inci_functions) = '')
            """,
            (functions_text, row_id),
        )
        updated_count += max(cursor.rowcount, 0)
    return updated_count


def group_rows_by_primary_name(rows):
    row_ids_by_name = defaultdict(list)
    display_names = {}
    for row_id, inci_name in rows:
        primary_name = normalize_primary_name(inci_name)
        if not primary_name:
            continue
        cache_key = primary_name.casefold()
        row_ids_by_name[cache_key].append(row_id)
        display_names[cache_key] = primary_name
    return row_ids_by_name, display_names


def main():
    args = parse_args()
    if not args.api_key:
        raise RuntimeError(
            "INCI_API_KEY가 없습니다. rag-service/.env에 키를 설정해주세요."
        )
    if args.limit < 1:
        raise ValueError("--limit은 1 이상이어야 합니다.")
    if args.sleep < 0:
        raise ValueError("--sleep은 0 이상이어야 합니다.")

    connection = mariadb.connect(
        host=args.db_host,
        port=args.db_port,
        user=args.db_user,
        password=args.db_password,
        database=args.db_name,
    )
    select_cursor = connection.cursor()
    update_cursor = connection.cursor()
    cache = load_cache()
    api_request_count = 0
    matched_name_count = 0
    updated_row_count = 0

    try:
        select_cursor.execute(
            """
            SELECT id, inci_name
              FROM ingredients
             WHERE inci_name IS NOT NULL
               AND TRIM(inci_name) <> ''
               AND (inci_functions IS NULL OR TRIM(inci_functions) = '')
             ORDER BY id
            """
        )
        rows = select_cursor.fetchall()
        row_ids_by_name, display_names = group_rows_by_primary_name(rows)

        for cache_key, row_ids in row_ids_by_name.items():
            inci_name = display_names[cache_key]

            if cache_key in cache:
                functions = cache[cache_key]
            else:
                if api_request_count >= args.limit:
                    break
                try:
                    functions = fetch_functions(inci_name, args.api_key)
                except requests.RequestException as error:
                    print(f"[요청 실패] {inci_name}: {error}")
                    continue

                api_request_count += 1
                cache[cache_key] = functions
                save_cache(cache)
                time.sleep(args.sleep)

            if not functions:
                print(f"[기능 정보 없음] {inci_name}")
                continue

            updated = update_functions(update_cursor, row_ids, functions)
            connection.commit()
            matched_name_count += 1
            updated_row_count += updated
            print(
                f"[업데이트] {inci_name}: {', '.join(functions)} "
                f"({updated}개 행)"
            )
    finally:
        save_cache(cache)
        select_cursor.close()
        update_cursor.close()
        connection.close()

    print()
    print(f"신규 API 요청: {api_request_count}회")
    print(f"기능 정보 확인 성분명: {matched_name_count}개")
    print(f"업데이트된 DB 행: {updated_row_count}개")


if __name__ == "__main__":
    main()
