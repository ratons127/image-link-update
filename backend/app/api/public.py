from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db.session import get_db
from app.models.models import File
from app.schemas.files import PublicFileResponse
from app.storage.local_storage import storage

router = APIRouter(prefix="/public")


@router.get("/{shareToken}", response_model=PublicFileResponse)
def get_public_file(shareToken: str, db: Session = Depends(get_db)):
    file = db.scalar(select(File).where(File.share_token == shareToken))
    if not file or file.revoked:
        raise HTTPException(status_code=404, detail="Link is invalid or revoked")
    if file.privacy == "PRIVATE":
        raise HTTPException(status_code=403, detail="Not available")
    file.views += 1
    db.commit()
    url = storage.public_url(file.storage_key) if file.storage_key else None
    return PublicFileResponse(
        id=file.id,
        name=file.filename,
        mimeType=file.mime_type,
        sizeBytes=file.size_bytes,
        shareToken=file.share_token,
        allowDownloads=file.download_enabled,
        revoked=file.revoked,
        viewUrl=url if file.mime_type and (file.mime_type.startswith("image/") or file.mime_type.startswith("video/")) else None,
        downloadUrl=url if file.download_enabled else None,
    )
