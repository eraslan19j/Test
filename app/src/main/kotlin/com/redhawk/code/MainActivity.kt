package com.redhawk.code

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.redhawk.code.llm.local.LlamaBridge
import com.redhawk.code.ui.theme.RedHawkTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedHawkTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NativeTestScreen(this)
                }
            }
        }
    }
}

private const val SOURCE_MODEL =
    "/sdcard/AI-Models/Qwen2.5-Coder-7B-Q4_K_M/qwen2.5-coder-7b-instruct-q4_k_m.gguf"
private const val SOURCE_MODEL_3B =
    "/sdcard/AI-Models/Qwen2.5-Coder-3B-Q4_K_M/qwen2.5-coder-3b-instruct-q4_k_m.gguf"

@Composable
fun NativeTestScreen(activity: ComponentActivity) {
    val ctx = LocalContext.current
    var status by remember { mutableStateOf("Test için butona bas") }
    var busy by remember { mutableStateOf(false) }
    var useBig by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ReDHawK Native Test", style = MaterialTheme.typography.headlineSmall)

        Text(
            "JNI: " + (if (LlamaBridge.isAvailable) "✅" else "❌ ${LlamaBridge.loadError}"),
            style = MaterialTheme.typography.bodyMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = useBig, onClick = { useBig = true }, label = { Text("7B") })
            FilterChip(selected = !useBig, onClick = { useBig = false }, label = { Text("3B") })
        }

        Button(
            onClick = {
                busy = true
                status = "Hazırlanıyor..."
                Thread {
                    val result = try {
                        val src = File(if (useBig) SOURCE_MODEL else SOURCE_MODEL_3B)
                        if (!src.exists()) {
                            "❌ Kaynak yok: ${src.absolutePath}"
                        } else {
                            // Private dizine kopyala (yoksa)
                            val modelsDir = File(ctx.filesDir, "models").apply { mkdirs() }
                            val dst = File(modelsDir, src.name)
                            if (!dst.exists() || dst.length() != src.length()) {
                                activity.runOnUiThread { status = "Model kopyalanıyor (${src.length()/1024/1024} MB)…" }
                                src.copyTo(dst, overwrite = true)
                            }

                            val t0 = System.currentTimeMillis()
                            val handle = LlamaBridge.nativeLoadModel(dst.absolutePath, 1024, 6, 0)
                            val loadMs = System.currentTimeMillis() - t0
                            if (handle == 0L) {
                                "❌ Yükleme başarısız"
                            } else {
                                val t1 = System.currentTimeMillis()
                                val resp = LlamaBridge.nativeGenerate(
                                    handle,
                                    "Merhaba, kendini 1 cümleyle tanıt.",
                                    100, 0.7f
                                )
                                val genMs = System.currentTimeMillis() - t1
                                LlamaBridge.nativeFreeModel(handle)
                                "✅ Yükleme: ${loadMs}ms\n✅ Üretim: ${genMs}ms\n\n$resp"
                            }
                        }
                    } catch (t: Throwable) {
                        "❌ ${t.message}"
                    }
                    activity.runOnUiThread { status = result; busy = false }
                }.start()
            },
            enabled = !busy && LlamaBridge.isAvailable
        ) {
            Text(if (busy) "Çalışıyor..." else "Yükle ve Test Et")
        }

        if (busy) CircularProgressIndicator()
        Text(status, style = MaterialTheme.typography.bodyMedium)
    }
}
