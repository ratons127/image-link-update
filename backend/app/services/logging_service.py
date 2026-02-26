from sqlalchemy.orm import Session

from app.models.models import AdminLog


def add_admin_log(
    db: Session,
    event_type: str,
    actor_user_id: str,
    actor_email: str,
    target_type: str,
    target_id: str,
    message: str,
    ip: str | None = None,
    device: str | None = None,
):
    db.add(
        AdminLog(
            event_type=event_type,
            actor_user_id=actor_user_id,
            actor_email=actor_email,
            target_type=target_type,
            target_id=target_id,
            message=message,
            ip=ip,
            device=device,
        )
    )
