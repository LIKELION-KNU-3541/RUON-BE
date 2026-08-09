from openai import OpenAI
from typing import List, Dict
from app.config import settings
from app.retrieval.hybrid_search import hybrid_search

client = OpenAI(api_key=settings.OPENAI_API_KEY)

SYSTEM_PROMPT = """당신은 화장품 성분 분석 전문가입니다.
아래 [검색된 성분 정보]만을 근거로 답변하세요.

규칙:
- 검색된 정보에 없는 내용은 추측하지 말고 "제공된 정보만으로는 확인이 어렵습니다"라고 답하세요.
- 안전성/부작용 관련 주장은 근거가 명확한 경우에만 언급하세요.
- 검색된 성분 정보 중 "임신 중 안전성"이 "주의 필요"로 표시된 성분이 있다면,
  질문 의도와 무관하게 답변에 반드시 별도로 언급하세요 (사용자가 임신 여부를 밝히지 않았어도).
- 답변 끝에 참고한 성분명을 출처로 표기하세요.
"""


def format_context(rows: List[Dict]) -> str:
    blocks = []
    for r in rows:
        blocks.append(
            f"- 성분명: {r.get('kor_name', '')}({r.get('inci_name', '')})\n"
            f"  기능: {r.get('function_kor', '')}\n"
            f"  유래: {r.get('origin', '')}\n"
            f"  배합한도: {r.get('usage_limit', '')}\n"
            f"  주의사항: {r.get('caution', '')}\n"
            f"  설명: {r.get('description', '')}"
        )
    return "\n\n".join(blocks)


def answer_question(query: str, top_k: int = 6) -> Dict:
    retrieved = hybrid_search(query, top_k=top_k)
    context = format_context(retrieved)

    resp = client.chat.completions.create(
        model=settings.CHAT_MODEL,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": f"[검색된 성분 정보]\n{context}\n\n[질문]\n{query}",
            },
        ],
        temperature=0.2,
    )

    return {
        "answer": resp.choices[0].message.content,
        "sources": [
            {"inci_name": r["inci_name"], "kor_name": r["kor_name"]}
            for r in retrieved
        ],
    }
