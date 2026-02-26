from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.core.security import create_access_token, hash_password, verify_password
from app.db.session import get_db
from app.models.models import User
from app.schemas.auth import AuthResponse, ForgotRequest, LoginRequest, LogoutRequest, SignUpRequest

router = APIRouter(prefix="/auth")


@router.post("/signup", response_model=AuthResponse)
def signup(request: SignUpRequest, db: Session = Depends(get_db)):
    identifier = request.identifier.strip()
    existing = db.scalar(select(User).where(User.email == identifier))
    if existing:
        raise HTTPException(status_code=400, detail="User already exists")
    user = User(
        name=identifier.split("@")[0] if "@" in identifier else identifier,
        email=identifier,
        phone=request.phone,
        password_hash=hash_password(request.password),
        role="ADMIN" if identifier.lower() == "admin" else "USER",
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return AuthResponse(token=create_access_token(user.id, user.email, user.role), userId=user.id, role=user.role)


@router.post("/login", response_model=AuthResponse)
def login(request: LoginRequest, db: Session = Depends(get_db)):
    identifier = request.identifier.strip()
    user = db.scalar(select(User).where(or_(User.email == identifier, User.name == identifier)))
    if not user or not verify_password(request.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return AuthResponse(token=create_access_token(user.id, user.email, user.role), userId=user.id, role=user.role)


@router.post("/forgot")
def forgot(_: ForgotRequest):
    return {"message": "Reset request received"}


@router.post("/logout")
def logout(_: LogoutRequest):
    return {"message": "Logged out"}
