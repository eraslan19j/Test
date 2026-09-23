# ReDHawK Code — Yol Haritası

Hedef: mobilde Codex seviyesinde, ücretsiz modellerle çalışan en güçlü yapay zeka aracı.

## Mimari (özet)

```
ui/            → Compose ekranları (chat, files, provider, settings, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu + otomatik sağlayıcı geçişi
ui/chat/       → ThinkingPanel (iki seviyeli) + ThinkingParser + TurkishGuard
ui/files/      → Dosya gezgini + metin editörü (ajan deposu / proje klasörü)
agent/         → AgentTools + ToolIntentParser + ProjectFiles + WebTools + Terminal
llm/           → LlmProvider arayüzü + OpenAI-uyumlu istemci + DuckAiProvider (anahtarsız)
data/db        → Room (chats, messages, providers)
data/prefs     → DataStore ayarlar
data/provider  → ProviderCatalog (hazır presetler)
```

Kural: yeni yapay zeka = `ProviderCatalog`'a 1 preset (+gerekirse ProviderFactory'e 1 dal).
Yeni ajan aracı = `AgentTools.specs`'e 1 ToolSpec + `execute`'e 1 dal +
`ToolIntentParser.TOOL_NAMES`'e adı ekle (metin niyeti de çalışsın).
Manuel sağlayıcı = kurulum ekranındaki "Özel uç" kartı (URL + key + model).
Ölü buton YASAK: menüye eklenen her şey çalışmak zorunda.

## Yapıldı

### v0.2 – v0.13 (özet)
- [x] Ajan Faz 1+2, kartal ikon, animasyonlu ana ekran
- [x] 19 sağlayıcı kartı (~75 model), otomatik kota geçişi
- [x] Sessiz "…" ölü, dil seçeneği, referans ThinkingPanel
- [x] Türkçe garantisi v1-v2, balonsuz AI, TTS, zengin menü
- [x] Gerçek açık/koyu/sistem teması, selamlamalı ana ekran
- [x] TTS build fix, Dosyalar ekranı (gezgin + editör)
- [x] Metin-içi araç niyeti v1-v2, belge-düzeyi İngilizce kararı

### v0.14 — Gerçek Auto + Guard 2.1 + Terminal
- [x] KÖK NEDEN BULUNDU: dil "auto" iken Guard kapalı + direktif
      yoktu = ham İngilizce. Artık "auto" mesajdan dili anlar
      (Türkçe karakter + 70 Türkçe ipucu kelime): Türkçe→Türkçe
      direktif + Guard, İngilizce→İngilizce
- [x] toUi güvenlik ağı: guardsız yazılmış eski mesajlar bile
      ekranda temizlenir (idempotent, bilgi panele taşınır)
- [x] Guard 2.1: yapışık cümle bölünür ("summarize.-"), "according
      to / the response shows / so we can" önekleri, güçlü sinyal
      (skor≥3) kod-vetosunu deler
- [x] Terminal: run_command ajanı (ls, cat, grep, find, git
      status/log/diff...) — salt-okunur allowlist, shell yok,
      10 sn + 8KB sınır, uygulama deposunda çalışır
- [x] Araştırma doğruladı: prompt'a güven %82 başarısız, çıktı
      denetimi (output rails) endüstri standardı

## Bilinen doğrular (ekran görüntülerinden)

- Küçük modeller 3 şekilde düşünme sızdırır, 3'ü de kapalı.
- Paneldeki düşünmenin İngilizce kalması NORMALDİR (referansta da öyle).
- Küçük modeller function-calling bilmez → metin niyeti şarttır.
- Küçük modeller "yapabilirim" deyip araç çalıştırmaz → prompt'ta yasak.
- Klasör görme terminal gerektirmez (SAF + dosya API'leri yeter).
- Pollinations anahtarsız öldü. Bedava: LLM7, OVH, Duck.ai, Dahl,
  NaraRouter, KiraAI-mini, Atria.

## Sıradaki (referans uygulamadan — önerilen sıra)

### v0.15 — Ajan Dosya Araçları v2
- [ ] `delete_file` ajan aracı (onaylı) + write_file diff önizleme

### v1.0 — Proje + Kurallar
- [ ] Proje kökü, redhawk.json, tam terminal yetkisi (onaylı)
- [ ] Menüye "Agent Terminali" + "Kurallar" (o zaman gerçek olur)

### v1.1 — Entegrasyonlar + Gelir
- [ ] Git/GitHub, MCP, SSH, ADB + AdMob/Pro
- [ ] Menüye "GitHub depoları" vb. (o zaman gerçek olur)

## Test listesi (AndroidIDE build sonrası)

1. Dil "Otomatik" iken Türkçe sor → balon saf Türkçe mi
2. Dil "Otomatik" iken "list files in this folder" (İngilizce sor)
   → İngilizce cevap geliyor mu (doğru davranış)
3. ESKİ İngilizce mesajlar bile temiz görünüyor mu (toUi ağı)
4. Ajan AÇIK (uygulama deposu), "ls -la çalıştır" → komut
   sonucu Türkçe özetle geliyor mu
5. Bağlı klasörde (SAF) run_command → düzgün HATA + dosya
   araçlarına yönlendirme var mı
6. "summarize.-" yapışık cümleler ayrışıyor mu
