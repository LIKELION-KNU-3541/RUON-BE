from fastapi import APIRouter
from pydantic import BaseModel
from typing import List

from app.retrieval.hybrid_search import hybrid_search
from app.generation.rag_chain import answer_question
from app.ingestion.loader import load_ingredients_csv
from app.ingestion.embedder import embed_and_insert
from app.db import get_conn
from app.analysis.ingredient_analysis import analyze_ingredients

router = APIRouter()


class SearchRequest(BaseModel):
    query: str
    top_k: int = 8


class AnswerRequest(BaseModel):
    query: str
    top_k: int = 6


class IngestRequest(BaseModel):
    ingrd_path: str          # 15111774 화장품 원료성분정보 (필수, 기준 데이터)
    rstrct_path: str = None  # 15111772 화장품 사용제한 원료정보 (선택)
    regl_path: str = None    # 15111773 화장품 규제정보 (선택)


class PregnancyCheckRequest(BaseModel):
    ingredients: List[str]  # 전성분표 리스트 (한글명 또는 영문명 섞여있어도 됨)


class IngredientAnalysisRequest(BaseModel):
    ingredients: List[str]


@router.post("/search")
def search(req: SearchRequest):
    """벡터 + 키워드 하이브리드 검색만 수행 (LLM 호출 없음)"""
    results = hybrid_search(req.query, top_k=req.top_k)
    return {"results": results}


@router.post("/answer")
def answer(req: AnswerRequest):
    """검색 + LLM 답변 생성"""
    return answer_question(req.query, top_k=req.top_k)


@router.post("/ingredient-analysis")
def ingredient_analysis(req: IngredientAnalysisRequest):
    """전성분 목록을 RAG DB와 대조해 성분별 기능·주의·안전성 정보를 반환."""
    return analyze_ingredients(req.ingredients)


@router.post("/ingest")
def ingest(req: IngestRequest):
    """CSV 파일들을 표준명 기준으로 조인 후 임베딩 -> DB 적재 (초기 적재/배치용)"""
    records = load_ingredients_csv(req.ingrd_path, req.rstrct_path, req.regl_path)
    embed_and_insert(records)
    return {"inserted": len(records)}


@router.post("/pregnancy-check")
def pregnancy_check(req: PregnancyCheckRequest):
    """
    전성분표(성분명 리스트)를 넣으면, 그 중 임신 중 주의가 필요한 성분만 찾아서 반환.
    (apply_pregnancy_flags.py로 pregnancy_safe 컬럼을 미리 채워둬야 정상 동작)
    """
    warnings = []
    unknown = []

    with get_conn() as conn:
        cur = conn.cursor(dictionary=True)
        for name in req.ingredients:
            name = name.strip()
            if not name:
                continue
            # 1) 정확히 일치하는 것 우선 (LIKE 부분매칭이 이명 필드에서 다른 성분을 잘못 집어오는 것 방지)
            cur.execute(
                """
                SELECT kor_name, inci_name, pregnancy_safe, pregnancy_notes
                FROM ingredients
                WHERE UPPER(TRIM(inci_name)) = UPPER(%s)
                   OR UPPER(TRIM(kor_name)) = UPPER(%s)
                LIMIT 1
                """,
                (name, name),
            )
            row = cur.fetchone()

            # 2) 정확히 일치하는 게 없으면 부분 매칭으로 폴백 (여러 개 나올 수 있어 전부 확인)
            if row is None:
                cur.execute(
                    """
                    SELECT kor_name, inci_name, pregnancy_safe, pregnancy_notes
                    FROM ingredients
                    WHERE UPPER(inci_name) LIKE CONCAT('%%', UPPER(%s), '%%')
                       OR UPPER(kor_name) LIKE CONCAT('%%', UPPER(%s), '%%')
                       OR UPPER(synonyms) LIKE CONCAT('%%', UPPER(%s), '%%')
                    """,
                    (name, name, name),
                )
                candidates = cur.fetchall()
                # 부분매칭 후보들 중 pregnancy_safe=False인 게 하나라도 있으면 그걸 우선 사용
                row = next((c for c in candidates if c.get("pregnancy_safe") in (False, 0)), None) \
                    or (candidates[0] if candidates else None)

            if row is None:
                unknown.append(name)
            elif row.get("pregnancy_safe") in (False, 0):
                warnings.append(
                    {
                        "input": name,
                        "kor_name": row.get("kor_name"),
                        "inci_name": row.get("inci_name"),
                        "reason": row.get("pregnancy_notes") or "임신 중 주의가 필요한 성분으로 분류됨",
                    }
                )

    return {
        "totalChecked": len(req.ingredients),
        "pregnancySafe": len(warnings) == 0,
        "warnings": warnings,
        "unknownIngredients": unknown,  # DB에서 못 찾은 성분명 (판단 불가 - 이 리스트가 있으면 신중하게 안내해야 함)
    }
