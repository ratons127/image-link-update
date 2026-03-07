from html import escape

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import HTMLResponse
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db.session import get_db
from app.models.models import File
from app.schemas.files import PublicFileResponse
from app.storage.local_storage import storage

router = APIRouter(prefix="/public")
share_router = APIRouter()


def get_shareable_file(share_token: str, db: Session) -> File:
    file = db.scalar(select(File).where(File.share_token == share_token))
    if not file or file.revoked:
        raise HTTPException(status_code=404, detail="Link is invalid or revoked")
    if file.privacy == "PRIVATE":
        raise HTTPException(status_code=403, detail="Not available")
    return file


@router.get("/{shareToken}", response_model=PublicFileResponse)
def get_public_file(shareToken: str, db: Session = Depends(get_db)):
    file = get_shareable_file(shareToken, db)
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


@share_router.get("/s/{shareToken}", response_class=HTMLResponse)
def view_share_page(shareToken: str, db: Session = Depends(get_db)):
    if not settings.public_pages_enabled:
        raise HTTPException(status_code=404, detail="Public pages are disabled")

    file = get_shareable_file(shareToken, db)
    file.views += 1
    db.commit()

    file_url = storage.public_url(file.storage_key) if file.storage_key else None
    title = escape(file.filename)
    mime = (file.mime_type or "").lower()
    can_preview = bool(file_url and (mime.startswith("image/") or mime.startswith("video/")))
    can_download = bool(file_url and file.download_enabled)

    preview_html = "<p>No inline preview available for this file.</p>"
    if can_preview and mime.startswith("image/"):
        preview_html = f'<img src="{escape(file_url)}" alt="{title}" style="max-width:100%;height:auto;border-radius:10px;" />'
    elif can_preview and mime.startswith("video/"):
        preview_html = f'<video controls style="max-width:100%;border-radius:10px;"><source src="{escape(file_url)}" type="{escape(file.mime_type or "video/mp4")}" /></video>'

    download_html = ""
    if can_download:
        download_html = f'<p><a href="{escape(file_url)}" download style="display:inline-block;padding:10px 14px;background:#111;color:#fff;border-radius:8px;text-decoration:none;">Download</a></p>'

    body = f"""<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>{title}</title>
</head>
<body style="font-family:Arial,sans-serif;margin:24px;background:#f8f9fb;color:#111;">
  <div style="max-width:760px;margin:0 auto;background:#fff;padding:20px;border-radius:12px;">
    <h1 style="margin-top:0;">{title}</h1>
    <p style="color:#555;">Shared via Qtiqo Share</p>
    {preview_html}
    {download_html}
  </div>
</body>
</html>"""
    return HTMLResponse(content=body)
