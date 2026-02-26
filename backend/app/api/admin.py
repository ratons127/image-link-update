from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.deps import get_admin_user
from app.core.security import hash_password, random_token
from app.db.session import get_db
from app.models.models import AdminLog, AppSettings, File, User
from app.schemas.admin import (
    AdminFileDto,
    AdminLogDto,
    AdminSettingsDto,
    AdminStatsResponse,
    AdminUserDto,
    CreateAdminUserRequest,
    PagedFilesResponse,
    PagedLogsResponse,
    PagedUsersResponse,
    PatchAdminFileRequest,
    PatchAdminUserRequest,
)
from app.storage.local_storage import storage

router = APIRouter(prefix="/admin")


def to_ms(dt):
    return int(dt.timestamp() * 1000)


def user_dto(u: User, file_count: int | None = None) -> AdminUserDto:
    return AdminUserDto(
        id=u.id,
        name=u.name,
        email=u.email,
        phone=u.phone,
        role=u.role,
        isSuspended=u.is_suspended,
        storageUsedBytes=u.storage_used_bytes,
        storageLimitBytes=u.storage_limit_bytes,
        maxUploadSizeBytes=u.max_upload_size_bytes,
        fileCount=file_count if file_count is not None else 0,
        createdAt=to_ms(u.created_at),
    )


def file_dto(f: File) -> AdminFileDto:
    return AdminFileDto(
        id=f.id,
        ownerId=f.owner_id,
        ownerEmail=f.owner.email if f.owner else "",
        filename=f.filename,
        mimeType=f.mime_type,
        sizeBytes=f.size_bytes,
        privacy=f.privacy,
        downloadEnabled=f.download_enabled,
        views=f.views,
        createdAt=to_ms(f.created_at),
        shareToken=f.share_token,
        shareUrl=(f"https://imagelink.qtiqo.com/s/{f.share_token}" if not f.revoked else None),
        previewUrl=(storage.public_url(f.storage_key) if f.storage_key and f.mime_type and (f.mime_type.startswith("image/") or f.mime_type.startswith("video/")) else None),
        isRevoked=f.revoked,
    )


def log_dto(l: AdminLog) -> AdminLogDto:
    return AdminLogDto(
        id=l.id, eventType=l.event_type, actorUserId=l.actor_user_id, actorEmail=l.actor_email,
        targetType=l.target_type, targetId=l.target_id, message=l.message, ip=l.ip, device=l.device,
        createdAt=to_ms(l.created_at)
    )


