# ReDHawK Code — Yol Haritası

Hedef: mobilde Codex seviyesinde, ücretsiz modellerle çalışan en güçlü yapay zeka aracı.

## Mimari (özet)

```
ui/            → Compose ekranları (chat, provider, settings, tools, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu + otomatik sağlayıcı geçişi
ui/tools/      → Ücretsiz Araçlar = web araçlarını uygulama içinden açan WebView merkezi
agent/         → AgentTools (dosya+internet) + ProjectFiles (SAF + app deposu) + WebTools
llm/           → LlmProvider arayüzü + OpenAI-uyumlu istemci + DuckAiProvider (anahtarsız)
data/db        → Room (chats, messages, providers)
data/prefs     → DataStore ayarlar
data/provider  → ProviderCatalog (hazır presetler)
```

Kural: yeni yapay zeka = `ProviderCatalog`'a 1 preset (+gerekirse ProviderFactory'e 1 dal).
Yeni ajan aracı = `AgentTools.specs`'e 1 ToolSpec + `execute`'e 1 dal.
Manuel sağlayıcı = kurulum ekranındaki "Özel uç" kartı (URL + key + model).
Web aracı = `FREE_TOOLS` listesine 1 satır.

## Yapıldı

### v0.2 — Sağlamlaştırma + Ajan Faz 1
- [x] Splash, onboarding, thinking bug'ı, durdur butonu, katalog düzeltmeleri
- [x] AJAN FAZ 1: tool_calls + dosya araçları + yazma onayı + SAF klasör

### v0.3 — İkon + Performans + İnternet + Gerçek Ayarlar
- [x] Adaptive launcher ikonu, canlı aktivite paneli, kaydırma performansı
- [x] AJAN FAZ 2: web_search + fetch_url (ücretsiz)
- [x] Gerçek ayarlar (sistem promptu, sıcaklık, token) + ölü satır temizliği

### v0.4 — Gerçek Kartal İkonu + Limitsiz Hissi + Duck.ai
- [x] Kartal logo her yerde: launcher + dairesel uygulama içi ikon + splash
- [x] Duck.ai (anahtarsız, deneysel) + otomatik sağlayıcı geçişi
- [x] İlk-token bekçisi (60 sn) + kendi mesajını dokunarak kopyalama

### v0.5 — Build Fix + Ücretsiz Araçlar + NVIDIA + Z.ai
- [x] Build fix: `send(): Job` dönüş tipi (recursive type inference hatası)
- [x] Ücretsiz Araçlar ekranı (WebView merkezi): Genspark, video
      yükseltici, Arena AI, Duck.ai web, Pollinations web
- [x] NVIDIA NIM preset (ücretsiz anahtar, 40 istek/dk, DeepSeek/Kimi/Llama)
- [x] Z.ai GLM preset (yeni üyelikte ücretsiz kredi)
- [x] EKLEMEYECEKLER (bilinçli red): sahte/geçici-eposta hesap otomasyonu
      (servis banı + Play politikası riski), Cohere (uyumsuz API formatı)

## Ücretsiz AI gerçekleri (araştırma notu)

- Gerçekten "limitsiz + tokensiz + anahtarsız + güçlü" AI yoktur.
  En yakınları: Pollinations (anahtarsız) ve Duck.ai (deneysel).
- Genspark'ın herkese açık ücretsiz sohbet API'si YOKTUR (resmi olmayan
  yollar Cloudflare + çerez ister, telefonda stabil olmaz). Uygulamada
  WebView ile açılır; kullanıcı kendi hesabıyla giriş yapar.
- Uzun limitli ücretsiz anahtarlılar: NVIDIA NIM, Gemini AI Studio, Groq,
  OpenRouter :free, Z.ai, Mistral deneme, GitHub Models, Cloudflare.
- Strateji: BİRDEN FAZLA ücretsiz sağlayıcı ekle → kota biten otomatik
  diğerine geçer = pratikte kesintisiz ücretsiz kullanım.
- "Ücretsiz limitsiz" kampanya siteleri haftada bir değişir; o yüzden
  Ücretsiz Araçlar listesi link-merkezlidir, kırılgan kazıyıcı değildir.

## Sıradaki (önerilen sıra)

### v0.6 — Dosya Gezgini + Editör
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme + `delete_file` aracı (onaylı)

### v0.7 — Ajan Hafızası + Terminal + Git
- [ ] Sohbet özeti / bağlam yönetimi, kısıtlı komut çalıştırma
- [ ] Git entegrasyonu (commit/geri al = ajan güvenlik ağı)

### v0.8 — Gelir
- [ ] AdMob + Play Billing Pro (anahtarsız hazır model + reklamsız)

## Test listesi (AndroidIDE build sonrası)

1. Build başarılı (send(): Job fix)
2. Menü → Ücretsiz Araçlar → Genspark / video yükseltici açılıyor mu
3. NVIDIA ekle (build.nvidia.com anahtarı) → sohbet et
4. Z.ai ekle → sohbet et
5. 2+ sağlayıcı → kota bitince otomatik geçiş
