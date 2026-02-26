from pydantic import BaseModel


class FileDto(BaseModel):
    id: str
    name: str
    mimeType: str | None
    sizeBytes: int
    privacy: str
    downloadEnabled: bool
    shareToken: str | None
    shareUrl: str | None
    createdAt: int
    uploadedAt: int | None
    revoked: bool


class InitFileUploadRequest(BaseModel):
    fileName: str
    mimeType: str | None = None
    sizeBytes: int
    privacy: str = "PUBLIC"
    downloadEnabled: bool = True


class InitFileUploadResponse(BaseModel):
    fileId: str
    uploadUrl: str
    shareToken: str


class CompleteFileUploadRequest(BaseModel):
    fileId: str


class PatchFileRequest(BaseModel):
    privacy: str | None = None
    downloadEnabled: bool | None = None


class PagedFilesResponse(BaseModel):
    items: list[FileDto]
    page: int
    pageSize: int
    total: int


class RevokeRegenerateResponse(BaseModel):
    shareToken: str
    shareUrl: str


class PublicFileResponse(BaseModel):
    id: str
    name: str
    mimeType: str | None
    sizeBytes: int
    shareToken: str
    allowDownloads: bool
    revoked: bool
    viewUrl: str | None = None
    downloadUrl: str | None = None
