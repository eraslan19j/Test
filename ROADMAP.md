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

### v0.2 – v0.11 (özet)
- [x] Ajan Faz 1+2, kartal ikon, animasyonlu ana ekran
- [x] 19 sağlayıcı kartı (~75 model), otomatik kota geçişi
- [x] Sessiz "…" ölü, dil seçeneği, dosya-kartı düşünme paneli
- [x] Saf Türkçe (3 sızıntı türü kapalı), balonsuz AI, TTS, zengin menü
- [x] Gerçek açık/koyu/sistem teması, selamlamalı ana ekran
- [x] TTS build fix, Dosyalar ekranı (gezgin + editör)

### v0.12 — Referans Panel + Ajan Tamiri + Türkçe Garantisi
- [x] ThinkingPanel referans tasarıma birebir: üst "Exploring /
      N s explored", alt "Düşünüyor…", dikey çizgi, "SSE · N sn ·"
      nabzı, bitince otomatik kapanma
- [x] StreamState (Connecting / WaitingFirstEvent / Streaming / Done):
      ilk olay beklenirken "Modelden ilk gerçek olay bekleniyor"
- [x] ToolIntentParser: function-calling bilmeyen modellerin metin
      olarak yazdığı araç çağrıları GERÇEK çağrıya çevrilir
      (<tool> bloğu, name({...}) tarzı, "Use ... with empty path" vb.)
- [x] AGENT_PROMPT'a <tool> formatı öğretildi; tur başına en fazla
      3 niyet, tekrarsız, write_file yine onaylı
- [x] TurkishGuard: İngilizce cümleler silinmeden düşünme paneline
      taşınır — cevap balonunda İngilizce GARANTİ kalmaz.
      Kod blokları, satır-içi kod, yol ve URL'ler korunur.
      Cevap boşalırsa Türkçe FALLBACK yazılır.
- [x] <tool> blokları ekrana ve DB'ye sızmaz

## Bilinen doğrular (ekran görüntülerinden)

- Küçük modeller 3 şekilde düşünme sızdırır, 3'ü de kapalı.
- Paneldeki düşünmenin İngilizce kalması NORMALDİR (referansta da öyle).
- Küçük modeller function-calling bilmez → metin niyeti şarttır.
- Pollinations anahtarsız öldü. Bedava: LLM7, OVH, Duck.ai, Dahl,
  NaraRouter, KiraAI-mini, Atria.

## Sıradaki (referans uygulamadan — önerilen sıra)

### v0.13 — Ajan Dosya Araçları v2
- [ ] `delete_file` ajan aracı (onaylı) + write_file diff önizleme

### v1.0 — Terminal + Proje + Kurallar
- [ ] Kısıtlı komut çalıştırma (onaylı), proje kökü, redhawk.json
- [ ] Menüye "Agent Terminali" + "Kurallar" (o zaman gerçek olur)

### v1.1 — Entegrasyonlar + Gelir
- [ ] Git/GitHub, MCP, SSH, ADB + AdMob/Pro
- [ ] Menüye "GitHub depoları" vb. (o zaman gerçek olur)

## Test listesi (AndroidIDE build sonrası)

1. Ajan AÇIK, "bu klasörde ne var" → panelde "⚙ list_files
   çalışıyor…" görünüyor mu, sonra Türkçe liste geliyor mu
2. Ajan KAPALI, "düşün ve hikaye yaz" → balon saf Türkçe mi,
   İngilizce düşünme panelde mi (kapalı duruyor mu)
3. Akış sırasında üstte "Exploring" + "SSE · N sn ·" var mı
4. Bitince üst satır "N s explored" olup kapanıyor mu
5. "```kod``` içeren cevapta kod bozulmadı mı"
6. write_file ajan yazınca onay diyaloğu çıkıyor mu
