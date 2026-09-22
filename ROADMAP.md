# ReDHawK Code — Yol Haritası

Hedef: mobilde Codex seviyesinde, ücretsiz modellerle çalışan en güçlü yapay zeka aracı.

## Mimari (özet)

```
ui/            → Compose ekranları (chat, provider, settings, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu + otomatik sağlayıcı geçişi
agent/         → AgentTools (dosya+internet) + ProjectFiles (SAF + app deposu) + WebTools
llm/           → LlmProvider arayüzü + OpenAI-uyumlu istemci + DuckAiProvider (anahtarsız)
data/db        → Room (chats, messages, providers)
data/prefs     → DataStore ayarlar
data/provider  → ProviderCatalog (hazır presetler)
```

Kural: yeni yapay zeka = `ProviderCatalog`'a 1 preset (+gerekirse ProviderFactory'e 1 dal).
Yeni ajan aracı = `AgentTools.specs`'e 1 ToolSpec + `execute`'e 1 dal.
Manuel sağlayıcı = kurulum ekranındaki "Özel uç" kartı (URL + key + model).

## Yapıldı

### v0.2 — Sağlamlaştırma + Ajan Faz 1
- [x] Splash, onboarding, thinking bug'ı, durdur butonu, katalog düzeltmeleri
- [x] AJAN FAZ 1: tool_calls + dosya araçları + yazma onayı + SAF klasör

### v0.3 — İkon + Performans + İnternet + Gerçek Ayarlar
- [x] Adaptive launcher ikonu, canlı aktivite paneli, kaydırma performansı
- [x] AJAN FAZ 2: web_search + fetch_url (ücretsiz)
- [x] Gerçek ayarlar (sistem promptu, sıcaklık, token) + ölü satır temizliği

### v0.4 — Gerçek Kartal İkonu + Limitsiz Hissi + Duck.ai
- [x] Kartal logo her yerde: launcher (güvenli alanlı) + dairesel uygulama
      içi ikon + splash + temalı ikon silueti
- [x] YENİ ÜCRETSİZ AI: Duck.ai (anahtarsız GPT-4o mini vb, deneysel)
- [x] Otomatik sağlayıcı geçişi: kota biterse (429/402/403) yedeğe geçip
      aynı mesajı otomatik tekrar dener (max 3, ücretsizler öncelikli)
- [x] İlk-token bekçisi (60 sn): takılan "…" balonu tarihe karıştı
- [x] Kendi mesajına dokunarak kopyalama + "Kopyalandı" bildirimi

## Ücretsiz AI gerçekleri (araştırma notu)

- Gerçekten "limitsiz + tokensiz + anahtarsız + güçlü" AI yoktur; faturayı
  biri öder. En yakınları: Pollinations (anahtarsız) ve Duck.ai (deneysel).
- Uzun limitli anahtarlı ücretsizler: Gemini AI Studio, Groq, OpenRouter :free,
  Mistral deneme, GitHub Models, Cloudflare Workers AI.
- Strateji: BİRDEN FAZLA ücretsiz sağlayıcı ekle → kota biten otomatik
  diğerine geçer = pratikte kesintisiz ücretsiz kullanım.
- Manuel ekleme: Sağlayıcı Ekle → "Özel uç" (OpenAI-uyumlu her URL olur).

## Sıradaki (önerilen sıra)

### v0.5 — Dosya Gezgini + Editör
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme + `delete_file` aracı (onaylı)

### v0.6 — Ajan Hafızası + Terminal + Git
- [ ] Sohbet özeti / bağlam yönetimi, kısıtlı komut çalıştırma
- [ ] Git entegrasyonu (commit/geri al = ajan güvenlik ağı)

### v0.7 — Gelir
- [ ] AdMob + Play Billing Pro (anahtarsız hazır model + reklamsız)

## Test listesi (AndroidIDE build sonrası)

1. Launcher ikonu = kartal, taşma yok; uygulama içi daire ikonlar kartal
2. Duck.ai ekle (anahtarsız) → sohbet et
3. 2+ sağlayıcı ekle → birinin kotasını bitir (veya yanlış key gir) →
   otomatik geçiş + "ile devam ediliyor" mesajı
4. Kendi mesajına dokun → "Kopyalandı"
5. Ajan modu + "güncel bilgiyi araştırıp dosyaya yaz" (web + dosya birlikte)
