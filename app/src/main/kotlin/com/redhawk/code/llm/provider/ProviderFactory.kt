package com.redhawk.code.llm.provider

import com.redhawk.code.data.db.ProviderEntity
import com.redhawk.code.llm.LlmProvider

object ProviderFactory {
    fun create(p: ProviderEntity): LlmProvider =
        if (p.type == "duckai") {
            DuckAiProvider(id = p.id, displayName = p.displayName)
        } else {
            OpenAiCompatProvider(
                id = p.id,
                displayName = p.displayName,
                baseUrl = p.baseUrl,
                apiKey = p.apiKey
            )
        }
}
