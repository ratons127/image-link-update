package com.qtiqo.share.util

import androidx.room.TypeConverter
import com.qtiqo.share.domain.model.FilePrivacy
import com.qtiqo.share.domain.model.UploadStatus

class RoomConverters {
    @TypeConverter
    fun toUploadStatus(value: String): UploadStatus = UploadStatus.valueOf(value)

    @TypeConverter
    fun fromUploadStatus(value: UploadStatus): String = value.name

    @TypeConverter
    fun toPrivacy(value: String): FilePrivacy = FilePrivacy.valueOf(value)

    @TypeConverter
    fun fromPrivacy(value: FilePrivacy): String = value.name
}
