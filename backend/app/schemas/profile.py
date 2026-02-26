from pydantic import BaseModel


class MeSummaryResponse(BaseModel):
    filesCount: int
    storageUsedBytes: int
    storageLimitBytes: int
    uploadLimitBytes: int


class ChangePasswordRequest(BaseModel):
    currentPassword: str
    newPassword: str
