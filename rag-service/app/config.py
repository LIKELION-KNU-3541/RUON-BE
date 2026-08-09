import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-3-small")
    CHAT_MODEL = os.getenv("CHAT_MODEL", "gpt-4o-mini")
    EMBEDDING_DIM = 1536  # text-embedding-3-small 기준. large(3072) 쓰면 스키마도 변경 필요

    DB_HOST = os.getenv("DB_HOST", "127.0.0.1")
    DB_PORT = int(os.getenv("DB_PORT", 3306))
    DB_USER = os.getenv("DB_USER", "rag_user")
    DB_PASSWORD = os.getenv("DB_PASSWORD", "rag_password")
    DB_NAME = os.getenv("DB_NAME", "cosmetic_rag")


settings = Settings()
