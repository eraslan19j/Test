package com.redhawk.code.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.redhawkDataStore by preferencesDataStore(name = "redhawk_prefs")

class PrefsStore(private val context: Context) {

    private object Keys {
        // Genel
        val SETUP_DONE              = booleanPreferencesKey("setup_done")
        val SELECTED_PROVIDER       = stringPreferencesKey("selected_provider_id")
        val SYSTEM_PROMPT           = stringPreferencesKey("system_prompt")
        val TEMPERATURE             = floatPreferencesKey("temperature")
        val MAX_TOKENS              = intPreferencesKey("max_tokens")
        val RESPONSE_LANG           = stringPreferencesKey("response_lang") // tr | en | auto

        // Görünüm
        val THEME                   = stringPreferencesKey("theme")        // system | light | dark
        val TRANSPARENT_SIDEBAR     = booleanPreferencesKey("transparent_sidebar")
        val CONTRAST                = intPreferencesKey("contrast")        // 0-100

        // Tercihler
        val SHOW_CURSOR_MARKS       = booleanPreferencesKey("show_cursor_marks")
        val REDUCE_MOTION           = booleanPreferencesKey("reduce_motion")
        val UI_FONT_SIZE            = intPreferencesKey("ui_font_size")    // 12-20
        val CODE_FONT_SIZE          = intPreferencesKey("code_font_size")  // 10-18
        val DIFF_STYLE              = stringPreferencesKey("diff_style")   // color | marks

        // Kişiselleştirme
        val CUSTOM_INSTRUCTIONS     = stringPreferencesKey("custom_instructions")
        val LOCAL_MEMORY            = booleanPreferencesKey("local_memory")
        val TOOL_MEMORY             = booleanPreferencesKey("tool_memory")
        val PERSONALITY             = stringPreferencesKey("personality")  // balanced | concise | detailed

        // Oluşturucu
        val PLAIN_TEXT_EDITOR       = booleanPreferencesKey("plain_text_editor")
        val SHOW_CONTEXT_USAGE      = booleanPreferencesKey("show_context_usage")
        val SEND_SHORTCUT           = stringPreferencesKey("send_shortcut") // enter | ctrl_enter

        // Bildirimler
        val NOTIFY_TURN_DONE        = booleanPreferencesKey("notify_turn_done")
        val NOTIFY_PERMISSION       = booleanPreferencesKey("notify_permission")
        val NOTIFY_QUESTION         = booleanPreferencesKey("notify_question")
        val NOTIFY_COMPLETION       = booleanPreferencesKey("notify_completion")

        // Kullanım istatistikleri
        val TOTAL_TOKENS            = intPreferencesKey("total_tokens")
        val TOTAL_CHATS             = intPreferencesKey("total_chats")
        val LONGEST_CHAT            = intPreferencesKey("longest_chat")
        val LAST_ACTIVITY           = stringPreferencesKey("last_activity")

        // Hızlı mod / prompt öner
        val FAST_MODE               = booleanPreferencesKey("fast_mode")
        val SUGGESTED_PROMPTS       = booleanPreferencesKey("suggested_prompts")
        val EXTENSIONS_ENABLED      = booleanPreferencesKey("extensions_enabled")
    }

