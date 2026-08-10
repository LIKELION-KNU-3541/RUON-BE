"""
INCI API(inciapi.com)에는 '임신 안전성 기준 전체 성분 조회' 엔드포인트가 없어서,
공식 문서에 명시된 임신 위험 카테고리 키워드로 검색 -> pregnancySafe=false인 것만 필터링하는 방식.

문서 근거 (https://inciapi.com/docs):
  pregnancySafe = false ← retinoids, salicylic acid, hydroquinone, oxybenzone,
                           phthalates, triclosan, toluene, formaldehyde releasers

사용법:
    python scripts/fetch_pregnancy_unsafe.py --api-key sk_live_xxx --out data/pregnancy_unsafe.json

주의:
- q= 검색은 이름/기능 텍스트 매칭이라 카테고리를 100% 포괄하지 못할 수 있음 (예: "레티놀"
  계열 파생물이 다 안 걸릴 수 있음). 완전한 커버리지가 필요하면 유료 플랜에서 전체
  성분 export 기능이 있는지 별도 문의 필요.
- Free 플랜은 월 20,000회 요청, 실패(404)는 과금 안 됨.
"""
import argparse
import json
import time
import requests

BASE_URL = "https://inciapi.com/v1/ingredients/search"

# 문서에 명시된 임신 위험 카테고리 검색 키워드
PREGNANCY_RISK_KEYWORDS = [
    "retinol", "retinoid", "retinal", "retinyl", "tretinoin",
    "salicylic acid", "bha",
    "hydroquinone",
    "oxybenzone",
    "phthalate",
    "triclosan",
    "toluene",
    "formaldehyde", "dmdm hydantoin", "quaternium-15",
]


def search_keyword(keyword: str, api_key: str, limit: int = 50) -> list:
    resp = requests.get(
        BASE_URL,
        params={"q": keyword, "limit": limit},
        headers={"X-API-Key": api_key},
        timeout=15,
    )
    if resp.status_code == 404:
        return []
    resp.raise_for_status()
    return resp.json().get("results", [])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-key", required=True, help="INCI API 키 (sk_live_...)")
    parser.add_argument("--out", required=True, help="저장할 JSON 파일 경로")
    args = parser.parse_args()

    unsafe_ingredients = {}  # inciName 기준으로 중복 제거

    for kw in PREGNANCY_RISK_KEYWORDS:
        print(f"검색 중: '{kw}'")
        results = search_keyword(kw, args.api_key)
        for ing in results:
            if ing.get("pregnancySafe") is False:
                unsafe_ingredients[ing["inciName"]] = ing
        time.sleep(0.3)  # 레이트리밋 방지

    final_list = list(unsafe_ingredients.values())

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(final_list, f, ensure_ascii=False, indent=2)

    print(f"\n완료: 임신 중 위험 성분 {len(final_list)}건을 {args.out}에 저장했습니다.")
    for ing in final_list:
        print(f"  - {ing['inciName']} ({ing.get('pregnancyNotes', '사유 미기재')})")


if __name__ == "__main__":
    main()
