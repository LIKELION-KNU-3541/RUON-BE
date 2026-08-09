import json
from typing import List, Dict
from app.db import get_conn
from app.ingestion.embedder import embed_texts

SELECT_COLS = """
    id, inci_name, kor_name, synonyms, cas_no, function_kor,
    origin, usage_limit, caution, description, source
"""


def vector_search(query: str, top_k: int = 10) -> List[Dict]:
    vec_str = json.dumps(embed_texts([query])[0])
    sql = f"""
        SELECT {SELECT_COLS},
               VEC_DISTANCE_COSINE(embedding, VEC_FromText(%s)) AS dist
        FROM ingredients
        ORDER BY dist ASC
        LIMIT %s
    """
    with get_conn() as conn:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql, (vec_str, top_k))
        return cur.fetchall()


def keyword_search(query: str, top_k: int = 10) -> List[Dict]:
    sql = f"""
        SELECT {SELECT_COLS},
               MATCH(inci_name, kor_name, synonyms, description)
                   AGAINST (%s IN NATURAL LANGUAGE MODE) AS score
        FROM ingredients
        WHERE MATCH(inci_name, kor_name, synonyms, description)
                   AGAINST (%s IN NATURAL LANGUAGE MODE)
        ORDER BY score DESC
        LIMIT %s
    """
    with get_conn() as conn:
        cur = conn.cursor(dictionary=True)
        cur.execute(sql, (query, query, top_k))
        return cur.fetchall()


def hybrid_search(query: str, top_k: int = 8, rrf_k: int = 60) -> List[Dict]:
    """Reciprocal Rank Fusion으로 벡터 검색 + 키워드 검색 결합"""
    vec_results = vector_search(query, top_k=20)
    kw_results = keyword_search(query, top_k=20)

    scores: Dict[int, float] = {}
    rows: Dict[int, Dict] = {}

    for rank, row in enumerate(vec_results):
        scores[row["id"]] = scores.get(row["id"], 0) + 1 / (rrf_k + rank + 1)
        rows[row["id"]] = row

    for rank, row in enumerate(kw_results):
        scores[row["id"]] = scores.get(row["id"], 0) + 1 / (rrf_k + rank + 1)
        rows[row["id"]] = row

    ranked_ids = sorted(scores.keys(), key=lambda i: scores[i], reverse=True)
    return [rows[i] for i in ranked_ids[:top_k]]
