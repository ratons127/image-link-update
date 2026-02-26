from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_env: str = "development"
    app_name: str = "Qtiqo Share Backend"
    base_url: str = "https://imagelink.qtiqo.com"
    api_port: int = 8080

    database_url: str
    jwt_secret: str
    jwt_expire_minutes: int = 60 * 24 * 30

    storage_mode: str = "local"
    local_storage_dir: str = "/data/uploads"
    public_storage_base_url: str = "https://imagelink.qtiqo.com/content"

    minio_endpoint: str = "minio:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin"
    minio_secure: bool = False
    minio_bucket: str = "imagelink-files"

    default_storage_limit_bytes: int = 100 * 1024 * 1024
    default_upload_limit_bytes: int = 10 * 1024 * 1024
    downloads_enabled_by_default: bool = True
    public_pages_enabled: bool = True
    rate_limit_per_minute: int = 120
    captcha_enabled: bool = False


settings = Settings()
