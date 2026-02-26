package com.qtiqo.share.feature.admin.data.api

import com.qtiqo.share.feature.admin.data.dto.AdminDtos.AdminFileDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.AdminLogDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.AdminSettingsDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.AdminStatsDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.AdminUserDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.CreateUserRequestDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.PagedFilesResponseDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.PagedLogsResponseDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.PagedUsersResponseDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.UpdateFileRequestDto
import com.qtiqo.share.feature.admin.data.dto.AdminDtos.UpdateUserRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @GET("/admin/stats")
    suspend fun getStats(@Header("Authorization") bearer: String): AdminStatsDto

    @GET("/admin/users")
    suspend fun getUsers(
        @Header("Authorization") bearer: String,
        @Query("query") query: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): PagedUsersResponseDto

    @POST("/admin/users")
    suspend fun createUser(@Header("Authorization") bearer: String, @Body request: CreateUserRequestDto): AdminUserDto

    @PATCH("/admin/users/{id}")
    suspend fun updateUser(@Header("Authorization") bearer: String, @Path("id") id: String, @Body request: UpdateUserRequestDto): AdminUserDto

    @DELETE("/admin/users/{id}")
    suspend fun deleteUser(@Header("Authorization") bearer: String, @Path("id") id: String)

    @GET("/admin/files")
    suspend fun getFiles(
        @Header("Authorization") bearer: String,
        @Query("query") query: String?,
        @Query("owner") owner: String?,
        @Query("privacy") privacy: String?,
        @Query("sort") sort: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): PagedFilesResponseDto

    @PATCH("/admin/files/{id}")
    suspend fun updateFile(@Header("Authorization") bearer: String, @Path("id") id: String, @Body request: UpdateFileRequestDto): AdminFileDto

    @DELETE("/admin/files/{id}")
    suspend fun deleteFile(@Header("Authorization") bearer: String, @Path("id") id: String)

    @GET("/admin/logs")
    suspend fun getLogs(
        @Header("Authorization") bearer: String,
        @Query("eventType") eventType: String?,
        @Query("actor") actor: String?,
        @Query("target") target: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): PagedLogsResponseDto

    @GET("/admin/settings")
    suspend fun getSettings(@Header("Authorization") bearer: String): AdminSettingsDto

    @PATCH("/admin/settings")
    suspend fun updateSettings(@Header("Authorization") bearer: String, @Body request: AdminSettingsDto): AdminSettingsDto
}
