from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.models import UploadSession
from app.storage.local_storage import storage

router = APIRouter(prefix="/uploads")


@router.put("/{upload_token}")
async def upload_raw(upload_token: str, request: Request, db: Session = Depends(get_db)):
    upload = db.scalar(select(UploadSession).where(UploadSession.upload_token == upload_token))
    if not upload:
        raise HTTPException(status_code=404, detail="Upload token not found")
    temp_path = storage.temp_path(upload_token)
    total = 0
    with temp_path.open("wb") as f:
        async for chunk in request.stream():
            if not chunk:
                continue
            total += len(chunk)
            if total > upload.expected_size_bytes:
                raise HTTPException(status_code=400, detail="Uploaded size exceeds expected size")
            f.write(chunk)
    if total != upload.expected_size_bytes:
        raise HTTPException(status_code=400, detail="Uploaded size mismatch")
    upload.received_size_bytes = total
    upload.completed = True
    db.commit()
    return {"message": "Uploaded"}
