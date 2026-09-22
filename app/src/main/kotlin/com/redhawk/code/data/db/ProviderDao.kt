package com.redhawk.code.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun get(id: String): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProviderEntity)

    @Query("UPDATE providers SET displayName=:name, baseUrl=:url, apiKey=:key, model=:model WHERE id=:id")
    suspend fun update(id: String, name: String, url: String, key: String, model: String)

    @Query("UPDATE providers SET model = :model WHERE id = :id")
    suspend fun updateModel(id: String, model: String)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun count(): Int
}
