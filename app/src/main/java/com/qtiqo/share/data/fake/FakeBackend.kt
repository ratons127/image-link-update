package com.qtiqo.share.data.fake

import com.qtiqo.share.domain.model.UserRole
import com.qtiqo.share.util.generateShareToken
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class FakeFileRecord(
    val id: String,
    val owner: String,
    var name: String,
    var mimeType: String?,
    var sizeBytes: Long,
    var shareToken: String,
    var privacy: String,
    var downloadEnabled: Boolean,
    var revoked: Boolean,
    var localUri: String?,
    var createdAt: Long,
    var uploadedAt: Long?
)

data class FakeUser(
    val identifier: String,
    val password: String,
    val role: UserRole,
    val phone: String?
)

@Singleton
class FakeBackend @Inject constructor() {
    private val users = ConcurrentHashMap<String, FakeUser>()
    private val files = ConcurrentHashMap<String, FakeFileRecord>()
    private val revokedTokens = mutableSetOf<String>()

    init {
        users["admin"] = FakeUser("admin", "admin123", UserRole.ADMIN, null)
        users["demo@qtiqo.com"] = FakeUser("demo@qtiqo.com", "demo123", UserRole.USER, null)
    }

    @Synchronized
    fun signUp(identifier: String, password: String, phone: String?): Pair<String, UserRole> {
        if (users.containsKey(identifier)) error("User already exists")
        val role = if (identifier.equals("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.USER
        users[identifier] = FakeUser(identifier, password, role, phone)
        return "fake-jwt-${UUID.randomUUID()}" to role
    }

    @Synchronized
    fun login(identifier: String, password: String): Pair<String, UserRole> {
        val user = users[identifier] ?: error("Invalid credentials")
        if (user.password != password) error("Invalid credentials")
        return "fake-jwt-${UUID.randomUUID()}" to user.role
    }

    fun forgot(identifier: String) {
        if (identifier.isBlank()) error("Identifier required")
    }

    @Synchronized
    fun createOrUpdateFile(record: FakeFileRecord) {
        files[record.id] = record
    }

    fun getFile(id: String): FakeFileRecord? = files[id]

    fun listFilesForOwner(owner: String): List<FakeFileRecord> =
        files.values.filter { it.owner == owner }.sortedByDescending { it.createdAt }

    @Synchronized
    fun revoke(fileId: String): FakeFileRecord {
        val file = files[fileId] ?: error("File not found")
        file.revoked = true
        revokedTokens += file.shareToken
        return file
    }

    @Synchronized
    fun regenerate(fileId: String): FakeFileRecord {
        val file = files[fileId] ?: error("File not found")
        revokedTokens += file.shareToken
        file.shareToken = generateShareToken()
        file.revoked = false
        return file
    }

    fun isRevokedToken(token: String): Boolean = token in revokedTokens

    fun findByShareToken(token: String): FakeFileRecord? =
        files.values.firstOrNull { it.shareToken == token }

    fun usersList(): List<FakeUser> = users.values.sortedBy { it.identifier }

    fun fileCount(): Int = files.size

    fun totalStorageBytes(): Long = files.values.sumOf { it.sizeBytes }
}
