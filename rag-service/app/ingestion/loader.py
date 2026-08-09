"""
식약처 공공데이터포털 화장품 원료 3종 API를 표준명(한글명) 기준으로 조인해서
표준 스키마로 변환.

실제 확인된 필드 (2026-08 기준, data.go.kr):

- 15111772 화장품 사용제한 원료정보 (getCsmtcsRstrctMaterialInfoService)
  순번, 구분, 표준명, 영문명, CASNo, 이명, 배합제한국가, 고시원료명, 단서조항, 제한사항

- 15111773 화장품 규제정보 (getCsmtcsReglMaterialInfoService)
  순번, 표준명, 영문명, 금지국가, 제한국가

- 15111774 화장품 원료성분정보 (getCsmtcsIngrdMaterialInfoService)
  순번, 표준명, 영문명, CASNo, 기원 및 정의, 이명

세 API 모두 오픈API(XML/JSON) 형태로 제공되며, CSV로 미리 받아둔 경우
공공데이터포털 다운로드 파일의 헤더도 대체로 위 필드명과 동일합니다.
(파일을 열어서 실제 헤더가 다르면 아래 COLUMN_MAP만 고치면 됩니다.)
"""
import json
import pandas as pd
from pathlib import Path
from typing import List, Dict, Any


def _extract_records(raw: Any) -> List[Dict]:
    """
    공공데이터포털 JSON은 보통 단순 배열이 아니라
    {"response": {"body": {"items": {"item": [ {...}, ... ]}}}} 형태로 감싸져 있습니다.
    이 함수가 어떤 형태로 오든 실제 레코드 리스트를 찾아서 꺼내줍니다.
    """
    if isinstance(raw, list):
        return raw

    if isinstance(raw, dict):
        # 가장 흔한 패턴: response.body.items.item
        for path in [
            ["response", "body", "items", "item"],
            ["response", "body", "items"],
            ["body", "items", "item"],
            ["items", "item"],
            ["items"],
        ]:
            node = raw
            ok = True
            for key in path:
                if isinstance(node, dict) and key in node:
                    node = node[key]
                else:
                    ok = False
                    break
            if ok:
                if isinstance(node, dict):  # item이 1개뿐이면 dict로 오는 경우 있음
                    return [node]
                if isinstance(node, list):
                    return node

    raise ValueError(
        "JSON 구조에서 레코드 리스트를 찾지 못했습니다. "
        "파일을 열어 실제 구조를 확인하고 _extract_records()의 path를 추가해주세요."
    )


def _load_raw(path: str) -> pd.DataFrame:
    """CSV/JSON 확장자를 보고 자동으로 읽어서 DataFrame으로 변환"""
    suffix = Path(path).suffix.lower()

    if suffix == ".csv":
        return pd.read_csv(path, encoding="utf-8-sig")

    if suffix == ".json":
        with open(path, "r", encoding="utf-8-sig") as f:
            raw = json.load(f)
        records = _extract_records(raw)
        return pd.json_normalize(records)

    raise ValueError(f"지원하지 않는 파일 형식입니다: {suffix} (csv 또는 json만 가능)")


def debug_columns(path: str):
    """실제 컬럼명이 예상과 다를 때 확인용 - COLUMN_MAP 수정 전에 이걸로 확인하세요"""
    df = _load_raw(path)
    print(f"[{path}] 컬럼: {list(df.columns)}")
    print(df.head(2))
    return list(df.columns)


# 원료성분정보(15111774, getCsmtcsIngdCpntInfoService01) 기준 - 실제 확인된 필드명
INGRD_COLUMN_MAP = {
    "INGR_KOR_NAME": "kor_name",
    "INGR_ENG_NAME": "inci_name",
    "CAS_NO": "cas_no",
    "INGR_SYNONYM": "synonyms",
    "ORIGIN_MAJOR_KOR_NAME": "description_raw",
}

RSTRCT_COLUMN_MAP = {
    "INGR_STD_NAME": "kor_name",
    "REGULATE_TYPE": "restrict_category",
    "COUNTRY_NAME": "restrict_countries",
    "NOTICE_INGR_NAME": "notice_name",
    "PROVIS_ATRCL": "proviso",
    "LIMIT_COND": "usage_limit",
}

REGL_COLUMN_MAP = {
    "INGR_STD_NAME": "kor_name",
    "PROH_NATIONAL": "banned_countries",
    "LIMIT_NATIONAL": "restricted_countries",
}


def _load_and_rename(path: str, column_map: Dict[str, str]) -> pd.DataFrame:
    df = _load_raw(path)
    df = df.rename(columns=column_map)
    keep = [c for c in column_map.values() if c in df.columns]
    if not keep:
        raise ValueError(
            f"{path}: COLUMN_MAP과 일치하는 컬럼이 하나도 없습니다. "
            f"실제 컬럼명: {list(df.columns)} — debug_columns('{path}')로 먼저 확인해보세요."
        )
    return df[keep].fillna("")


def load_ingredients_csv(
    ingrd_path: str,
    rstrct_path: str = None,
    regl_path: str = None,
) -> List[Dict]:
    """
    ingrd_path(원료성분정보, 15111774)를 기준(base)으로 하고,
    rstrct_path(사용제한, 15111772), regl_path(규제정보, 15111773)를
    표준명 기준으로 left join.
    사용제한/규제정보 파일이 없으면 원료성분정보만으로 진행.

    csv, json 확장자 둘 다 지원 (자동 감지). 세 파일을 굳이 같은 형식으로
    안 맞춰도 됨 (예: ingrd만 json, 나머지는 csv 이런 조합도 가능).
    """
    base = _load_and_rename(ingrd_path, INGRD_COLUMN_MAP)

    if rstrct_path:
        rstrct = _load_and_rename(rstrct_path, RSTRCT_COLUMN_MAP)
        base = base.merge(rstrct, on="kor_name", how="left")

    if regl_path:
        regl = _load_and_rename(regl_path, REGL_COLUMN_MAP)
        base = base.merge(regl, on="kor_name", how="left")

    base = base.fillna("")
    records = base.to_dict(orient="records")

    for r in records:
        r["description"] = _build_description(r)
        r["function_kor"] = r.get("restrict_category", "")
        r["origin"] = ""  # 이 3개 API엔 유래(동/식물성) 필드가 없음 - 필요시 별도 소스 추가
        r["caution"] = r.get("proviso", "")
        r["source"] = "식약처 화장품 원료성분/사용제한/규제정보 (data.go.kr)"

    return records


def _build_description(r: Dict) -> str:
    """검색/임베딩 품질을 위해 필드들을 자연어 문장으로 합성"""
    parts = []
    if r.get("inci_name") or r.get("kor_name"):
        parts.append(f"{r.get('kor_name', '')}({r.get('inci_name', '')})")
    if r.get("synonyms"):
        parts.append(f"이명: {r['synonyms']}")
    if r.get("description_raw"):
        parts.append(f"기원 및 정의: {r['description_raw']}")
    if r.get("usage_limit"):
        parts.append(f"사용제한사항: {r['usage_limit']}")
    if r.get("restrict_countries"):
        parts.append(f"배합제한국가: {r['restrict_countries']}")
    if r.get("banned_countries"):
        parts.append(f"사용금지국가: {r['banned_countries']}")
    if r.get("restricted_countries"):
        parts.append(f"사용제한국가: {r['restricted_countries']}")
    if r.get("proviso"):
        parts.append(f"단서조항: {r['proviso']}")
    return " / ".join(parts)
