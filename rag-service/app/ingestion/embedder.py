import json
from typing import List, Dict
from openai import OpenAI
from tenacity import retry, wait_exponential, stop_after_attempt

from app.config import settings
from app.db import get_conn

client = OpenAI(api_key=settings.OPENAI_API_KEY)


@retry(wait=wait_exponential(min=1, max=20), stop=stop_after_attempt(5))
def embed_texts(texts: List[str]) -> List[List[float]]:
    """여러 텍스트를 한 번의 API 호출로 임베딩 (훨씬 빠름)"""
    resp = client.embeddings.create(model=settings.EMBEDDING_MODEL, input=texts)
    return [d.embedding for d in resp.data]


def embed_and_insert(records: List[Dict], batch_size: int = 100):
    with get_conn() as conn:
        cur = conn.cursor()
        for i in range(0, len(records), batch_size):
            batch = records[i : i + batch_size]
            texts = [r["description"] for r in batch]
            vectors = embed_texts(texts)  # API 호출 1번으로 batch_size개 한번에 처리

            for r, vec in zip(batch, vectors):
                vec_str = json.dumps(vec)  # VEC_FromText 입력용 '[0.1,0.2,...]'

                cur.execute(
                    """
                    INSERT INTO ingredients
                        (inci_name, kor_name, synonyms, cas_no, function_kor,
                         origin, usage_limit, caution, description, source, embedding)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, VEC_FromText(%s))
                    """,
                    (
                        r.get("inci_name", ""),
                        r.get("kor_name", ""),
                        r.get("synonyms", ""),
                        r.get("cas_no", ""),
                        r.get("function_kor", ""),
                        r.get("origin", ""),
                        r.get("usage_limit", ""),
                        r.get("caution", ""),
                        r.get("description", ""),
                        r.get("source", ""),
                        vec_str,
                    ),
                )
            conn.commit()
            print(f"[embed_and_insert] {i + len(batch)}/{len(records)} 완료")
