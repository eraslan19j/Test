# ReDHawK Code — Yol Haritası

## Mimari (özet)

```
ui/            → Compose ekranları (chat, provider, settings, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu
agent/         → AgentTools (araç tanımları) + ProjectFiles (SAF + app deposu)
llm/           → LlmProvider arayüzü + OpenAI-uyumlu istemci (SSE streaming)
data/db        → Room (chats, messages, providers)
data/prefs     → DataStore ayarlar
data/provider  → ProviderCatalog (hazır presetler)
```

Kural: yeni yapay zeka sağlayıcısı = `ProviderCatalog`'a 1 preset.
Yeni ajan aracı = `AgentTools.specs`'e 1 ToolSpec + `execute`'e 1 dal.

## Yapıldı (v0.2)

- [x] Splash düzeltmesi (API 31+ tema tutarlılığı + vektör ikon)
- [x] Onboarding (ilk açılış karşılama + sağlayıcı kurulumu)
- [x] Thinking bug düzeltmesi (full-reparse: Gemini/GPT cevabı doğru balonda)
- [x] Durdur butonu gerçekten bağlantıyı kesiyor
- [x] Sağlayıcı kataloğu: DeepSeek URL, GitHub Models, güncel Groq/OpenRouter
      modelleri, anahtarsız Pollinations preseti, bozuk Anthropic kaldırıldı
- [x] Kurulumda API key silme bug'ı + opsiyonel key + eksik Düzenle butonu
- [x] Ölü kod temizliği (StoragePermissionGate, autoTitle, .cxx artıkları)
- [x] AJAN FAZ 1: tool_calls + list/read/write_file + yazma onayı +
      proje klasörü seçimi (SAF) + uygulama deposu

## Sıradaki (önerilen sıra)

- [ ] AJAN FAZ 2: `delete_file`, `run_command` (kısıtlı), diff önizlemeli yazma
- [ ] Dosya gezgini ekranı (ajan deposunu uygulamada gör/düzenle)
- [ ] Kod editörü (renklendirme + satır numarası)
- [ ] Git entegrasyonu (commit/geri al = ajan güvenlik ağı)
- [ ] Gelir: AdMob + Play Billing (Pro: anahtarsız hazır model)
- [ ] Bildirimler, tema seçenekleri, dışa aktarma

## Test listesi (AndroidIDE build sonrası)

1. İlk açılış → onboarding → Pollinations ile anahtarsız deneme
2. Gemini anahtarı ekle → sohbet → cevabın doğru balonda geldiğini gör
3. Durdur butonu → yazma anında kesilmeli
4. Ajan modunu aç → "merhaba.txt diye bir dosya oluştur" → onay diyaloğu →
   uygulama deposuna yazıldığını doğrula
5. Proje klasörü seç → ajanın oraya yazdığını doğrula