@router.get("/stats", response_model=AdminStatsResponse)
def stats(db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    return AdminStatsResponse(
        totalUsers=db.scalar(select(func.count()).select_from(User)) or 0,
        totalFiles=db.scalar(select(func.count()).select_from(File)) or 0,
        totalStorageUsedBytes=db.scalar(select(func.coalesce(func.sum(User.storage_used_bytes), 0))) or 0,
        totalViews=db.scalar(select(func.coalesce(func.sum(File.views), 0))) or 0,
    )


@router.get("/users", response_model=PagedUsersResponse)
def list_users(query: str | None = None, page: int = 1, pageSize: int = 20, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    q = select(User)
    if query:
        q = q.where((User.email.ilike(f"%{query}%")) | (User.name.ilike(f"%{query}%")))
    total = db.scalar(select(func.count()).select_from(q.subquery())) or 0
    users = db.scalars(q.order_by(User.created_at.desc()).offset((page - 1) * pageSize).limit(pageSize)).all()
    counts = {row[0]: row[1] for row in db.execute(select(File.owner_id, func.count()).group_by(File.owner_id)).all()}
    next_page = page + 1 if page * pageSize < total else None
    return PagedUsersResponse(items=[user_dto(u, counts.get(u.id, 0)) for u in users], nextPage=next_page)


@router.post("/users", response_model=AdminUserDto)
def create_user(request: CreateAdminUserRequest, db: Session = Depends(get_db), admin: User = Depends(get_admin_user)):
    if db.scalar(select(User).where(User.email == request.email)):
        raise HTTPException(status_code=400, detail="User exists")
    user = User(
        name=request.name,
        email=request.email,
        phone=request.phone,
        password_hash=hash_password(request.password),
        role=request.role,
        storage_limit_bytes=request.storageLimitBytes,
        max_upload_size_bytes=request.maxUploadSizeBytes,
    )
    db.add(user)
    db.flush()
    db.add(AdminLog(event_type="LOGIN", actor_user_id=admin.id, actor_email=admin.email, target_type="USER", target_id=user.id, message=f"Created user {request.email}"))
    db.commit()
    db.refresh(user)
    return user_dto(user, 0)


@router.patch("/users/{user_id}", response_model=AdminUserDto)
def patch_user(user_id: str, request: PatchAdminUserRequest, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    user = db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if request.role is not None:
        user.role = request.role
    if request.isSuspended is not None:
        user.is_suspended = request.isSuspended
    if request.storageLimitBytes is not None:
        user.storage_limit_bytes = request.storageLimitBytes
    if request.maxUploadSizeBytes is not None:
        user.max_upload_size_bytes = request.maxUploadSizeBytes
    db.commit()
    db.refresh(user)
    count = db.scalar(select(func.count()).select_from(File).where(File.owner_id == user.id)) or 0
    return user_dto(user, count)


@router.delete("/users/{user_id}")
def delete_user(user_id: str, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    user = db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    db.query(File).filter(File.owner_id == user_id).delete()
    db.delete(user)
    db.commit()
    return {"message": "Deleted"}


@router.get("/files", response_model=PagedFilesResponse)
def list_admin_files(query: str | None = None, owner: str | None = None, privacy: str | None = None, sort: str | None = None, page: int = 1, pageSize: int = 20, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    q = select(File)
    if query:
        q = q.where(File.filename.ilike(f"%{query}%"))
    if owner:
        q = q.join(User, User.id == File.owner_id).where((User.email.ilike(f"%{owner}%")) | (User.id == owner))
    if privacy:
        q = q.where(File.privacy == privacy)
    total = db.scalar(select(func.count()).select_from(q.subquery())) or 0
    if sort == "OLDEST":
        q = q.order_by(File.created_at.asc())
    elif sort == "LARGEST":
        q = q.order_by(File.size_bytes.desc())
    elif sort == "MOST_VIEWED":
        q = q.order_by(File.views.desc())
    else:
        q = q.order_by(File.created_at.desc())
    rows = db.scalars(q.offset((page - 1) * pageSize).limit(pageSize)).all()
    next_page = page + 1 if page * pageSize < total else None
    return PagedFilesResponse(items=[file_dto(f) for f in rows], nextPage=next_page)


@router.patch("/files/{file_id}", response_model=AdminFileDto)
def patch_admin_file(file_id: str, request: PatchAdminFileRequest, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    file = db.get(File, file_id)
    if not file:
        raise HTTPException(status_code=404, detail="File not found")
    if request.privacy is not None:
        file.privacy = request.privacy
    if request.downloadEnabled is not None:
        file.download_enabled = request.downloadEnabled
    db.commit()
    db.refresh(file)
    return file_dto(file)


@router.delete("/files/{file_id}")
def delete_admin_file(file_id: str, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    file = db.get(File, file_id)
    if not file:
        raise HTTPException(status_code=404, detail="File not found")
    owner = db.get(User, file.owner_id)
    if owner:
        owner.storage_used_bytes = max(0, owner.storage_used_bytes - file.size_bytes)
    db.delete(file)
    db.commit()
    return {"message": "Deleted"}


@router.get("/logs", response_model=PagedLogsResponse)
def list_logs(eventType: str | None = None, actor: str | None = None, target: str | None = None, page: int = 1, pageSize: int = 20, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    q = select(AdminLog)
    if eventType:
        q = q.where(AdminLog.event_type == eventType)
    if actor:
        q = q.where(AdminLog.actor_email.ilike(f"%{actor}%"))
    if target:
        q = q.where(AdminLog.target_id.ilike(f"%{target}%"))
    total = db.scalar(select(func.count()).select_from(q.subquery())) or 0
    items = db.scalars(q.order_by(AdminLog.created_at.desc()).offset((page - 1) * pageSize).limit(pageSize)).all()
    next_page = page + 1 if page * pageSize < total else None
    return PagedLogsResponse(items=[log_dto(l) for l in items], nextPage=next_page)


@router.get("/settings", response_model=AdminSettingsDto)
def get_settings(db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    s = db.get(AppSettings, 1)
    if not s:
        raise HTTPException(status_code=500, detail="Settings not initialized")
    return AdminSettingsDto(
        defaultStorageLimitBytes=s.default_storage_limit_bytes,
        maxUploadSizeBytes=s.max_upload_size_bytes,
        downloadsEnabledByDefault=s.downloads_enabled_by_default,
        publicPagesEnabled=s.public_pages_enabled,
        rateLimitPerMinute=s.rate_limit_per_minute,
        captchaEnabled=s.captcha_enabled,
    )


@router.patch("/settings", response_model=AdminSettingsDto)
def patch_settings(request: AdminSettingsDto, db: Session = Depends(get_db), _: User = Depends(get_admin_user)):
    s = db.get(AppSettings, 1)
    if not s:
        s = AppSettings(id=1)
        db.add(s)
    s.default_storage_limit_bytes = request.defaultStorageLimitBytes
    s.max_upload_size_bytes = request.maxUploadSizeBytes
    s.downloads_enabled_by_default = request.downloadsEnabledByDefault
    s.public_pages_enabled = request.publicPagesEnabled
    s.rate_limit_per_minute = request.rateLimitPerMinute
    s.captcha_enabled = request.captchaEnabled
    db.commit()
    return request
