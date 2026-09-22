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
Ölü buton YASAK: menüye eklenen her şey çalışmak zorunda.

## Yapıldı

### v0.2 – v0.9 (özet)
- [x] Ajan Faz 1+2, kartal ikon, animasyonlu ana ekran
- [x] 19 sağlayıcı kartı (~75 model), otomatik kota geçişi
- [x] Sessiz "…" ölü, dil seçeneği, dosya-kartı düşünme paneli
- [x] Titreşimler, parlama çerçevesi (v0.10'da balonla gitti)

### v0.10 — Saf Türkçe + Balonsuz AI + Zengin Menü + Gerçek Tema
- [x] KRİTİK: etiketsiz İngilizce giriş temizliği (stripPreamble) —
      "The user asks..." tarzı cümleler baştan atılır, SADECE arkada
      Türkçe varsa. Tam-İngilizce cevaplara dokunulmaz.
- [x] Dil direktifi sertleşti: giriş cümlesi yasağı eklendi
- [x] AI yanıtları balonsuz + tam genişlik (referans tarzı)
- [x] Sesli okuma (TTS): yanıt başına 🔊 butonu, Türkçe ses
- [x] Sol menü zenginleşti: Yeni Sohbet, Yanıt Dili, Tema: Koyu/Açık
      (hepsi çalışıyor — ölü buton yok)
- [x] GERÇEK açık tema: RedHawkTheme prefs'i okur (koyu/açık/sistem)
- [x] Ana ekran kahramanı: saate göre selamlama
      (Günaydın/İyi günler/İyi akşamlar/İyi geceler)

## Bilinen doğrular (ekran görüntülerinden)

- Küçük modeller 3 şekilde düşünme sızdırır, 3'ü de kapalı:
  (1) <think> etiketli → panele, (2) kapanış-tek → panele,
  (3) etiketsiz İngilizce giriş → çöpe (Türkçe varsa).
- Paneldeki düşünmenin İngilizce kalması NORMALDİR (referansta da öyle).
- Pollinations anahtarsız öldü. Bedava: LLM7, OVH, Duck.ai, Dahl,
  NaraRouter, KiraAI-mini, Atria.

## Sıradaki (referans uygulamadan — önerilen sıra)

### v0.11 — Dosya Gezgini + Editör (referans: Dosyalar, Editör)
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme + `delete_file` aracı (onaylı)
- [ ] Menüye "Dosyalar" satırı (o zaman gerçek olur)

### v1.0 — Terminal + Proje + Kurallar
- [ ] Kısıtlı komut çalıştırma (onaylı), proje kökü, redhawk.json
- [ ] Menüye "Agent Terminali" + "Kurallar" (o zaman gerçek olur)

### v1.1 — Entegrasyonlar + Gelir
- [ ] Git/GitHub, MCP, SSH, ADB + AdMob/Pro
- [ ] Menüye "GitHub depoları" vb. (o zaman gerçek olur)

## Test listesi (AndroidIDE build sonrası)

1. Build başarılı
2. KiraAI kira-mini-1.0 → "hava durumu nasıl İSTANBUL" →
   balonda SADECE Türkçe (The user asks... YOK)
3. AI yanıtı balonsuz, tam genişlik; kullanıcı balonu kırmızı
4. 🔊 → yanıtı Türkçe sesli okuyor mu
5. Menü: Yeni Sohbet + Yanıt Dili + Tema satırları çalışıyor mu
6. Tema: Koyu → açık tema gerçekten açılıyor mu
7. Ana ekranda saate uygun selamlama
