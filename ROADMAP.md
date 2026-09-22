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
- [x] Kartal logo her yerde: launcher + dairesel uygulama içi ikon + splash
- [x] Duck.ai (anahtarsız, deneysel) + otomatik sağlayıcı geçişi
- [x] İlk-token bekçisi (60 sn) + kendi mesajını dokunarak kopyalama

### v0.5 — Build Fix + NVIDIA + Z.ai
- [x] Build fix: `send(): Job` dönüş tipi (recursive type inference hatası)
- [x] NVIDIA NIM + Z.ai GLM presetleri
- [x] Ücretsiz Araçlar ekranı eklendi, sonra v0.6'da KALDIRILDI (kullanıcı isteği)

### v0.6 — KiraAI + "…" Ölümü + Animasyonlu Ana Ekran
- [x] KiraAI preset (kiraai.vn/api/v1, OpenAI uyumlu, ücretli anahtar)
- [x] Düşünme modeli köprüsü: reasoning_content → düşünme paneli
      (DeepSeek R1 tarzı modeller artık dakikalarca "…" gibi durmaz)
- [x] Boş-yanıt bekçisi: sessiz "…" yerine "Model boş yanıt döndü" uyarısı
- [x] Animasyonlu "yazıyor…" göstergesi (nabız atan 3 nokta)
- [x] Ana ekran: animasyonlu arka plan (süzülen ışıklar) + kademeli
      kart girişleri + nabız atan logo + degrade ikonlar
- [x] 1min.ai BEKLEMEDE: resmi API özel format (OpenAI-uyumlu değil),
      docs JS-gated; endpoint doğrulanamadı (bak: Sıradaki)

## Ücretsiz AI gerçekleri (araştırma notu)

- Gerçekten "limitsiz + tokensiz + anahtarsız + güçlü" AI yoktur.
  En yakınları: Pollinations (anahtarsız) ve Duck.ai (deneysel).
- KiraAI: OpenAI-SDK uyumlu (`/api/v1`), ücretli anahtar; bedava
  promosyonları Eylül 2026'da tek tek bitiyor (kendi duyuruları).
  Model ID'leri sayfa slug'larından alındı; 404 verirse sitedeki
  tam ID'yi model alanına yapıştır.
- 1min.ai: Relay projeleri var ama resmi uç OpenAI-uyumlu DEĞİL.
  Kullanıcının panelindeki sohbet endpoint örneği gelince özel
  istemci (OneMinProvider) yazılacak.
- Strateji: BİRDEN FAZLA sağlayıcı ekle → kota biten otomatik
  diğerine geçer = pratikte kesintisiz kullanım.

## Sıradaki (önerilen sıra)

### v0.7 — 1min.ai Özel İstemci (kullanıcı endpoint gönderince)
- [ ] docs.1min.ai sohbet endpoint formatı → OneMinProvider.kt
- [ ] Kredi bakiyesi göstergesi (destekleniyorsa)

### v0.8 — Dosya Gezgini + Editör
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme + `delete_file` aracı (onaylı)

### v0.9 — Ajan Hafızası + Terminal + Git
- [ ] Sohbet özeti / bağlam yönetimi, kısıtlı komut çalıştırma
- [ ] Git entegrasyonu (commit/geri al = ajan güvenlik ağı)

### v1.0 — Gelir
- [ ] AdMob + Play Billing Pro (anahtarsız hazır model + reklamsız)

## Test listesi (AndroidIDE build sonrası)

1. Build başarılı
2. KiraAI ekle (satın aldığın anahtar) → sohbet et
3. Düşünme modeli dene (örn. DeepSeek R1 türevi) → düşünme paneli akıyor mu
4. Ana ekran: arka plan ışıkları + kart animasyonları
5. Menüde "Ücretsiz Araçlar" YOK (kaldırıldı)
