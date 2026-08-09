import mariadb
from contextlib import contextmanager
from app.config import settings


def get_pool():
    return mariadb.ConnectionPool(
        pool_name="cosmetic_rag_pool",
        pool_size=5,
        host=settings.DB_HOST,
        port=settings.DB_PORT,
        user=settings.DB_USER,
        password=settings.DB_PASSWORD,
        database=settings.DB_NAME,
    )


_pool = None


@contextmanager
def get_conn():
    global _pool
    if _pool is None:
        _pool = get_pool()
    conn = _pool.get_connection()
    try:
        yield conn
    finally:
        conn.close()
