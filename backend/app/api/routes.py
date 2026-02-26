from fastapi import APIRouter

from app.api import auth, files, public, profile, admin, uploads, content

api_router = APIRouter()
api_router.include_router(auth.router, tags=["auth"])
api_router.include_router(files.router, tags=["files"])
api_router.include_router(uploads.router, tags=["uploads"])
api_router.include_router(public.router, tags=["public"])
api_router.include_router(content.router, tags=["content"])
api_router.include_router(profile.router, tags=["profile"])
api_router.include_router(admin.router, tags=["admin"])
