package com.redhawk.code.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "Yeni sohbet",
    val modelId: String = "qwen2.5-coder-3b",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val starred: Boolean = false,
    val archived: Boolean = false,
    val projectUri: String? = null
)
