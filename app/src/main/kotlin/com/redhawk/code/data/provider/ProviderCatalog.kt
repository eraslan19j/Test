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
    val availableModels: List<String> = emptyList(),
    val tokenLimit: String = "",
    val supportsTools: Boolean = true
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
            ),
            tokenLimit = "1M",
            supportsTools = true
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
            ),
            tokenLimit = "32K",
            supportsTools = false
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
            ),
            tokenLimit = "4K",
            supportsTools = false
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
            ),
            tokenLimit = "128K",
            supportsTools = true
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
            ),
            tokenLimit = "128K",
            supportsTools = true
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
            ),
            tokenLimit = "128K",
            supportsTools = true
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
            ),
            tokenLimit = "128K",
            supportsTools = true
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "LLM7",
            defaultBaseUrl = "https://api.llm7.io/v1",
            defaultModel = "DeepSeek-V4-Flash-0731",
            requiresKey = false,
            keyUrl = "https://token.llm7.io",
            isFree = true,
            freeHint = "Anahtar gerekmez — dakikada ~10 istek (turbo)",
            description = "ANAHTARSIZ · DeepSeek V4 + GLM 5.3 + Codestral",
            availableModels = listOf(
                "DeepSeek-V4-Flash-0731",
                "GLM-5.3-Flash",
                "codestral-latest"
            ),
            tokenLimit = "64K",
            supportsTools = true
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "OVH",
            defaultBaseUrl = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1",
            defaultModel = "gpt-oss-20b",
            requiresKey = false,
            keyUrl = "",
            isFree = true,
            freeHint = "Anahtar gerekmez — model başına ~2 istek/dk",
            description = "ANAHTARSIZ · Qwen/Llama/gpt-oss (AB sunucuları)",
            availableModels = listOf(
                "gpt-oss-20b",
                "gpt-oss-120b",
                "Qwen3-Coder-30B-A3B-Instruct",
                "Qwen3.8-27B",
                "Meta-Llama-3_3-70B-Instruct",
                "Mistral-Small-3.2-24B-Instruct-2506",
                "Mistral-Nemo-Instruct-2407"
            ),
            tokenLimit = "128K",
            supportsTools = true
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Dahl",
            defaultBaseUrl = "https://inference.dahl.global/v1",
            defaultModel = "MiniMaxAI/MiniMax-M2.7",
            requiresKey = true,
            keyUrl = "https://inference.dahl.global",
            isFree = true,
            freeHint = "Anahtar başına 100M token (ücretsiz kayıt)",
            description = "100M TOKEN · MiniMax M2.7 (OpenAI uyumlu)",
            availableModels = listOf(
                "MiniMaxAI/MiniMax-M2.7"
            ),
            tokenLimit = "128K",
            supportsTools = true
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "NaraRouter",
            defaultBaseUrl = "https://router.bynara.id/v1",
            defaultModel = "claude-sonnet-4.5",
            requiresKey = true,
            keyUrl = "https://router.bynara.id/register",
            isFree = true,
            freeHint = "Günde 7M token (Google ile kayıt)",
            description = "GÜNLÜK 7M · 30+ model yönlendirme",
            availableModels = listOf(
                "claude-sonnet-4.5",
                "qwen-3.8-max-free"
            ),
            tokenLimit = "200K",
            supportsTools = true
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
            ),
            tokenLimit = "128K",
            supportsTools = true
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
                "mistralai/mistral-small-3.1-24b-instruct:free",
                "poolside/laguna-s-2.1:free",
                "nvidia/nemotron-3-ultra-550b-a55b:free",
                "dots-studio/dots-3-note-preview:free",
                "nex-agi/nex-n2.5-pro:free",
                "inclusionai/ling-3.0-flash-vl:free"
            ),
            tokenLimit = "16K",
            supportsTools = true
        ),
        ProviderTemplate(
            type = "openai_compat",
            displayName = "Kilo Auto",
            defaultBaseUrl = "https://api-inference.huggingface.co/v1",
            defaultModel = "kilo-auto/free",
            requiresKey = false,
            keyUrl = "https://huggingface.co/settings/tokens",
            isFree = true,
            freeHint = "HF token ile ücretsiz (dakikada 30K token)",
            description = "ANAHTARSIZ · Kilo otomatik yönlendirme (multimodal)",
            availableModels = listOf("kilo-auto/free"),
            tokenLimit = "16K",
            supportsTools = false
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
            ),
            tokenLimit = "128K",
            supportsTools = true
        ),
            defaultBaseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-chat",
            requiresKey = true,
            keyUrl = "https://platform.deepseek.com/api_keys",
            isFree = false,
            freeHint = "platform.deepseek.com → API Keys",
            description = "Ucuz · kaliteli · düşünme desteği",
            availableModels = listOf("deepseek-chat", "deepseek-reasoner"),
            tokenLimit = "128K",
            supportsTools = true
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
            ),
            tokenLimit = "200K",
            supportsTools = true
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
            availableModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"),
            tokenLimit = "16K",
            supportsTools = true),
        ProviderTemplate("openai_compat", "Grok (xAI)",
            "https://api.x.ai/v1", "grok-3-mini", true,
            keyUrl = "https://console.x.ai/",
            description = "Ücretli",
            availableModels = listOf("grok-3-mini", "grok-3", "grok-4"),
            tokenLimit = "128K",
            supportsTools = true),
        ProviderTemplate("openai_compat", "Özel uç",
            "https://", "model-id", false,
            description = "Kendi sunucun (Ollama, vLLM, LM Studio…)",
            availableModels = emptyList(),
            tokenLimit = "?K",
            supportsTools = true),
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
