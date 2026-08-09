"""
fetch_pregnancy_unsafe.py로 받은 data/pregnancy_unsafe.json을
우리 ingredients 테이블에 매칭해서 pregnancy_safe=FALSE로 마킹합니다.

매칭 전략: inci_name이 정확히 일치하거나, synonyms(이명) 안에 포함되는 경우.
(우리 DB의 inci_name엔 여러 개 이름이 콤마로 붙은 경우도 있어서 LIKE로 부분 매칭)

사용법:
    python scripts/apply_pregnancy_flags.py \
        --json-path data/pregnancy_unsafe.json \
        --db-host 127.0.0.1 --db-port 3307 \
        --db-user rag_user --db-password rag_password --db-name cosmetic_rag
"""
import argparse
import json
import mariadb


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--json-path", required=True)
    parser.add_argument("--db-host", default="127.0.0.1")
    parser.add_argument("--db-port", type=int, default=3307)
    parser.add_argument("--db-user", default="rag_user")
    parser.add_argument("--db-password", default="rag_password")
    parser.add_argument("--db-name", default="cosmetic_rag")
    args = parser.parse_args()

    with open(args.json_path, "r", encoding="utf-8") as f:
        unsafe_list = json.load(f)

    conn = mariadb.connect(
        host=args.db_host, port=args.db_port,
        user=args.db_user, password=args.db_password, database=args.db_name,
    )
    cur = conn.cursor()

    matched_total = 0
    not_found = []

    for ing in unsafe_list:
        name = ing.get("inciName", "").strip()
        if not name:
            continue
        notes = ing.get("pregnancyNotes") or "임신 중 사용 주의 성분으로 분류됨 (INCI API)"

        cur.execute(
            """
            UPDATE ingredients
            SET pregnancy_safe = FALSE, pregnancy_notes = %s
            WHERE UPPER(inci_name) LIKE CONCAT('%%', UPPER(%s), '%%')
               OR UPPER(synonyms) LIKE CONCAT('%%', UPPER(%s), '%%')
            """,
            (notes, name, name),
        )
        affected = cur.rowcount
        if affected > 0:
            matched_total += affected
        else:
            not_found.append(name)

    conn.commit()

    print(f"완료: {len(unsafe_list)}개 위험 성분 중 DB에서 {matched_total}개 행 매칭되어 마킹됨")
    if not_found:
        print(f"\nDB에서 못 찾은 성분 ({len(not_found)}개, 우리 공공데이터엔 없는 이름일 수 있음):")
        for n in not_found[:20]:
            print(f"  - {n}")
        if len(not_found) > 20:
            print(f"  ... 외 {len(not_found) - 20}개")

    conn.close()


if __name__ == "__main__":
    main()
