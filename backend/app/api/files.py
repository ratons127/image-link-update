from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.deps import get_current_user
from app.core.security import random_token, stable_storage_key
from app.db.session import get_db
from app.models.models import File, UploadSession, User
from app.schemas.files import (
    CompleteFileUploadRequest,
    FileDto,
    InitFileUploadRequest,
    InitFileUploadResponse,
    PatchFileRequest,
    PagedFilesResponse,
    RevokeRegenerateResponse,
)
from app.storage.local_storage import storage

router = APIRouter(prefix="/files")


def to_ms(dt):
    if not dt:
        return None
    return int(dt.timestamp() * 1000)


def file_to_dto(file: File) -> FileDto:
    return FileDto(
        id=file.id,
        name=file.filename,
        mimeType=file.mime_type,
        sizeBytes=file.size_bytes,
        privacy=file.privacy,
        downloadEnabled=file.download_enabled,
        shareToken=file.share_token,
        shareUrl=f"{settings.base_url.rstrip('/')}/s/{file.share_token}" if not file.revoked else None,
        createdAt=to_ms(file.created_at),
        uploadedAt=to_ms(file.uploaded_at),
        revoked=file.revoked,
    )


@router.post("/init", response_model=InitFileUploadResponse)
def init_upload(request: InitFileUploadRequest, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    if request.sizeBytes > user.max_upload_size_bytes:
        raise HTTPException(status_code=400, detail="Upload exceeds max upload limit")
    if user.storage_used_bytes + request.sizeBytes > user.storage_limit_bytes:
        raise HTTPException(status_code=400, detail="Storage limit exceeded")
    share_token = random_token(16)
    file = File(
        owner_id=user.id,
        filename=request.fileName,
        mime_type=request.mimeType,
        size_bytes=request.sizeBytes,
        privacy=request.privacy,
        download_enabled=request.downloadEnabled,
        status="UPLOADING",
        share_token=share_token,
        revoked=False,
    )
    db.add(file)
    db.flush()
    upload_token = random_token(24)
    temp_path = str(storage.temp_path(upload_token))
    db.add(
        UploadSession(
            file_id=file.id,
            upload_token=upload_token,
            expected_size_bytes=request.sizeBytes,
            temp_path=temp_path,
        )
    )
    db.commit()
    return InitFileUploadResponse(
        fileId=file.id,
        uploadUrl=f"{settings.base_url.rstrip('/')}/uploads/{upload_token}",
        shareToken=share_token,
    )


@router.post("/complete")
def complete_upload(request: CompleteFileUploadRequest, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    file = db.scalar(select(File).where(File.id == request.fileId, File.owner_id == user.id))
    if not file:
        raise HTTPException(status_code=404, detail="File not found")
    upload = db.scalar(select(UploadSession).where(UploadSession.file_id == file.id))
    if not upload or not upload.completed:
        raise HTTPException(status_code=400, detail="Upload not complete")
    final_key = stable_storage_key(file.id, file.filename)
    storage.promote_temp(upload.temp_path, final_key, file.mime_type)
    file.storage_key = final_key
    file.status = "DONE"
    file.uploaded_at = datetime.now(timezone.utc)
    user.storage_used_bytes += file.size_bytes
    db.delete(upload)
    db.commit()
    return {"message": "Upload completed"}


@router.get("", response_model=PagedFilesResponse)
def list_files(
    search: str | None = None,
    filter: str | None = None,
    sort: str | None = None,
    page: int = 1,
    pageSize: int = 20,
    db: Session = Depends(get_db),
    user: User = Depends(get_current_user),
):
    q = select(File).where(File.owner_id == user.id)
    if search:
        q = q.where(File.filename.ilike(f"%{search}%"))
    if filter:
        q = q.where(File.privacy == filter)
    total = db.scalar(select(func.count()).select_from(q.subquery())) or 0
    if sort == "OLDEST":
        q = q.order_by(File.created_at.asc())
    elif sort == "LARGEST":
        q = q.order_by(File.size_bytes.desc())
    elif sort == "SMALLEST":
        q = q.order_by(File.size_bytes.asc())
    else:
        q = q.order_by(File.created_at.desc())
    items = db.scalars(q.offset((page - 1) * pageSize).limit(pageSize)).all()
    return PagedFilesResponse(items=[file_to_dto(f) for f in items], page=page, pageSize=pageSize, total=total)


@router.get("/{file_id}", response_model=FileDto)
def get_file(file_id: str, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    file = db.scalar(select(File).where(File.id == file_id, File.owner_id == user.id))
    if not file:
        raise HTTPException(status_code=404, detail="File not found")
    return file_to_dto(file)


@router.patch("/{file_id}", response_model=FileDto)
def patch_file(file_id: str, request: PatchFileRequest, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    file = db.scalar(select(File).where(File.id == file_id, File.owner_id == user.id))
    if not file:
        raise HTTPException(status_code=404, detail="File not found")
    if request.privacy is not None:
        file.privacy = request.privacy
    if request.downloadEnabled is not None:
        file.download_enabled = request.downloadEnabled
    db.commit()
    db.refresh(file)
    return file_to_dto(file)


@router.post("/{file_id}/revoke", response_model=RevokeRegenerateResponse)
def revoke(file_id: str, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    file = db.scalar(select(File).where(File.id == file_id, File.owner_id == user.id))
    if not file:
        raise HTTPException(status_code=404, detail="File not found")
    file.revoked = True
    db.commit()
    return RevokeRegenerateResponse(shareToken=file.share_token, shareUrl="")


@router.post("/{file_id}/regenerate", response_model=RevokeRegenerateResponse)
def regenerate(file_id: str, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    file = db.scalar(select(File).where(File.id == file_id, File.owner_id == user.id))
    if not file:
        raise HTTPException(status_code=404, detail="File not found")
    file.share_token = random_token(16)
    file.revoked = False
    db.commit()
    return RevokeRegenerateResponse(shareToken=file.share_token, shareUrl=f"{settings.base_url.rstrip('/')}/s/{file.share_token}")
