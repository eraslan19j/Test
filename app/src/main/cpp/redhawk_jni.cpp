#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include <cstring>
#include "llama.h"

#define TAG "ReDHawK-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LlamaCtx {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    const llama_vocab* vocab = nullptr;
    int n_ctx = 0;
    std::mutex mu;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_redhawk_code_llm_local_LlamaBridge_nativeLoadModel(
    JNIEnv* env, jobject, jstring jModelPath,
    jint nCtx, jint nThreads, jint nGpuLayers)
{
    const char* path = env->GetStringUTFChars(jModelPath, nullptr);
    llama_backend_init();

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = nGpuLayers;

    llama_model* model = llama_model_load_from_file(path, mparams);
    env->ReleaseStringUTFChars(jModelPath, path);
    if (!model) { LOGE("Model load failed: %s", path); return 0; }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = nCtx;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;
    cparams.type_k = GGML_TYPE_Q8_0;
    cparams.type_v = GGML_TYPE_Q8_0;

    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) { llama_model_free(model); return 0; }

    auto* lc = new LlamaCtx();
    lc->model = model;
    lc->ctx = ctx;
    lc->vocab = llama_model_get_vocab(model);
    lc->n_ctx = nCtx;

    LOGI("Model loaded. n_ctx=%d threads=%d", nCtx, nThreads);
    return reinterpret_cast<jlong>(lc);
}

JNIEXPORT void JNICALL
Java_com_redhawk_code_llm_local_LlamaBridge_nativeFreeModel(JNIEnv*, jobject, jlong handle)
{
    auto* lc = reinterpret_cast<LlamaCtx*>(handle);
    if (!lc) return;
    if (lc->ctx) llama_free(lc->ctx);
    if (lc->model) llama_model_free(lc->model);
    delete lc;
}

JNIEXPORT jstring JNICALL
Java_com_redhawk_code_llm_local_LlamaBridge_nativeGenerate(
    JNIEnv* env, jobject, jlong handle,
    jstring jPrompt, jint maxTokens, jfloat temperature)
{
    auto* lc = reinterpret_cast<LlamaCtx*>(handle);
    if (!lc || !lc->ctx) return env->NewStringUTF("[hata]");

    std::lock_guard<std::mutex> lock(lc->mu);

    const char* promptCStr = env->GetStringUTFChars(jPrompt, nullptr);
    std::string userInput(promptCStr);
    env->ReleaseStringUTFChars(jPrompt, promptCStr);

    std::string prompt;
    prompt.reserve(userInput.size() + 128);
    prompt += "<|im_start|>system\nSen ReDHawK adinda yardimci bir AI asistansin. Kisa ve net Turkce cevap ver.<|im_end|>\n";
    prompt += "<|im_start|>user\n";
    prompt += userInput;
    prompt += "<|im_end|>\n<|im_start|>assistant\n";

    int n_prompt_tokens = -llama_tokenize(
        lc->vocab, prompt.c_str(), (int)prompt.size(),
        nullptr, 0, false, true);
    std::vector<llama_token> tokens(n_prompt_tokens);
    if (llama_tokenize(lc->vocab, prompt.c_str(), (int)prompt.size(),
                       tokens.data(), (int)tokens.size(), false, true) < 0) {
        return env->NewStringUTF("[tokenize hata]");
    }

    auto* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
    std::string result;
    result.reserve(512);

    for (int i = 0; i < maxTokens; i++) {
        if (llama_decode(lc->ctx, batch) != 0) break;
        llama_token id = llama_sampler_sample(smpl, lc->ctx, -1);
        if (llama_vocab_is_eog(lc->vocab, id)) break;

        char buf[256];
        int n = llama_token_to_piece(lc->vocab, id, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);

        if (result.find("<|im_end|>") != std::string::npos) break;

        batch = llama_batch_get_one(&id, 1);
    }

    llama_sampler_free(smpl);

    // Stop string'leri temizle
    const char* stops[] = {"<|im_end|>", "<|im_start|>", "<|endoftext|>"};
    for (auto stop : stops) {
        size_t pos;
        while ((pos = result.find(stop)) != std::string::npos) result.erase(pos, strlen(stop));
    }

    return env->NewStringUTF(result.c_str());
}

}
