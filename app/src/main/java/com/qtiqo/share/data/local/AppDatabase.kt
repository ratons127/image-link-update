package com.qtiqo.share.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qtiqo.share.data.local.dao.UploadDao
import com.qtiqo.share.data.local.entity.UploadEntity
import com.qtiqo.share.util.RoomConverters

@Database(
    entities = [UploadEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun uploadDao(): UploadDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE uploads ADD COLUMN ownerIdentifier TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
