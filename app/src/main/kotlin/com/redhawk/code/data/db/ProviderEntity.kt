package com.redhawk.code.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val displayName: String = "",
    val type: String = "openai_compat",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val isFree: Boolean = false,
    val systemPrompt: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
