from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse
from pathlib import Path
from starlette.background import BackgroundTask
from starlette.responses import StreamingResponse

from app.storage.local_storage import storage

router = APIRouter(prefix="/content")


@router.get("/{storage_key}")
def serve_content(storage_key: str):
    safe_name = Path(storage_key).name
    if safe_name != storage_key:
        raise HTTPException(status_code=400, detail="Invalid path")
    if storage.uses_minio:
        try:
            obj = storage.get_minio_object(safe_name)
        except Exception:
            raise HTTPException(status_code=404, detail="File not found")
        return StreamingResponse(
            obj.stream(1024 * 1024),
            media_type="application/octet-stream",
            background=BackgroundTask(lambda: (obj.close(), obj.release_conn())),
        )
    path = storage.open_local_file(safe_name)
    if not path.exists():
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(path)
