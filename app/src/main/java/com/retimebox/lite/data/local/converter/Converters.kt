package com.retimebox.lite.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retimebox.lite.data.local.entity.ContentReference
import com.retimebox.lite.data.local.entity.MediaType
import com.retimebox.lite.data.local.entity.RefType
import com.retimebox.lite.data.local.entity.SourceType
import com.retimebox.lite.data.local.entity.SpaceType

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)

    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun fromSpaceType(value: SpaceType): String = value.name

    @TypeConverter
    fun toSpaceType(value: String): SpaceType = SpaceType.valueOf(value)

    @TypeConverter
    fun fromRefType(value: RefType): String = value.name

    @TypeConverter
    fun toRefType(value: String): RefType = RefType.valueOf(value)

    @TypeConverter
    fun fromContentReferenceList(value: List<ContentReference>): String =
        gson.toJson(value)

    @TypeConverter
    fun toContentReferenceList(value: String): List<ContentReference> {
        if (value.isBlank()) return emptyList()
        val type = object : TypeToken<List<ContentReference>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromLongList(value: List<Long>): String = gson.toJson(value)

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        if (value.isBlank()) return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
