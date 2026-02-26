import os
from pathlib import Path

from minio import Minio

from app.core.config import settings


class LocalStorage:
    def __init__(self):
        self.base = Path(settings.local_storage_dir)
        self.base.mkdir(parents=True, exist_ok=True)
        (self.base / "tmp").mkdir(parents=True, exist_ok=True)
        (self.base / "files").mkdir(parents=True, exist_ok=True)
        self._minio = None
        if settings.storage_mode.lower() == "minio":
            self._minio = Minio(
                settings.minio_endpoint,
                access_key=settings.minio_access_key,
                secret_key=settings.minio_secret_key,
                secure=settings.minio_secure,
            )
            if not self._minio.bucket_exists(settings.minio_bucket):
                self._minio.make_bucket(settings.minio_bucket)

    def temp_path(self, upload_token: str) -> Path:
        return self.base / "tmp" / upload_token

    def final_path(self, storage_key: str) -> Path:
        return self.base / "files" / storage_key

    def public_url(self, storage_key: str) -> str:
        return f"{settings.public_storage_base_url.rstrip('/')}/{storage_key}"

    @property
    def uses_minio(self) -> bool:
        return self._minio is not None

    def promote_temp(self, temp_path: str, storage_key: str, content_type: str | None = None) -> None:
        if self._minio is None:
            os.replace(temp_path, self.final_path(storage_key))
            return
        self._minio.fput_object(
            settings.minio_bucket,
            storage_key,
            temp_path,
            content_type=content_type or "application/octet-stream",
        )
        try:
            os.remove(temp_path)
        except FileNotFoundError:
            pass

    def open_local_file(self, storage_key: str) -> Path:
        return self.final_path(storage_key)

    def get_minio_object(self, storage_key: str):
        if self._minio is None:
            raise RuntimeError("MinIO is not enabled")
        return self._minio.get_object(settings.minio_bucket, storage_key)


storage = LocalStorage()
