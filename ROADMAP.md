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

### v0.2 – v0.7.1 (özet)
- [x] Ajan Faz 1+2 (dosya + web araçları, yazma onayı, SAF klasör)
- [x] Kartal ikon her yerde, animasyonlu ana ekran, balon cilası
- [x] 15 sağlayıcı kartı (~60 model), otomatik kota geçişi
- [x] Sessiz "…"nın 4 sebebi kapatıldı (SSE hata zarfı, sıfır-olay
      koruması, reasoning köprüsü, boş-yanıt bekçisi) + canlı sayaç
- [x] Duck.ai (anahtarsız, deneysel), NVIDIA, Z.ai, Atria, KiraAI

### v0.8 — Dil Seçeneği + Düşünme Sızıntısı Fix + Dosya Paneli
- [x] Yanıt dili ayarı (Türkçe / English / Otomatik) → sistem
      promptuna direktif olarak eklenir, sohbete anında uygulanır
- [x] Varsayılan sistem promptu düzgün Türkçeye çevrildi
- [x] KRİTİK FIX: açılışsız kapanış etiketi (</think> tek başına)
      artık yakalanıyor — düşünme cevaba sızmaz
- [x] Güvenlik ağı: yanıta sızmış tüm etiket artıkları temizlenir
- [x] Düşünme paneli dosya-kartı tarzına çevrildi (klasör ikonu,
      giriş animasyonu, kırmızı zaman çizgisi)

## Bilinen doğrular (ekran görüntülerinden)

- Küçük modeller (kira-mini vb.) İNGİLİZCE DÜŞÜNÜR — bu normaldir,
  referans uygulamada da düşünme İngilizcedir. Dil ayarı CEVABI
  garanti eder; paneldeki düşünme İngilizce kalabilir.
- Pollinations artık hesap+kredi istiyor (anahtarsız öldü). Bedava
  deneme için: Duck.ai veya KiraAI kira-mini-1.0 (anahtarlı).

## Sıradaki (referans uygulamadan — önerilen sıra)

### v0.9 — Dosya Gezgini + Editör (referans: Dosyalar, Editör)
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme + `delete_file` aracı (onaylı)

### v1.0 — Terminal + Proje (referans: Agent Terminali, Proje seç)
- [ ] Kısıtlı komut çalıştırma (onaylı) + proje kökü seçimi

### v1.1 — Kurallar + Yapılandırma (referans: Kurallar, redhawk.json)
- [ ] redhawk.json proje kuralları + Skills referans ekranı

### v1.2 — Entegrasyonlar (referans: GitHub, MCP, SSH, ADB)
- [ ] Git (commit/geri al), GitHub depoları, MCP sunucuları,
      SSH anahtarları, ADB yapılandırması

### v1.3 — Gelir (referans: Abonelik Pro)
- [ ] AdMob + Play Billing Pro (anahtarsız hazır model + reklamsız)

## Test listesi (AndroidIDE build sonrası)

1. Build başarılı
2. KiraAI kira-mini-1.0 → "merhaba" → cevap SADECE Türkçe,
   balonda İngilizce düşünme + </think> YOK
3. Düşünme paneli dosya-kartı görünümünde, açılır-kapanır
4. Ayarlar → Yanıt dili → English → cevap İngilizce
5. Ayarlar → Yanıt dili → Otomatik → model serbest
