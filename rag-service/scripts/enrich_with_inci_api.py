"""
INCI API(inciapi.com)로 우리 DB에 이미 있는 성분(영문명 기준)들을
자극도/효능/임신안전성 등으로 보강하는 스크립트.

GET /v1/ingredients/:inciName 를 성분마다 1번씩 호출합니다.
-> 21,863개 전부 돌리면 무료 플랜(월 20,000회) 한도를 넘을 수 있어요.
   실패(404, 매칭 안 됨)는 과금 안 되지만, 성공 매칭만 20,000건까지 무료입니다.

전략: 일단 "많이 쓰이는/유명한 성분" 위주로 먼저 채우고 싶다면
--limit으로 개수를 제한해서 우선순위 높은 것부터 테스트하는 걸 추천.
(예: 이미 있는 21,863건 중 INCI API가 실제로 데이터를 갖고 있을 법한
 상위 N개만 우선 진행)

사용법:
    python scripts/enrich_with_inci_api.py \
        --api-key sk_live_xxx \
        --db-host 127.0.0.1 --db-port 3307 \
        --db-user rag_user --db-password rag_password --db-name cosmetic_rag \
        --limit 2000
"""
import argparse
import time
import requests
import mariadb

BASE_URL = "https://inciapi.com/v1/ingredients"


def fetch_ingredient_info(inci_name: str, api_key: str):
    """성분 하나 조회. 못 찾으면 None 리턴 (과금 안 됨)."""
    resp = requests.get(
        f"{BASE_URL}/{inci_name}",
        headers={"X-API-Key": api_key},
        timeout=15,
    )
    if resp.status_code == 404:
        return None
    if resp.status_code == 429:
        raise RuntimeError("INCI API 요청 한도 초과 (429). 나중에 다시 시도하거나 플랜을 업그레이드하세요.")
    resp.raise_for_status()
    return resp.json().get("ingredient")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-key", required=True)
    parser.add_argument("--db-host", default="127.0.0.1")
    parser.add_argument("--db-port", type=int, default=3307)
    parser.add_argument("--db-user", default="rag_user")
    parser.add_argument("--db-password", default="rag_password")
    parser.add_argument("--db-name", default="cosmetic_rag")
    parser.add_argument("--limit", type=int, default=None, help="테스트용으로 앞에서 N개만 처리")
    parser.add_argument("--sleep", type=float, default=0.2)
    args = parser.parse_args()

    conn = mariadb.connect(
        host=args.db_host, port=args.db_port,
        user=args.db_user, password=args.db_password, database=args.db_name,
    )
    cur = conn.cursor()

    # inci_name이 비어있지 않은 것만 (한글명만 있고 영문명 없는 성분은 INCI API 매칭 불가)
    query = "SELECT id, inci_name FROM ingredients WHERE inci_name IS NOT NULL AND inci_name != ''"
    if args.limit:
        query += f" LIMIT {args.limit}"
    cur.execute(query)
    rows = cur.fetchall()

    print(f"총 {len(rows)}건 처리 예정")

    matched = 0
    for i, (row_id, inci_name) in enumerate(rows):
        # 콤마로 여러 이름이 붙은 경우 첫 번째 이름만 사용
        primary_name = inci_name.split(",")[0].strip()
        if not primary_name:
            continue

        try:
            info = fetch_ingredient_info(primary_name, args.api_key)
        except RuntimeError as e:
            print(f"중단: {e}")
            break

        if info:
            matched += 1
            update_cur = conn.cursor()
            update_cur.execute(
                """
                UPDATE ingredients SET
                    inci_functions = %s,
                    irritancy_potential = %s,
                    comedogenicity_rating = %s,
                    safety_score = %s,
                    pregnancy_safe = %s,
                    pregnancy_notes = %s,
                    is_allergen = %s
                WHERE id = %s
                """,
                (
                    ", ".join(info.get("functions", []) or []),
                    info.get("irritancyPotential"),
                    info.get("comedogenicityRating"),
                    info.get("safetyScore"),
                    info.get("pregnancySafe"),
                    info.get("pregnancyNotes"),
                    info.get("isEuAllergen", False),
                    row_id,
                ),
            )
            conn.commit()

        if (i + 1) % 100 == 0:
            print(f"[enrich] {i + 1}/{len(rows)} 처리 (매칭 {matched}건)")

        time.sleep(args.sleep)

    print(f"\n완료: 총 {len(rows)}건 중 {matched}건 매칭되어 보강됨")
    conn.close()


if __name__ == "__main__":
    main()
