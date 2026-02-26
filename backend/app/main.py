from fastapi import FastAPI

from app.api.routes import api_router
from app.db.session import engine
from app.db.base import Base
from app.services.seed import seed_initial_data


app = FastAPI(title="Qtiqo Share Backend")


@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)
    seed_initial_data()


@app.get("/health")
def health():
    return {"status": "ok"}


app.include_router(api_router)
