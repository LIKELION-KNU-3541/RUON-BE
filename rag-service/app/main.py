from fastapi import FastAPI
from app.api.routes import router

app = FastAPI(title="Cosmetic Ingredient RAG")
app.include_router(router)


@app.get("/health")
def health():
    return {"status": "ok"}
