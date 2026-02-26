from pydantic import BaseModel, EmailStr


class SignUpRequest(BaseModel):
    identifier: str
    password: str
    phone: str | None = None


class LoginRequest(BaseModel):
    identifier: str
    password: str


class ForgotRequest(BaseModel):
    identifier: str


class LogoutRequest(BaseModel):
    token: str | None = None


class AuthResponse(BaseModel):
    token: str
    userId: str
    role: str
