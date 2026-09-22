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

### v0.2 – v0.8 (özet)
- [x] Ajan Faz 1+2, kartal ikon, animasyonlu ana ekran, balon cilası
- [x] 15 sağlayıcı kartı, otomatik kota geçişi, sessiz "…" ölü
- [x] Duck.ai, NVIDIA, Z.ai, Atria, KiraAI, dil seçeneği (TR/EN/Oto)
- [x] Düşünme sızıntı fix'i + dosya-kartı düşünme paneli

### v0.9 — Yüksek Tokenlılar + Premium Dokunuşlar
- [x] Dahl preset (100M token/anahtar, MiniMax-M2.7, doğrulandı)
- [x] NaraRouter preset (günde 7M, 30+ model, doğrulandı)
- [x] LLM7 preset (ANAHTARSIZ turbo: DeepSeek V4, GLM 5.3,
      Codestral — /v1/models'tan doğrulandı)
- [x] OVH preset (ANAHTARSIZ: gpt-oss, Qwen3, Llama-3.3-70B,
      Mistral — /v1/models'tan doğrulandı)
- [x] Dokunsal premium: gönder/durdur + ana ekran kartları titreşimli
- [x] Akan balonda nabız gibi atan parlama çerçevesi
      (bitince animasyon durur, pil/CPU dostu)
- [x] Asistan kopyalamada "Kopyalandı" bildirimi
- [x] EKLENMEYENLER (gerekçeli): Aion (OpenAI-uyumsuz özel API),
      LongCat (kota doğrulanamadı), Kilo (uç yolu uyumsuz),
      Tencent/Baidu/Spark (Çin kimlik onayı + özel API),
      Copilot köprüsü (MS oturumu gerekir), Puter (user-pays JS),
      MonkeyCode (REST API değil), APmix/AnyModel/Token-Free
      (doğrulanabilir doküman yok)

## Ücretsiz AI gerçekleri (araştırma notu)

- Doğrulama kuralı: /v1/models veya resmi docs görülmeden preset YOK.
  Model ID'si tahmin edilmez (Dahl docs'un kendi uyarısı).
- Anahtarsız üçlü: LLM7 (~10/dk turbo), OVH (~2/dk/model),
  Duck.ai (deneysel). Pollinations anahtarsız öldü (kredi istiyor).
- Yüksek tokenlı anahtarlılar: Dahl (100M/anahtar), NaraRouter
  (7M/gün), Atria (100M kampanya), KiraAI kira-mini-1.0.
- Strateji değişmedi: birden fazla ücretsiz ekle, kota biten
  otomatik diğerine geçsin.

## Sıradaki (referans uygulamadan — önerilen sıra)

### v0.10 — Dosya Gezgini + Editör
- [ ] Ajan deposunu uygulamada gör/düzenle (gezgin + metin editörü)
- [ ] write_file için diff önizleme + `delete_file` aracı (onaylı)

### v1.0 — Terminal + Proje + Kurallar
- [ ] Kısıtlı komut çalıştırma (onaylı), proje kökü, redhawk.json

### v1.1 — Entegrasyonlar + Gelir
- [ ] Git/GitHub, MCP, SSH, ADB + AdMob/Pro

## Test listesi (AndroidIDE build sonrası)

1. Build başarılı
2. LLM7 ekle (anahtarsız) → DeepSeek-V4-Flash-0731 ile sohbet
3. OVH ekle (anahtarsız) → gpt-oss-20b ile sohbet
4. Dahl ekle (100M anahtar) → sohbet et
5. NaraRouter ekle (Google kaydı) → sohbet et
6. Gönder/durdur + kart dokunuşlarında titreşim
7. Akan balonda parlama çerçevesi atıyor mu
