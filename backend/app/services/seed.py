from sqlalchemy import select

from app.core.config import settings
from app.core.security import hash_password, random_token
from app.db.session import SessionLocal
from app.models.models import AppSettings, User, File, AdminLog


def seed_initial_data():
    db = SessionLocal()
    try:
        if db.scalar(select(User).limit(1)) is None:
            admin = User(
                name="admin",
                email="admin@qtiqo.com",
                password_hash=hash_password("admin123"),
                role="ADMIN",
                storage_limit_bytes=10 * 1024 * 1024 * 1024,
                max_upload_size_bytes=250 * 1024 * 1024,
            )
            demo = User(
                name="Demo User",
                email="demo@qtiqo.com",
                password_hash=hash_password("demo123"),
                role="USER",
                storage_limit_bytes=settings.default_storage_limit_bytes,
                max_upload_size_bytes=settings.default_upload_limit_bytes,
            )
            db.add_all([admin, demo])
            db.flush()
            for i in range(3):
                token = random_token(16)
                db.add(
                    File(
                        owner_id=demo.id,
                        filename=f"demo_{i+1}.jpg",
                        mime_type="image/jpeg",
                        size_bytes=400_000 + i * 200_000,
                        privacy="PUBLIC",
                        download_enabled=True,
                        views=10 + i,
                        status="DONE",
                        share_token=token,
                        revoked=False,
                    )
                )
            db.add(
                AppSettings(
                    id=1,
                    default_storage_limit_bytes=settings.default_storage_limit_bytes,
                    max_upload_size_bytes=settings.default_upload_limit_bytes,
                    downloads_enabled_by_default=settings.downloads_enabled_by_default,
                    public_pages_enabled=settings.public_pages_enabled,
                    rate_limit_per_minute=settings.rate_limit_per_minute,
                    captcha_enabled=settings.captcha_enabled,
                )
            )
            db.add(
                AdminLog(
                    event_type="LOGIN",
                    actor_user_id=admin.id,
                    actor_email=admin.email,
                    target_type="USER",
                    target_id=admin.id,
                    message="Initial admin seed login event",
                )
            )
            db.commit()
    finally:
        db.close()
