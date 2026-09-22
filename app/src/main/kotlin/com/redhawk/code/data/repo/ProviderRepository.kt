package com.redhawk.code.data.repo

import android.content.Context
import com.redhawk.code.data.db.AppDatabase
import com.redhawk.code.data.db.ProviderEntity
import com.redhawk.code.data.provider.ProviderTemplate
import kotlinx.coroutines.flow.Flow

class ProviderRepository(ctx: Context) {
    private val dao = AppDatabase.get(ctx).providerDao()

    fun observeAll(): Flow<List<ProviderEntity>> = dao.observeAll()
    suspend fun get(id: String): ProviderEntity? = dao.get(id)
    suspend fun count(): Int = dao.count()
    suspend fun add(p: ProviderEntity) = dao.upsert(p)
    suspend fun update(id: String, name: String, url: String, key: String, model: String) =
        dao.update(id, name, url, key, model)
    suspend fun delete(id: String) = dao.delete(id)
    suspend fun updateModel(id: String, model: String) = dao.updateModel(id, model)

    suspend fun createFromTemplate(
        template: ProviderTemplate,
        apiKey: String = "",
        model: String = template.defaultModel,
        baseUrl: String = template.defaultBaseUrl
    ): ProviderEntity {
        val e = ProviderEntity(
            displayName = template.displayName,
            type = template.type,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            isFree = template.isFree
        )
        dao.upsert(e)
        return e
    }
}
