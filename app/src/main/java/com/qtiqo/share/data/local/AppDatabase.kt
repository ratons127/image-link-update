package com.qtiqo.share.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qtiqo.share.data.local.dao.UploadDao
import com.qtiqo.share.data.local.entity.UploadEntity
import com.qtiqo.share.util.RoomConverters

@Database(
    entities = [UploadEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun uploadDao(): UploadDao
}
