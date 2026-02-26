from pydantic import BaseModel


class AdminStatsResponse(BaseModel):
    totalUsers: int
    totalFiles: int
    totalStorageUsedBytes: int
    totalViews: int


class AdminUserDto(BaseModel):
    id: str
    name: str
    email: str
    phone: str | None
    role: str
    plan: str = "FREE"
    isSuspended: bool
    storageUsedBytes: int
    storageLimitBytes: int
    maxUploadSizeBytes: int
    fileCount: int
    createdAt: int


class PagedUsersResponse(BaseModel):
    items: list[AdminUserDto]
    nextPage: int | None


class CreateAdminUserRequest(BaseModel):
    name: str
    email: str
    phone: str | None = None
    password: str
    role: str = "USER"
    storageLimitBytes: int
    maxUploadSizeBytes: int


class PatchAdminUserRequest(BaseModel):
    role: str | None = None
    isSuspended: bool | None = None
    storageLimitBytes: int | None = None
    maxUploadSizeBytes: int | None = None


class AdminFileDto(BaseModel):
    id: str
    ownerId: str
    ownerEmail: str
    filename: str
    mimeType: str | None
    sizeBytes: int
    privacy: str
    downloadEnabled: bool
    views: int
    createdAt: int
    shareToken: str | None = None
    shareUrl: str | None = None
    previewUrl: str | None = None
    isRevoked: bool = False


class PagedFilesResponse(BaseModel):
    items: list[AdminFileDto]
    nextPage: int | None


class PatchAdminFileRequest(BaseModel):
    privacy: str | None = None
    downloadEnabled: bool | None = None


class AdminLogDto(BaseModel):
    id: str
    eventType: str
    actorUserId: str
    actorEmail: str
    targetType: str
    targetId: str
    message: str
    ip: str | None
    device: str | None
    createdAt: int


class PagedLogsResponse(BaseModel):
    items: list[AdminLogDto]
    nextPage: int | None


class AdminSettingsDto(BaseModel):
    defaultStorageLimitBytes: int
    maxUploadSizeBytes: int
    downloadsEnabledByDefault: bool
    publicPagesEnabled: bool
    rateLimitPerMinute: int
    captchaEnabled: bool
