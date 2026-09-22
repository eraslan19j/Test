package com.redhawk.code.data.repo

import android.content.Context
import com.redhawk.code.data.db.AppDatabase
import com.redhawk.code.data.db.ChatEntity
import com.redhawk.code.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(ctx: Context) {
    private val db = AppDatabase.get(ctx)
    private val chatDao = db.chatDao()
    private val msgDao = db.messageDao()

    fun observeChats(): Flow<List<ChatEntity>> = chatDao.observeAll()
    fun observeArchived(): Flow<List<ChatEntity>> = chatDao.observeArchived()
    fun observeChat(id: String): Flow<ChatEntity?> = chatDao.observe(id)
    fun observeMessages(chatId: String): Flow<List<MessageEntity>> = msgDao.observeForChat(chatId)

    suspend fun getChat(id: String): ChatEntity? = chatDao.get(id)
    suspend fun createChat(modelId: String, title: String = "Yeni sohbet"): ChatEntity {
        val c = ChatEntity(title = title, modelId = modelId)
        chatDao.upsert(c)
        return c
    }

    suspend fun renameChat(id: String, title: String) = chatDao.rename(id, title)
    suspend fun setPinned(id: String, p: Boolean) = chatDao.setPinned(id, p)
    suspend fun setStarred(id: String, s: Boolean) = chatDao.setStarred(id, s)
    suspend fun setArchived(id: String, a: Boolean) = chatDao.setArchived(id, a)
    suspend fun setModel(id: String, modelId: String) = chatDao.setModel(id, modelId)
    suspend fun setProject(id: String, uri: String?) = chatDao.setProject(id, uri)
    suspend fun deleteChat(id: String) = chatDao.deleteById(id)
    suspend fun clearArchived() = chatDao.clearArchived()

    suspend fun addMessage(msg: MessageEntity) = msgDao.upsert(msg)
    suspend fun updateMessageContent(id: String, content: String) = msgDao.updateContent(id, content)
    suspend fun updateMessageError(id: String, err: String?) = msgDao.updateError(id, err)
    suspend fun lastMessage(chatId: String): MessageEntity? = msgDao.lastInChat(chatId)
    suspend fun search(chatId: String, q: String) = msgDao.search(chatId, q)
    suspend fun deleteMessage(id: String) = msgDao.deleteById(id)
    suspend fun clearMessages(chatId: String) = msgDao.deleteForChat(chatId)

    suspend fun touch(chatId: String) = chatDao.touch(chatId)

    /** Otomatik başlık: ilk kullanıcı mesajının ilk 40 karakteri */
    suspend fun autoTitle(chatId: String): String? {
        val chat = chatDao.get(chatId) ?: return null
        if (chat.title != "Yeni sohbet") return chat.title
        val first = msgDao.firstUserMessage(chatId) ?: return null
        val title = first.content.trim().replace("\n", " ").take(40).trim()
        if (title.isBlank()) return null
        chatDao.rename(chatId, title)
        return title
    }
}
