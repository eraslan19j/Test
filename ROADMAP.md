# ReDHawK Code — Yol Haritası

Hedef: mobilde Codex seviyesinde, ücretsiz modellerle çalışan en güçlü yapay zeka aracı.

## Mimari (özet)

```
ui/            → Compose ekranları (chat, provider, settings, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu
agent/         → AgentTools (araçlar) + ProjectFiles (SAF + app deposu) + WebTools (internet)
llm/           → LlmProvider arayüzü + OpenAI-uyumlu istemci (SSE + tool_calls)
data/db        → Room (chats, messages, providers)
data/prefs     → DataStore ayarlar
data/provider  → ProviderCatalog (hazır presetler)
```

Kural: yeni yapay zeka = `ProviderCatalog`'a 1 preset.
Yeni ajan aracı = `AgentTools.specs`'e 1 ToolSpec + `execute`'e 1 dal.

## Yapıldı

### v0.2 — Sağlamlaştırma + Ajan Faz 1
- [x] Splash düzeltmesi, onboarding, thinking bug'ı, durdur butonu
- [x] Sağlayıcı kataloğu düzeltmeleri + anahtarsız Pollinations
- [x] AJAN FAZ 1: tool_calls + list/read/write_file + yazma onayı + SAF klasör

### v0.3 — İkon + Performans + İnternet + Gerçek Ayarlar
- [x] Profesyonel adaptive launcher ikonu (safe-zone + degrade + temalı ikon)
- [x] Canlı aktivite paneli (durum + canlı süre + açılır düşünme)
- [x] Performans: akışta animasyonsuz kaydırma, satır animasyonları kaldırıldı
- [x] AJAN FAZ 2: ücretsiz web_search (DuckDuckGo) + fetch_url araçları
- [x] Gerçek ayarlar: sistem promptu editörü, sıcaklık, maksimum token
      (sıcaklık/token API isteğine gerçekten ekleniyor)
- [x] Ölü ayar satırları temizlendi (terminal/dil/klasör sahteleri gitti)
- [x] Pollinations model listesi genişletildi

## Sıradaki (önerilen sıra)

### v0.4 — Dosya Gezgini + Editör
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme (yazmadan önce farkı göster)
- [ ] `delete_file` aracı (onaylı)

### v0.5 — Ajan Hafızası + Terminal
- [ ] Sohbet özeti / bağlam yönetimi (uzun sohbetlerde şişmeyi önle)
- [ ] Kısıtlı komut çalıştırma (izinli komut listesiyle)
- [ ] Git entegrasyonu (commit/geri al = ajan güvenlik ağı)

### v0.6 — Gelir
- [ ] AdMob (ücretsiz katman)
- [ ] Play Billing Pro (anahtarsız hazır model + reklamsız)

## Test listesi (AndroidIDE build sonrası)

1. Launcher ikonu: yuvarlak/kare maskelerde düzgün görünmeli, taşma yok
2. Sohbet: akış sırasında kasma yok, aktivite panelinde süre canlı işliyor
3. Ayarlar → sıcaklık/token değiştir → cevap tarzı/uzunluğu değişmeli
4. Ajan modu: "güncel Kotlin sürümünü internetten araştır" → web_search
   kullanmalı; "sonucu arastirma.txt dosyasına yaz" → onay + yazma
5. Proje klasörü seç → ajanın oraya yazdığını doğrula
