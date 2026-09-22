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

### v0.6 — KiraAI + "…" Ölümü + Animasyonlu Ana Ekran
- [x] Ücretsiz Araçlar ekranı KALDIRILDI (kullanıcı isteği)
- [x] KiraAI preset + reasoning_content köprüsü + animasyonlu yazıyor
      göstergesi + boş-yanıt bekçisi + animasyonlu ana ekran

### v0.7 — Sessiz Ölüm Yok + Atria + KiraAI Ücretsiz
- [x] SSE-içi hata zarfı artık yakalanıyor (200 içinde {"error"} =
      eskiden sessiz "…", şimdi net hata + kota algısı)
- [x] Sıfır-olay koruması: hiç SSE gelmeden kapanan akış hata verir
      (Duck.ai dahil), sessiz "…" imkansız
- [x] Canlı geçen süre: "yazıyor… 12 sn" (bekçi hâlâ 60 sn —
      bedava düşünme modelleri yavaş olabilir)
- [x] Atria preset (api.atria-asi.ai/v1, Bearer, doğrulanmış;
      100M token kampanya, model: Atria-Dawn-Preview)
- [x] KiraAI güncellendi: varsayılan ücretsiz kira-mini-1.0 +
      dokümandan doğrulanmış 15 model ID'si
- [x] 1min.ai HÂLÂ BEKLEMEDE: 2 arama + docs + relay denemesi sonuçsuz
      (relay reposu silinmiş/404). Kullanıcı panelinden 1 curl örneği
      gönderince özel istemci yazılacak.

## Ücretsiz AI gerçekleri (araştırma notu)

- Gerçekten "limitsiz + tokensiz + anahtarsız + güçlü" AI yoktur.
  En yakınları: Pollinations (anahtarsız) ve Duck.ai (deneysel).
- KiraAI: %100 OpenAI uyumlu, Bearer auth. `kira-mini-1.0` dokümanda
  açıkça ücretsiz. "Free" sonekli promosyon modellerin bir kısmı
  Eylül 2026'da bitirildi (kendi duyuruları) — o yüzden preset,
  güncel dokümandaki ID'leri kullanır.
- Atria: `/v1/chat/completions` canlı doğrulandı (üçüncü parti PR),
  anahtar `atr_...`, model adı büyük-küçük harf duyarlı.
  max_tokens aralığı 1-65536 (bizim slider max 8192 → güvenli).
- Sessiz "…"nın 4 sebebi vardı, 4'ü de kapatıldı: (1) eski APK,
  (2) reasoning_content yok sayma, (3) SSE-içi error zarfı,
  (4) SSE-dışı/boş 200 yanıtı. Artık her durumda ya metin ya hata.
- 1min.ai: API ürünü mevcut ama tel formatı kamuya doğrulanamadı.

## Sıradaki (önerilen sıra)

### v0.8 — 1min.ai Özel İstemci (kullanıcı curl gönderince)
- [ ] Sohbet endpoint formatı → OneMinProvider.kt

### v0.9 — Dosya Gezgini + Editör
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme + `delete_file` aracı (onaylı)

### v1.0 — Ajan Hafızası + Terminal + Git + Gelir
- [ ] Sohbet özeti, kısıtlı komut çalıştırma, Git, AdMob/Pro

## Test listesi (AndroidIDE build sonrası)

1. Build başarılı
2. Atria ekle (atr_ anahtarı) → sohbet et
3. KiraAI ekle → varsayılan kira-mini-1.0 ücretsiz dene
4. Bozuk model ID'si yaz → net hata mesajı (sessiz "…" YOK)
5. "yazıyor… N sn" sayacı görünüyor mu
