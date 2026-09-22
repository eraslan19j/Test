package com.redhawk.code.llm.local

import android.util.Log

object LlamaBridge {
    private const val TAG = "LlamaBridge"

    @Volatile var loadError: String? = null
        private set

    @Volatile var loaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("redhawk_jni")
            loaded = true
            Log.i(TAG, "libredhawk_jni.so yüklendi")
        } catch (t: Throwable) {
            loadError = t.message ?: t.toString()
            Log.e(TAG, "Native kütüphane yüklenemedi: ${t.message}", t)
        }
    }

    val isAvailable: Boolean get() = loaded

    external fun nativeLoadModel(
        modelPath: String, nCtx: Int, nThreads: Int, nGpuLayers: Int
    ): Long
    external fun nativeFreeModel(handle: Long)
    external fun nativeGenerate(
        handle: Long, prompt: String, maxTokens: Int, temperature: Float
    ): String
}
