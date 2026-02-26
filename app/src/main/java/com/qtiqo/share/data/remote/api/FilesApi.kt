package com.qtiqo.share.data.remote.api

import com.qtiqo.share.data.remote.dto.CompleteFileUploadRequest
import com.qtiqo.share.data.remote.dto.FileDto
import com.qtiqo.share.data.remote.dto.InitFileUploadRequest
import com.qtiqo.share.data.remote.dto.InitFileUploadResponse
import com.qtiqo.share.data.remote.dto.PagedFilesResponse
import com.qtiqo.share.data.remote.dto.PatchFileRequest
import com.qtiqo.share.data.remote.dto.RevokeRegenerateResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface FilesApi {
    @POST("/files/init")
    suspend fun initUpload(@Body request: InitFileUploadRequest): InitFileUploadResponse

    @POST("/files/complete")
    suspend fun completeUpload(@Body request: CompleteFileUploadRequest)

    @GET("/files")
    suspend fun getFiles(
        @Query("search") search: String? = null,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): PagedFilesResponse

    @GET("/files/{id}")
    suspend fun getFile(@Path("id") id: String): FileDto

    @PATCH("/files/{id}")
    suspend fun patchFile(@Path("id") id: String, @Body request: PatchFileRequest): FileDto

    @POST("/files/{id}/revoke")
    suspend fun revoke(@Path("id") id: String): RevokeRegenerateResponse

    @POST("/files/{id}/regenerate")
    suspend fun regenerate(@Path("id") id: String): RevokeRegenerateResponse
}
