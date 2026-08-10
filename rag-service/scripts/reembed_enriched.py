"""
enrich_with_inci_api.py로 자극도/효능/임신안전성을 채운 뒤,
그 정보가 description(임베딩 원문)에는 반영이 안 돼있는 상태입니다.
(벡터 검색이 "순한 성분" 같은 질문에 반응하려면 이 정보가 임베딩에 들어가야 함)

이 스크립트는 보강된 행만 골라서 description을 다시 합성하고 재임베딩합니다.

사용법:
    python scripts/reembed_enriched.py \
        --db-host 127.0.0.1 --db-port 3307 \
        --db-user rag_user --db-password rag_password --db-name cosmetic_rag
"""
import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import mariadb
from app.ingestion.embedder import embed_texts


def build_description(row: dict) -> str:
    parts = []
    if row.get("kor_name") or row.get("inci_name"):
        parts.append(f"{row.get('kor_name', '')}({row.get('inci_name', '')})")
    if row.get("synonyms"):
        parts.append(f"이명: {row['synonyms']}")
    if row.get("inci_functions"):
        parts.append(f"효능/기능: {row['inci_functions']}")
    if row.get("irritancy_potential"):
        level_kor = {"none": "매우 순함", "low": "순한 편", "moderate": "보통 자극", "high": "자극적"}.get(
            row["irritancy_potential"], row["irritancy_potential"]
        )
        parts.append(f"자극도: {level_kor}")
    if row.get("comedogenicity_rating") is not None:
        parts.append(f"코메도제닉(모공막힘) 등급: {row['comedogenicity_rating']}/5 (낮을수록 순함)")
    if row.get("pregnancy_safe") is not None:
        parts.append("임신 중 안전" if row["pregnancy_safe"] else "임신 중 주의 필요")
        if row.get("pregnancy_notes"):
            parts.append(f"임신 관련: {row['pregnancy_notes']}")
    if row.get("usage_limit"):
        parts.append(f"사용제한사항: {row['usage_limit']}")
    if row.get("banned_countries"):
        parts.append(f"사용금지국가: {row['banned_countries']}")
    if row.get("restricted_countries"):
        parts.append(f"사용제한국가: {row['restricted_countries']}")
    return " / ".join(parts)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--db-host", default="127.0.0.1")
    parser.add_argument("--db-port", type=int, default=3307)
    parser.add_argument("--db-user", default="rag_user")
    parser.add_argument("--db-password", default="rag_password")
    parser.add_argument("--db-name", default="cosmetic_rag")
    parser.add_argument("--batch-size", type=int, default=100)
    args = parser.parse_args()

    conn = mariadb.connect(
        host=args.db_host, port=args.db_port,
        user=args.db_user, password=args.db_password, database=args.db_name,
    )
    cur = conn.cursor(dictionary=True)
    cur.execute(
        """
        SELECT id, kor_name, inci_name, synonyms, inci_functions,
               irritancy_potential, comedogenicity_rating, pregnancy_safe,
               pregnancy_notes, usage_limit, banned_countries, restricted_countries
        FROM ingredients
        WHERE inci_functions IS NOT NULL OR irritancy_potential IS NOT NULL
        """
    )
    rows = cur.fetchall()
    print(f"재임베딩 대상: {len(rows)}건")

    for i in range(0, len(rows), args.batch_size):
        batch = rows[i : i + args.batch_size]
        descriptions = [build_description(r) for r in batch]
        vectors = embed_texts(descriptions)

        update_cur = conn.cursor()
        for r, desc, vec in zip(batch, descriptions, vectors):
            vec_str = json.dumps(vec)
            update_cur.execute(
                "UPDATE ingredients SET description = %s, embedding = VEC_FromText(%s) WHERE id = %s",
                (desc, vec_str, r["id"]),
            )
        conn.commit()
        print(f"[reembed] {i + len(batch)}/{len(rows)} 완료")

    print("완료")
    conn.close()


if __name__ == "__main__":
    main()
