"""
공공데이터포털(data.go.kr) 오픈API를 호출해서 전체 데이터를 JSON 파일로 저장하는 스크립트.
"""
import argparse
import json
import time
import requests


def fetch_all(base_url: str, service_key: str, num_of_rows: int = 100, sleep_sec: float = 0.2) -> list:
    all_items = []
    page_no = 1

    while True:
        params = {
            "serviceKey": service_key,
            "pageNo": page_no,
            "numOfRows": num_of_rows,
            "type": "json",
        }
        resp = requests.get(base_url, params=params, timeout=15)
        resp.raise_for_status()

        try:
            data = resp.json()
        except ValueError:
            raise RuntimeError(
                f"JSON 파싱 실패. 응답 원문 일부: {resp.text[:500]}\n"
                "-> 대부분 인증키 문제(Encoding/Decoding 혼동)이거나 서비스 미승인 상태입니다."
            )

        body = data.get("response", data).get("body", {})
        if not body:
            print("\n--- 서버 원본 응답 (디버깅용) ---")
            print(json.dumps(data, ensure_ascii=False, indent=2)[:1500])
            print("--- 여기까지 ---\n")
            header = data.get("response", data).get("header", {})
            raise RuntimeError(f"API 에러 응답 또는 예상과 다른 구조: {header}")
        items_node = body.get("items", {})
        items = items_node.get("item", []) if isinstance(items_node, dict) else items_node
        if isinstance(items, dict):
            items = [items]

        if not items:
            break

        all_items.extend(items)

        total_count = int(body.get("totalCount", 0))
        print(f"page {page_no}: {len(items)}건 수집 (누적 {len(all_items)} / 전체 {total_count})")

        if len(all_items) >= total_count or not items:
            break

        page_no += 1
        time.sleep(sleep_sec)

    return all_items


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True, help="API Endpoint URL")
    parser.add_argument("--service-key", required=True, help="Decoding 인증키")
    parser.add_argument("--out", required=True, help="저장할 JSON 파일 경로")
    parser.add_argument("--num-of-rows", type=int, default=100)
    args = parser.parse_args()

    items = fetch_all(args.url, args.service_key, args.num_of_rows)

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=2)

    print(f"\n완료: 총 {len(items)}건을 {args.out}에 저장했습니다.")
    if items:
        print(f"첫 레코드 필드명: {list(items[0].keys())}")


if __name__ == "__main__":
    main()