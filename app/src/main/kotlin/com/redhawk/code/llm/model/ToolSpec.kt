package com.redhawk.code.llm.model

/**
 * Modele bildirilen araç tanımı (JSON Schema ile parametreler).
 */
data class ToolSpec(
    val name: String,
    val description: String,
    val parametersJsonSchema: String
)
