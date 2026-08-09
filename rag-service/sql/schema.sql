-- 화장품 성분 RAG용 스키마
-- MariaDB 11.7+ VECTOR 타입 사용 (text-embedding-3-small = 1536차원)

CREATE TABLE IF NOT EXISTS ingredients (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    inci_name       VARCHAR(255) NOT NULL,        -- 영문 INCI명
    kor_name        VARCHAR(255),                 -- 한글 성분명
    synonyms        TEXT,                         -- 이명/동의어 (쉼표 구분)
    cas_no          VARCHAR(255),
    function_kor    VARCHAR(255),                 -- 기능 (보습제, 계면활성제 등)
    origin          VARCHAR(50),                  -- 유래 (동물성/식물성/합성 등)
    usage_limit     VARCHAR(255),                 -- 배합 한도
    caution         TEXT,                         -- 주의사항
    description     TEXT,                         -- 성분 설명 (임베딩 원문 소스)
    source          VARCHAR(255),                 -- 데이터 출처
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 벡터 검색용 컬럼 (임베딩 모델 차원에 맞게 조정)
    embedding       VECTOR(1536) NOT NULL,

    FULLTEXT INDEX ft_search (inci_name, kor_name, synonyms, description),
    VECTOR INDEX vec_idx (embedding)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
