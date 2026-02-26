# Qtiqo Share Backend (Production Foundation)

This backend matches the Android app contracts and is intended to run on Ubuntu 24.

## Stack
- FastAPI
- PostgreSQL
- Docker Compose
- Local file storage (default)
- MinIO service included (ready to integrate)

## Implemented endpoint groups
- `POST /auth/signup`, `/auth/login`, `/auth/forgot`, `/auth/logout`
- `POST /files/init`
- `PUT /uploads/{uploadToken}` (raw bytes upload URL from `/files/init`)
- `POST /files/complete`
- `GET /files`, `GET /files/{id}`, `PATCH /files/{id}`
- `POST /files/{id}/revoke`, `POST /files/{id}/regenerate`
- `GET /public/{shareToken}`
- `GET /me/summary`, `POST /me/change-password`
- `/admin/*` stats/users/files/logs/settings
- `GET /content/{storage_key}` file serving

## Quick start (local / Ubuntu VM)

1. Copy env file:
```bash
cd backend
cp .env.example .env
```

2. Edit secrets in `.env`:
- `DATABASE_URL`
- `JWT_SECRET`
- `BASE_URL=https://imagelink.qtiqo.com`

3. Start services:
```bash
docker compose up -d --build
```

4. Verify:
```bash
curl http://127.0.0.1:8080/health
```

## Seeded users
- Admin: `admin@qtiqo.com` / `admin123`
- Demo: `demo@qtiqo.com` / `demo123`

## Ubuntu 24 production setup (Nginx + SSL)

Install:
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y nginx certbot python3-certbot-nginx
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

Copy Nginx config:
```bash
sudo cp backend/nginx/imagelink.qtiqo.com.conf /etc/nginx/sites-available/imagelink.qtiqo.com
sudo ln -s /etc/nginx/sites-available/imagelink.qtiqo.com /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

Enable HTTPS:
```bash
sudo certbot --nginx -d imagelink.qtiqo.com
```

## Important production notes
- Current storage backend is local disk (`/data/uploads`). For scale, move to S3/MinIO integration.
- No background virus scanning yet.
- No rate limiting enforcement yet (settings are stored, enforcement not implemented).
- `PUT /uploads/{token}` currently buffers request body in memory; switch to streaming for large uploads before production scale.
- Add database migrations (Alembic) before schema changes in production.
- Add backups for PostgreSQL and uploaded files.
