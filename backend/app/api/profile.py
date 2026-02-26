from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.core.security import verify_password, hash_password
from app.db.session import get_db
from app.models.models import File, User
from app.schemas.profile import ChangePasswordRequest, MeSummaryResponse

router = APIRouter(prefix="/me")


@router.get("/summary", response_model=MeSummaryResponse)
def me_summary(db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    files_count = db.scalar(select(func.count()).select_from(File).where(File.owner_id == user.id)) or 0
    return MeSummaryResponse(
        filesCount=files_count,
        storageUsedBytes=user.storage_used_bytes,
        storageLimitBytes=user.storage_limit_bytes,
        uploadLimitBytes=user.max_upload_size_bytes,
    )


@router.post("/change-password")
def change_password(request: ChangePasswordRequest, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    if not verify_password(request.currentPassword, user.password_hash):
        raise HTTPException(status_code=400, detail="Current password is incorrect")
    if len(request.newPassword) < 8:
        raise HTTPException(status_code=400, detail="New password must be at least 8 characters")
    user.password_hash = hash_password(request.newPassword)
    db.commit()
    return {"message": "Password updated"}
