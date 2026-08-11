from typing import Dict, List, Optional

from app.db import get_conn


ANALYSIS_COLUMNS = """
    kor_name, inci_name, synonyms, cas_no, function_kor, origin,
    usage_limit, caution, description, source, inci_functions,
    irritancy_potential, comedogenicity_rating, safety_score,
    pregnancy_safe, pregnancy_notes, is_allergen
"""


def _normalize_boolean(value) -> Optional[bool]:
    if value is None:
        return None
    return bool(value)


def _find_ingredient(cur, name: str) -> Optional[Dict]:
    cur.execute(
        f"""
        SELECT {ANALYSIS_COLUMNS}
        FROM ingredients
        WHERE UPPER(TRIM(inci_name)) = UPPER(%s)
           OR UPPER(TRIM(kor_name)) = UPPER(%s)
        LIMIT 1
        """,
        (name, name),
    )
    row = cur.fetchone()
    if row is not None:
        return row

    cur.execute(
        f"""
        SELECT {ANALYSIS_COLUMNS}
        FROM ingredients
        WHERE UPPER(inci_name) LIKE CONCAT('%%', UPPER(%s), '%%')
           OR UPPER(kor_name) LIKE CONCAT('%%', UPPER(%s), '%%')
           OR UPPER(synonyms) LIKE CONCAT('%%', UPPER(%s), '%%')
        """,
        (name, name, name),
    )
    candidates = cur.fetchall()
    return next(
        (candidate for candidate in candidates
         if candidate.get("pregnancy_safe") in (False, 0)),
        candidates[0] if candidates else None,
    )


def analyze_ingredients(ingredients: List[str]) -> Dict:
    analyzed = []
    unknown = []

    with get_conn() as conn:
        cur = conn.cursor(dictionary=True)
        for raw_name in ingredients:
            name = raw_name.strip()
            if not name:
                continue

            row = _find_ingredient(cur, name)
            if row is None:
                unknown.append(name)
                continue

            analyzed.append(
                {
                    "input": name,
                    "korName": row.get("kor_name"),
                    "inciName": row.get("inci_name"),
                    "synonyms": row.get("synonyms"),
                    "casNo": row.get("cas_no"),
                    "function": row.get("inci_functions") or row.get("function_kor"),
                    "origin": row.get("origin"),
                    "usageLimit": row.get("usage_limit"),
                    "caution": row.get("caution"),
                    "description": row.get("description"),
                    "irritancyPotential": row.get("irritancy_potential"),
                    "comedogenicityRating": row.get("comedogenicity_rating"),
                    "safetyScore": row.get("safety_score"),
                    "pregnancySafe": _normalize_boolean(row.get("pregnancy_safe")),
                    "pregnancyNotes": row.get("pregnancy_notes"),
                    "allergen": _normalize_boolean(row.get("is_allergen")),
                    "source": row.get("source"),
                }
            )

    caution_count = sum(
        1 for item in analyzed
        if item["pregnancySafe"] is False
        or item["allergen"] is True
        or bool(item["caution"])
    )
    return {
        "totalChecked": len([name for name in ingredients if name.strip()]),
        "matchedCount": len(analyzed),
        "cautionCount": caution_count,
        "analyzedIngredients": analyzed,
        "unknownIngredients": unknown,
    }
