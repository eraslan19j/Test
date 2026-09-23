# ReDHawK Code — Yol Haritası

Hedef: mobilde Codex seviyesinde, ücretsiz modellerle çalışan en güçlü yapay zeka aracı.

## Mimari (özet)

```
ui/            → Compose ekranları (chat, files, provider, settings, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu + otomatik sağlayıcı geçişi
ui/chat/       → ThinkingPanel (iki seviyeli) + ThinkingParser + TurkishGuard
ui/files/      → Dosya gezgini + metin editörü (ajan deposu / proje klasörü)
agent/         → AgentTools + ToolIntentParser (metin-içi niyet) + ProjectFiles + WebTools
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

### v0.2 – v0.12 (özet)
- [x] Ajan Faz 1+2, kartal ikon, animasyonlu ana ekran
- [x] 19 sağlayıcı kartı (~75 model), otomatik kota geçişi
- [x] Sessiz "…" ölü, dil seçeneği, referans ThinkingPanel
- [x] Türkçe garantisi v1, balonsuz AI, TTS, zengin menü
- [x] Gerçek açık/koyu/sistem teması, selamlamalı ana ekran
- [x] TTS build fix, Dosyalar ekranı (gezgin + editör)
- [x] Metin-içi araç niyeti v1 (<tool>, fonksiyon, doğal dil)

### v0.13 — Kök Çözüm: Guard v2 + Niyet v2
- [x] BELGE-düzeyi İngilizce kararı: kod dışı metnin tamamı
      İngilizceyse toptan panele taşınır (cümle sezgilerinin
      deliği kapatıldı: "Use tool list_files with empty path.")
- [x] Cümle düzeyi güçlendi: "I can", "use tool", "to list" gibi
      25+ yeni önek; use/tool/list/file/path kelimeleri geri eklendi
      (araç adları TOOLX'e nötrlenir, Türkçe cümle yakalanmaz)
- [x] USE_TOOL kalıbı genellendi: "Use (tool) list_files" arası
      kelimeler opsiyonel ("Use tool list_files with empty path"
      ARTIK ÇALIŞIR)
- [x] Boşluklu doğal dil: "I can list files" → kök listelenir
      (eylem kapısıyla: salt açıklama cümleleri çalışmaz)
- [x] Claimed-aralıkları: aynı metin parçası iki kez çalışmaz
- [x] AGENT_PROMPT: "sormadan önce bak, yapmadan 'yapabilirim' deme"

## Bilinen doğrular (ekran görüntülerinden)

- Küçük modeller 3 şekilde düşünme sızdırır, 3'ü de kapalı.
- Paneldeki düşünmenin İngilizce kalması NORMALDİR (referansta da öyle).
- Küçük modeller function-calling bilmez → metin niyeti şarttır.
- Küçük modeller "yapabilirim" deyip araç çalıştırmaz → prompt'ta yasak.
- Pollinations anahtarsız öldü. Bedava: LLM7, OVH, Duck.ai, Dahl,
  NaraRouter, KiraAI-mini, Atria.

## Sıradaki (referans uygulamadan — önerilen sıra)

### v0.14 — Ajan Dosya Araçları v2
- [ ] `delete_file` ajan aracı (onaylı) + write_file diff önizleme

### v1.0 — Terminal + Proje + Kurallar
- [ ] Kısıtlı komut çalıştırma (onaylı), proje kökü, redhawk.json
- [ ] Menüye "Agent Terminali" + "Kurallar" (o zaman gerçek olur)

### v1.1 — Entegrasyonlar + Gelir
- [ ] Git/GitHub, MCP, SSH, ADB + AdMob/Pro
- [ ] Menüye "GitHub depoları" vb. (o zaman gerçek olur)

## Test listesi (AndroidIDE build sonrası)

1. Ajan AÇIK, "bu klasörde ne var" → "⚙ list_files çalışıyor…"
   + Türkçe dosya listesi (soru sormadan!)
2. "gördüğün klasörlerin isimlerini söyle" → balonda İngilizce YOK,
   ham metin kapalı panelde
3. "I can list files" tarzı cevapta araç GERÇEKTE çalışıyor mu
4. "```kod``` + Türkçe açıklama" cevabı bozulmadı mı
5. Dosyalar ekranındaki içerikle ajanın listesi aynı mı
