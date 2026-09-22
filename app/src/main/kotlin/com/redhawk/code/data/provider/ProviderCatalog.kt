package com.redhawk.code.data.provider

data class ProviderTemplate(
    val type: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val requiresKey: Boolean,
    val keyUrl: String = "",
    val isFree: Boolean = false,
    val freeHint: String = "",
    val description: String = "",
    val availableModels: List<String> = emptyList()
)

object ProviderCatalog {

    val presets = listOf(
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Google Gemini",
            defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-2.5-flash",
            requiresKey = true,
            keyUrl = "https://aistudio.google.com/apikey",
            isFree = true,
            freeHint = "aistudio.google.com → Get API Key (ücretsiz)",
            description = "Cömert ücretsiz tier · hızlı",
            availableModels = listOf(
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-1.5-flash",
                "gemini-1.5-pro"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Groq Llama 3.3",
            defaultBaseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            requiresKey = true,
            keyUrl = "https://console.groq.com/keys",
            isFree = true,
            freeHint = "console.groq.com → API Keys (ücretsiz)",
            description = "Çok hızlı çıkarım",
            availableModels = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "gemma2-9b-it",
                "mixtral-8x7b-32768"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "OpenRouter Gemini",
            defaultBaseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "google/gemini-2.0-flash-exp:free",
            requiresKey = true,
            keyUrl = "https://openrouter.ai/keys",
            isFree = true,
            freeHint = "openrouter.ai → Keys → :free modeller",
            description = "Yüzlerce model · ücretsiz",
            availableModels = listOf(
                "google/gemini-2.0-flash-exp:free",
                "meta-llama/llama-3.3-70b-instruct:free",
                "deepseek/deepseek-r1:free",
                "qwen/qwen-2.5-72b-instruct:free"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Mistral",
            defaultBaseUrl = "https://api.mistral.ai/v1",
            defaultModel = "mistral-small-latest",
            requiresKey = true,
            keyUrl = "https://console.mistral.ai/api-keys",
            isFree = true,
            freeHint = "console.mistral.ai → API Keys (ücretsiz)",
            description = "Avrupa · 128k context",
            availableModels = listOf(
                "mistral-small-latest",
                "mistral-large-latest",
                "open-mistral-nemo",
                "codestral-latest"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "DeepSeek",
            defaultBaseUrl = "https://api.deepseek.com/v1",
            defaultModel = "deepseek-chat",
            requiresKey = true,
            keyUrl = "https://platform.deepseek.com/api_keys",
            isFree = false,
            freeHint = "platform.deepseek.com → API Keys",
            description = "Ucuz · kaliteli · düşünme desteği",
            availableModels = listOf("deepseek-chat", "deepseek-reasoner")
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "GitHub Models",
            defaultBaseUrl = "https://models.inference.ai.azure.com",
            defaultModel = "gpt-4o-mini",
            requiresKey = true,
            keyUrl = "https://github.com/settings/tokens",
            isFree = true,
            freeHint = "GitHub → Settings → Personal access tokens",
            description = "GitHub ile ücretsiz · GPT-4o vb.",
            availableModels = listOf("gpt-4o", "gpt-4o-mini", "o1-mini", "o3-mini")
        ),
    )

    val types = listOf(
        ProviderTemplate("openai_compat", "OpenAI",
            "https://api.openai.com/v1", "gpt-4o-mini", true,
            keyUrl = "https://platform.openai.com/api-keys",
            availableModels = listOf("gpt-4o-mini", "gpt-4o")),
        ProviderTemplate("openai_compat", "Anthropic",
            "https://api.anthropic.com/v1", "claude-3-5-haiku-20241022", true,
            keyUrl = "https://console.anthropic.com/settings/keys",
            availableModels = listOf("claude-3-5-haiku-20241022", "claude-3-5-sonnet-20241022")),
        ProviderTemplate("openai_compat", "Grok (xAI)",
            "https://api.x.ai/v1", "grok-3-mini", true,
            keyUrl = "https://console.x.ai/",
            availableModels = listOf("grok-3-mini", "grok-3")),
        ProviderTemplate("openai_compat", "DeepSeek",
            "https://api.deepseek.com/v1", "deepseek-chat", true,
            keyUrl = "https://platform.deepseek.com/api_keys",
            availableModels = listOf("deepseek-chat", "deepseek-reasoner")),
    )

    fun byType(type: String): ProviderTemplate? = types.firstOrNull { it.type == type }
    fun byName(name: String): ProviderTemplate? = presets.firstOrNull { it.displayName == name }

    /** Base URL prefix eşleşmesiyle preset bul */
    fun byBaseUrl(url: String): ProviderTemplate? {
        val host = url.substringAfter("://").substringBefore("/")
        return presets.firstOrNull { p ->
            val presetHost = p.defaultBaseUrl.substringAfter("://").substringBefore("/")
            host == presetHost
        }
    }

    /** Verilen provider için model listesi */
    fun modelsFor(baseUrl: String, currentModel: String): List<String> {
        val tpl = byBaseUrl(baseUrl)
        val list = tpl?.availableModels ?: emptyList()
        return if (currentModel in list) list else listOf(currentModel) + list
    }
}
