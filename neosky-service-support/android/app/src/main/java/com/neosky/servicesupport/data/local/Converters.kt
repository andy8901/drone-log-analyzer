package com.neosky.servicesupport.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

/**
 * Room can't natively persist nested lists (a ticket's timeline/comments/attachments, a
 * drone's components), so those are stored as a JSON string column and decoded back into the
 * domain model directly by the mappers in [com.neosky.servicesupport.data.repository] — Room
 * itself only ever sees the raw JSON via these passthrough converters, keeping this class tiny
 * and not coupled to any specific nested type.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}
