package com.qtiqo.share.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qtiqo.share.data.local.entity.UploadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDao {
    @Query("SELECT * FROM uploads WHERE ownerIdentifier = :ownerIdentifier ORDER BY createdAt DESC")
    fun observeAll(ownerIdentifier: String): Flow<List<UploadEntity>>

    @Query("SELECT * FROM uploads WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UploadEntity?>

    @Query("SELECT * FROM uploads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UploadEntity?

    @Query("SELECT * FROM uploads WHERE shareToken = :token LIMIT 1")
    suspend fun getByShareToken(token: String): UploadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UploadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<UploadEntity>)

    @Update
    suspend fun update(entity: UploadEntity)

    @Query("DELETE FROM uploads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM uploads WHERE ownerIdentifier = :ownerIdentifier")
    fun observeCount(ownerIdentifier: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM uploads WHERE ownerIdentifier = :ownerIdentifier")
    fun observeTotalSize(ownerIdentifier: String): Flow<Long>
}
