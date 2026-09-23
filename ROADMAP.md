# ReDHawK Code — Yol Haritası

Hedef: mobilde Codex seviyesinde, ücretsiz modellerle çalışan en güçlü yapay zeka aracı.

## Mimari (özet)

```
ui/            → Compose ekranları (chat, files, provider, settings, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu + otomatik sağlayıcı geçişi
ui/files/      → Dosya gezgini + metin editörü (ajan deposu / proje klasörü)
agent/         → AgentTools (dosya+internet) + ProjectFiles (SAF + app deposu) + WebTools
llm/           → LlmProvider arayüzü + OpenAI-uyumlu istemci + DuckAiProvider (anahtarsız)
data/db        → Room (chats, messages, providers)
data/prefs     → DataStore ayarlar
data/provider  → ProviderCatalog (hazır presetler)
```

Kural: yeni yapay zeka = `ProviderCatalog`'a 1 preset (+gerekirse ProviderFactory'e 1 dal).
Yeni ajan aracı = `AgentTools.specs`'e 1 ToolSpec + `execute`'e 1 dal.
Manuel sağlayıcı = kurulum ekranındaki "Özel uç" kartı (URL + key + model).
Ölü buton YASAK: menüye eklenen her şey çalışmak zorunda.

## Yapıldı

### v0.2 – v0.10 (özet)
- [x] Ajan Faz 1+2, kartal ikon, animasyonlu ana ekran
- [x] 19 sağlayıcı kartı (~75 model), otomatik kota geçişi
- [x] Sessiz "…" ölü, dil seçeneği, dosya-kartı düşünme paneli
- [x] Saf Türkçe (3 sızıntı türü kapalı), balonsuz AI, TTS, zengin menü
- [x] Gerçek açık/koyu/sistem teması, selamlamalı ana ekran

### v0.11 — Build Fix (TTS) + Dosya Gezgini + Editör
- [x] Build fix: TTS motoru MessageItem'a parametreyle taşındı +
      kendi-kendini referans eden init düzeltildi
- [x] Dosyalar ekranı: klasör gezme, metin düzenleme, kaydetme,
      yeni dosya, onaylı silme (SAF + uygulama deposu)
- [x] ProjectFiles.delete eklendi (yerel + SAF)
- [x] Menüde "ÇALIŞMA ALANI → Dosyalar" satırı + "files" rotası
- [x] Liste/editör arası kayar-solma geçiş animasyonu

## Bilinen doğrular (ekran görüntülerinden)

- Küçük modeller 3 şekilde düşünme sızdırır, 3'ü de kapalı.
- Paneldeki düşünmenin İngilizce kalması NORMALDİR (referansta da öyle).
- Pollinations anahtarsız öldü. Bedava: LLM7, OVH, Duck.ai, Dahl,
  NaraRouter, KiraAI-mini, Atria.

## Sıradaki (referans uygulamadan — önerilen sıra)

### v0.12 — Ajan Dosya Araçları v2
- [ ] `delete_file` ajan aracı (onaylı) + write_file diff önizleme

### v1.0 — Terminal + Proje + Kurallar
- [ ] Kısıtlı komut çalıştırma (onaylı), proje kökü, redhawk.json
- [ ] Menüye "Agent Terminali" + "Kurallar" (o zaman gerçek olur)

### v1.1 — Entegrasyonlar + Gelir
- [ ] Git/GitHub, MCP, SSH, ADB + AdMob/Pro
- [ ] Menüye "GitHub depoları" vb. (o zaman gerçek olur)

## Test listesi (AndroidIDE build sonrası)

1. Build başarılı (TTS fix)
2. 🔊 → yanıtı Türkçe sesli okuyor mu
3. Menü → Dosyalar → ajan deposu listeleniyor mu
4. Ajan modunda "merhaba.txt oluştur" → Dosyalar'da görünüyor mu
5. Dosya aç → düzenle → kaydet → "GÜNCELLENDİ" bildirimi
6. Yeni dosya + silme onayı çalışıyor mu
