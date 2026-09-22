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
            freeHint = "aistudio.google.com → Get API Key (ücretsiz, kart istemez)",
            description = "ÖNERİLEN · Cömert ücretsiz tier · hızlı",
            availableModels = listOf(
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Pollinations",
            defaultBaseUrl = "https://text.pollinations.ai/openai",
            defaultModel = "openai",
            requiresKey = false,
            keyUrl = "",
            isFree = true,
            freeHint = "Anahtar gerekmez — anında dene (demo kalitesinde)",
            description = "ANAHTARSIZ · Kayıtsız deneme",
            availableModels = listOf(
                "openai",
                "mistral",
                "gemini",
                "deepseek",
                "qwen-coder",
                "claude"
            )
        ),
        ProviderTemplate(
            type = "duckai",
            displayName = "Duck.ai",
            defaultBaseUrl = "https://duckduckgo.com",
            defaultModel = "gpt-4o-mini",
            requiresKey = false,
            keyUrl = "",
            isFree = true,
            freeHint = "Anahtar gerekmez — deneysel, bozulabilir",
            description = "ANAHTARSIZ · GPT-4o mini ücretsiz (deneysel)",
            availableModels = listOf(
                "gpt-4o-mini",
                "meta-llama/Llama-3.3-70B-Instruct-Turbo",
                "claude-3-haiku-20240307",
                "mistralai/Mixtral-8x7B-Instruct-v0.1"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "NVIDIA NIM",
            defaultBaseUrl = "https://integrate.api.nvidia.com/v1",
            defaultModel = "deepseek-ai/deepseek-v3.2",
            requiresKey = true,
            keyUrl = "https://build.nvidia.com",
            isFree = true,
            freeHint = "Ücretsiz anahtar — dakikada 40 istek",
            description = "ÜCRETSİZ · DeepSeek, Kimi, Llama (NVIDIA)",
            availableModels = listOf(
                "deepseek-ai/deepseek-v3.2",
                "moonshotai/kimi-k2-instruct",
                "meta/llama-3.1-70b-instruct",
                "nvidia/llama-3.1-nemotron-70b-instruct"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Z.ai GLM",
            defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
            defaultModel = "glm-4-flash",
            requiresKey = true,
            keyUrl = "https://open.bigmodel.cn",
            isFree = true,
            freeHint = "Yeni üyelikte ücretsiz kredi",
            description = "ÜCRETSİZ KREDİ · GLM-4 (Zhipu)",
            availableModels = listOf(
                "glm-4-flash",
                "glm-4v-flash",
                "glm-4-plus"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Atria",
            defaultBaseUrl = "https://api.atria-asi.ai/v1",
            defaultModel = "Atria-Dawn-Preview",
            requiresKey = true,
            keyUrl = "https://api.atria-asi.ai/docs",
            isFree = true,
            freeHint = "100M token ücretsiz kota (kampanya)",
            description = "ÜCRETSİZ KOTA · Atria Dawn agentic (OpenAI uyumlu)",
            availableModels = listOf(
                "Atria-Dawn-Preview"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "KiraAI",
            defaultBaseUrl = "https://kiraai.vn/api/v1",
            defaultModel = "kira-mini-1.0",
            requiresKey = true,
            keyUrl = "https://kiraai.vn/developer/",
            isFree = true,
            freeHint = "kira-mini-1.0 ücretsiz (anahtar gerekli)",
            description = "ÜCRETSİZ MODEL VAR · Kira + GLM/Qwen/Mimo (OpenAI uyumlu)",
            availableModels = listOf(
                "kira-mini-1.0",
                "kira-flash",
                "kira-3.5-flash",
                "kira-3.5-pro",
                "kira-2.5-flash",
                "kira-2.5-pro",
                "glm-5.3-flash",
                "glm-5.3",
                "qwen3.8-flash",
                "qwen3.8-27b",
                "mimo-v2.5",
                "hy3",
                "gpt-oss-120b",
                "mercury-2.5",
                "minimax-m3"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Groq",
            defaultBaseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "openai/gpt-oss-120b",
            requiresKey = true,
            keyUrl = "https://console.groq.com/keys",
            isFree = true,
            freeHint = "console.groq.com → API Keys (ücretsiz)",
            description = "Çok hızlı çıkarım · açık modeller",
            availableModels = listOf(
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "qwen/qwen3-32b",
                "moonshotai/kimi-k2-instruct",
                "meta-llama/llama-4-scout-17b-16e-instruct"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "OpenRouter",
            defaultBaseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "deepseek/deepseek-r1:free",
            requiresKey = true,
            keyUrl = "https://openrouter.ai/keys",
            isFree = true,
            freeHint = "openrouter.ai → Keys → :free modeller",
            description = "Yüzlerce model · ücretsiz seçenekler",
            availableModels = listOf(
                "deepseek/deepseek-r1:free",
                "meta-llama/llama-3.3-70b-instruct:free",
                "qwen/qwen-2.5-72b-instruct:free",
                "google/gemini-2.0-flash-exp:free",
                "mistralai/mistral-small-3.1-24b-instruct:free"
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
            freeHint = "console.mistral.ai → API Keys (ücretsiz deneme)",
            description = "Avrupa · kodlama için Codestral",
            availableModels = listOf(
                "mistral-small-latest",
                "mistral-medium-latest",
                "mistral-large-latest",
                "codestral-latest",
                "open-mistral-nemo"
            )
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "DeepSeek",
            defaultBaseUrl = "https://api.deepseek.com",
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
            defaultBaseUrl = "https://models.github.ai/inference",
            defaultModel = "openai/gpt-4o-mini",
            requiresKey = true,
            keyUrl = "https://github.com/settings/tokens",
            isFree = true,
            freeHint = "GitHub → Settings → Personal access tokens (models:read)",
            description = "GitHub hesabınla ücretsiz kotalar",
            availableModels = listOf(
                "openai/gpt-4o-mini",
                "openai/gpt-4o",
                "openai/gpt-4.1-mini",
                "deepseek/DeepSeek-V3-0324",
                "meta/Llama-3.3-70B-Instruct"
            )
        ),
    )

    /**
     * Diğer / ücretli / özel uçlar. Hepsi OpenAI-uyumlu (/chat/completions) olmalı.
     * NOT: Anthropic burada YOK — Claude API'si OpenAI protokolüyle uyumsuz
     * (/v1/messages kullanır). Ayrı bir ClaudeProvider yazılmadan eklenemez.
     */
    val types = listOf(
        ProviderTemplate("openai_compat", "OpenAI",
            "https://api.openai.com/v1", "gpt-4o-mini", true,
            keyUrl = "https://platform.openai.com/api-keys",
            description = "Ücretli · referans kalite",
            availableModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini")),
        ProviderTemplate("openai_compat", "Grok (xAI)",
            "https://api.x.ai/v1", "grok-3-mini", true,
            keyUrl = "https://console.x.ai/",
            description = "Ücretli",
            availableModels = listOf("grok-3-mini", "grok-3", "grok-4")),
        ProviderTemplate("openai_compat", "Özel uç",
            "https://", "model-id", false,
            description = "Kendi sunucun (Ollama, vLLM, LM Studio…)",
            availableModels = emptyList()),
    )

    fun byType(type: String): ProviderTemplate? =
        (presets + types).firstOrNull { it.type == type }

    fun byName(name: String): ProviderTemplate? =
        (presets + types).firstOrNull { it.displayName == name }

    /** Base URL prefix eşleşmesiyle preset bul */
    fun byBaseUrl(url: String): ProviderTemplate? {
        val host = url.substringAfter("://").substringBefore("/")
        return (presets + types).firstOrNull { p ->
            val presetHost = p.defaultBaseUrl.substringAfter("://").substringBefore("/")
            host.isNotBlank() && host == presetHost
        }
    }

    /** Verilen provider için model listesi */
    fun modelsFor(baseUrl: String, currentModel: String): List<String> {
        val tpl = byBaseUrl(baseUrl)
        val list = tpl?.availableModels ?: emptyList()
        return if (currentModel in list) list else listOf(currentModel) + list
    }
}
