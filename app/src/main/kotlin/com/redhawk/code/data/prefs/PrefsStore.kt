package com.redhawk.code.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.redhawkDataStore by preferencesDataStore(name = "redhawk_prefs")

class PrefsStore(private val context: Context) {

    private object Keys {
        val PROVIDER   = stringPreferencesKey("provider")     // "openai_compat" | "gemini"
        val BASE_URL   = stringPreferencesKey("base_url")
        val API_KEY    = stringPreferencesKey("api_key")
        val MODEL      = stringPreferencesKey("model")
        val PROJECT_URI = stringPreferencesKey("project_uri") // SAF klasör URI
    }

    val provider: Flow<String> = context.redhawkDataStore.data
        .map { it[Keys.PROVIDER] ?: "openai_compat" }

    val baseUrl: Flow<String> = context.redhawkDataStore.data
        .map { it[Keys.BASE_URL] ?: "http://127.0.0.1:11434/v1" }

    val apiKey: Flow<String> = context.redhawkDataStore.data
        .map { it[Keys.API_KEY] ?: "" }

    val model: Flow<String> = context.redhawkDataStore.data
        .map { it[Keys.MODEL] ?: "qwen2.5-coder:7b" }

    val projectUri: Flow<String?> = context.redhawkDataStore.data
        .map { it[Keys.PROJECT_URI] }

    suspend fun setProvider(v: String)   = context.redhawkDataStore.edit { it[Keys.PROVIDER] = v }
    suspend fun setBaseUrl(v: String)    = context.redhawkDataStore.edit { it[Keys.BASE_URL] = v }
    suspend fun setApiKey(v: String)     = context.redhawkDataStore.edit { it[Keys.API_KEY] = v }
    suspend fun setModel(v: String)      = context.redhawkDataStore.edit { it[Keys.MODEL] = v }
    suspend fun setProjectUri(v: String?)= context.redhawkDataStore.edit {
        if (v == null) it.remove(Keys.PROJECT_URI) else it[Keys.PROJECT_URI] = v
    }
}
