package com.redhawk.code.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE archived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE archived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun get(id: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :id")
    fun observe(id: String): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: ChatEntity)

    @Query("UPDATE chats SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: String, title: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET starred = :starred, updatedAt = :now WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET modelId = :modelId, updatedAt = :now WHERE id = :id")
    suspend fun setModel(id: String, modelId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET projectUri = :uri, updatedAt = :now WHERE id = :id")
    suspend fun setProject(id: String, uri: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE chats SET updatedAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chats WHERE archived = 1")
    suspend fun clearArchived()
}