    // ---- Genel
    val setupDone: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.SETUP_DONE] ?: false }
    suspend fun setSetupDone(v: Boolean) = context.redhawkDataStore.edit { it[Keys.SETUP_DONE] = v }

    val selectedProviderId: Flow<String?> = context.redhawkDataStore.data.map { it[Keys.SELECTED_PROVIDER] }
    suspend fun setSelectedProvider(id: String?) = context.redhawkDataStore.edit {
        if (id == null) it.remove(Keys.SELECTED_PROVIDER) else it[Keys.SELECTED_PROVIDER] = id
    }

    val systemPrompt: Flow<String> = context.redhawkDataStore.data.map { it[Keys.SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT }
    suspend fun setSystemPrompt(s: String) = context.redhawkDataStore.edit { it[Keys.SYSTEM_PROMPT] = s }

    val temperature: Flow<Float> = context.redhawkDataStore.data.map { it[Keys.TEMPERATURE] ?: 0.7f }
    suspend fun setTemperature(v: Float) = context.redhawkDataStore.edit { it[Keys.TEMPERATURE] = v }

    val maxTokens: Flow<Int> = context.redhawkDataStore.data.map { it[Keys.MAX_TOKENS] ?: 2048 }
    suspend fun setMaxTokens(v: Int) = context.redhawkDataStore.edit { it[Keys.MAX_TOKENS] = v }

    val responseLang: Flow<String> = context.redhawkDataStore.data.map { it[Keys.RESPONSE_LANG] ?: "tr" }
    suspend fun setResponseLang(v: String) = context.redhawkDataStore.edit { it[Keys.RESPONSE_LANG] = v }

    // ---- Görünüm
    val theme: Flow<String> = context.redhawkDataStore.data.map { it[Keys.THEME] ?: "dark" }
    suspend fun setTheme(v: String) = context.redhawkDataStore.edit { it[Keys.THEME] = v }

    val transparentSidebar: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.TRANSPARENT_SIDEBAR] ?: true }
    suspend fun setTransparentSidebar(v: Boolean) = context.redhawkDataStore.edit { it[Keys.TRANSPARENT_SIDEBAR] = v }

    val contrast: Flow<Int> = context.redhawkDataStore.data.map { it[Keys.CONTRAST] ?: 50 }
    suspend fun setContrast(v: Int) = context.redhawkDataStore.edit { it[Keys.CONTRAST] = v }

    // ---- Tercihler
    val showCursorMarks: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.SHOW_CURSOR_MARKS] ?: false }
    suspend fun setShowCursorMarks(v: Boolean) = context.redhawkDataStore.edit { it[Keys.SHOW_CURSOR_MARKS] = v }

    val reduceMotion: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.REDUCE_MOTION] ?: false }
    suspend fun setReduceMotion(v: Boolean) = context.redhawkDataStore.edit { it[Keys.REDUCE_MOTION] = v }

    val uiFontSize: Flow<Int> = context.redhawkDataStore.data.map { it[Keys.UI_FONT_SIZE] ?: 14 }
    suspend fun setUiFontSize(v: Int) = context.redhawkDataStore.edit { it[Keys.UI_FONT_SIZE] = v }

    val codeFontSize: Flow<Int> = context.redhawkDataStore.data.map { it[Keys.CODE_FONT_SIZE] ?: 13 }
    suspend fun setCodeFontSize(v: Int) = context.redhawkDataStore.edit { it[Keys.CODE_FONT_SIZE] = v }

    val diffStyle: Flow<String> = context.redhawkDataStore.data.map { it[Keys.DIFF_STYLE] ?: "color" }
    suspend fun setDiffStyle(v: String) = context.redhawkDataStore.edit { it[Keys.DIFF_STYLE] = v }

    // ---- Kişiselleştirme
    val customInstructions: Flow<String> = context.redhawkDataStore.data.map { it[Keys.CUSTOM_INSTRUCTIONS] ?: "" }
    suspend fun setCustomInstructions(v: String) = context.redhawkDataStore.edit { it[Keys.CUSTOM_INSTRUCTIONS] = v }

    val localMemory: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.LOCAL_MEMORY] ?: false }
    suspend fun setLocalMemory(v: Boolean) = context.redhawkDataStore.edit { it[Keys.LOCAL_MEMORY] = v }

    val toolMemory: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.TOOL_MEMORY] ?: false }
    suspend fun setToolMemory(v: Boolean) = context.redhawkDataStore.edit { it[Keys.TOOL_MEMORY] = v }

    val personality: Flow<String> = context.redhawkDataStore.data.map { it[Keys.PERSONALITY] ?: "balanced" }
    suspend fun setPersonality(v: String) = context.redhawkDataStore.edit { it[Keys.PERSONALITY] = v }

    // ---- Oluşturucu
    val plainTextEditor: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.PLAIN_TEXT_EDITOR] ?: false }
    suspend fun setPlainTextEditor(v: Boolean) = context.redhawkDataStore.edit { it[Keys.PLAIN_TEXT_EDITOR] = v }

    val showContextUsage: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.SHOW_CONTEXT_USAGE] ?: true }
    suspend fun setShowContextUsage(v: Boolean) = context.redhawkDataStore.edit { it[Keys.SHOW_CONTEXT_USAGE] = v }

    val sendShortcut: Flow<String> = context.redhawkDataStore.data.map { it[Keys.SEND_SHORTCUT] ?: "enter" }
    suspend fun setSendShortcut(v: String) = context.redhawkDataStore.edit { it[Keys.SEND_SHORTCUT] = v }

    // ---- Bildirimler
    val notifyTurnDone: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.NOTIFY_TURN_DONE] ?: true }
    suspend fun setNotifyTurnDone(v: Boolean) = context.redhawkDataStore.edit { it[Keys.NOTIFY_TURN_DONE] = v }

    val notifyPermission: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.NOTIFY_PERMISSION] ?: true }
    suspend fun setNotifyPermission(v: Boolean) = context.redhawkDataStore.edit { it[Keys.NOTIFY_PERMISSION] = v }

    val notifyQuestion: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.NOTIFY_QUESTION] ?: true }
    suspend fun setNotifyQuestion(v: Boolean) = context.redhawkDataStore.edit { it[Keys.NOTIFY_QUESTION] = v }

    val notifyCompletion: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.NOTIFY_COMPLETION] ?: true }
    suspend fun setNotifyCompletion(v: Boolean) = context.redhawkDataStore.edit { it[Keys.NOTIFY_COMPLETION] = v }

    // ---- İstatistik
    val totalTokens: Flow<Int> = context.redhawkDataStore.data.map { it[Keys.TOTAL_TOKENS] ?: 0 }
    suspend fun addTokens(n: Int) = context.redhawkDataStore.edit { it[Keys.TOTAL_TOKENS] = (it[Keys.TOTAL_TOKENS] ?: 0) + n }

    val totalChats: Flow<Int> = context.redhawkDataStore.data.map { it[Keys.TOTAL_CHATS] ?: 0 }
    suspend fun setTotalChats(n: Int) = context.redhawkDataStore.edit { it[Keys.TOTAL_CHATS] = n }

    val longestChat: Flow<Int> = context.redhawkDataStore.data.map { it[Keys.LONGEST_CHAT] ?: 0 }
    suspend fun setLongestChat(n: Int) = context.redhawkDataStore.edit { it[Keys.LONGEST_CHAT] = n }

    val lastActivity: Flow<String> = context.redhawkDataStore.data.map { it[Keys.LAST_ACTIVITY] ?: "" }
    suspend fun setLastActivity(s: String) = context.redhawkDataStore.edit { it[Keys.LAST_ACTIVITY] = s }

    // ---- Hızlı mod / diğer
    val fastMode: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.FAST_MODE] ?: false }
    suspend fun setFastMode(v: Boolean) = context.redhawkDataStore.edit { it[Keys.FAST_MODE] = v }

    val suggestedPrompts: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.SUGGESTED_PROMPTS] ?: true }
    suspend fun setSuggestedPrompts(v: Boolean) = context.redhawkDataStore.edit { it[Keys.SUGGESTED_PROMPTS] = v }

    val extensionsEnabled: Flow<Boolean> = context.redhawkDataStore.data.map { it[Keys.EXTENSIONS_ENABLED] ?: true }
    suspend fun setExtensionsEnabled(v: Boolean) = context.redhawkDataStore.edit { it[Keys.EXTENSIONS_ENABLED] = v }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "Sen ReDHawK adında yardımcı bir AI asistansın. Kısa, net ve Türkçe cevap ver. " +
            "Kod yazarken temiz ve okunabilir kod üret. Emin olmadığın konularda tahmin yürütme."
    }
}
