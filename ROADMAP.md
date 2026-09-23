# ReDHawK Code — Yol Haritası

Hedef: mobilde Codex seviyesinde, ücretsiz modellerle çalışan en güçlü yapay zeka aracı.

## Mimari (özet)

```
ui/            → Compose ekranları (chat, files, provider, permissions, settings, onboarding)
ui/chat/       → ChatViewModel = sohbet + ajan orkestrasyonu + otomatik sağlayıcı geçişi
ui/chat/       → ThinkingPanel (iki seviyeli) + ThinkingParser + TurkishGuard
ui/home/       → StarfieldBackground + HomeHeader (premium animasyonlu giriş)
ui/files/      → Dosya gezgini + metin editörü (ajan deposu / proje klasörü)
ui/permissions → Ajan izinleri (profil + izinler + chmod) → DataStore → döngüde denetim
agent/         → AgentTools (9 araç) + ToolIntentParser + ProjectFiles + WebTools + Terminal
agent/         → Permission.kt (profil/izin kataloğu + araç eşleşmesi)
llm/           → LlmProvider arayüzü + OpenAI-uyumlu istemci + DuckAiProvider (anahtarsız)
data/db        → Room (chats, messages, providers)
data/prefs     → DataStore ayarlar (dil, tema, ajan izinleri, chmod...)
data/provider  → ProviderCatalog (hazır presetler)
```

Kural: yeni yapay zeka = `ProviderCatalog`'a 1 preset (+gerekirse ProviderFactory'e 1 dal).
Yeni ajan aracı = `AgentTools.specs`'e 1 ToolSpec + `execute`'e 1 dal +
`ToolIntentParser.TOOL_NAMES`'e adı ekle + `PermissionCatalog.permissionFor`'a eşle.
(Yıkıcı araçlar doğal-dil kalıplarına EKLENMEZ.)
Manuel sağlayıcı = kurulum ekranındaki "Özel uç" kartı (URL + key + model).
Ölü buton YASAK: menüye eklenen her şey çalışmak zorunda.

## Yapıldı

### v0.2 – v0.14 (özet)
- [x] Ajan Faz 1+2, kartal ikon, animasyonlu ana ekran
- [x] 19 sağlayıcı kartı (~75 model), otomatik kota geçişi
- [x] Sessiz "…" ölü, akıllı auto dil, referans ThinkingPanel
- [x] Türkçe garantisi v2.1 (belge+cümle katmanı), balonsuz AI, TTS
- [x] Gerçek açık/koyu/sistem teması, selamlamalı ana ekran
- [x] TTS build fix, Dosyalar ekranı (gezgin + editör)
- [x] Metin-içi araç niyeti v2, run_command terminali (salt-okunur)

### v0.15 — Meta-Filtresi + Canlı Sayaç + Premium Ana Ekran + İzinler
- [x] Meta-filtresi: "komutunu çalıştırdım / aracı kullandım" tarzı
      öz-anlatım balondan panele taşınır (sonuç cümlesi korunur);
      "Then summarize." gibi artıklar yakalanır
- [x] Canlı ms sayaç: üretim boyunca 100ms tick (0'da takılma
      bitti), panelde "2.0 s" + "SSE · 2.0 sn ·" ondalıklı
- [x] Premium ana ekran: kayan yıldızlar + kayan-yıldız çizgileri
      (Canvas), nefes alan 34sp ReDHawK başlığı
- [x] Ajan izinleri ekranı (referans tasarıma sadık): profil
      çipleri, 6 izin satırı, chmod ızgarası + rwx özeti,
      İPTAL/KAYDET ile DataStore'a yazılır
- [x] GERÇEK denetim: her araç çağrısı öncesi izin kontrolü
      (kapalıysa model alternatife yönlendirilir); chmod 777 =
      onay diyalogları atlanır, AI otomatik devam eder
- [x] 3 yeni araç: delete_file, move_file (SAF kopyala-sil destekli),
      chmod_file (uygulama deposu) + write_file diff önizlemesi
- [x] Girişler: menü "Ajan izinleri" + hızlı işlemler kartı +
      ölü onOpenPermissions canlandırıldı

## Bilinen doğrular (ekran görüntülerinden)

- Küçük modeller 3 şekilde düşünme sızdırır, 3'ü de kapalı.
- Paneldeki düşünmenin İngilizce kalması NORMALDİR (referansta da öyle).
- Küçük modeller function-calling bilmez → metin niyeti şarttır.
- Küçük modeller "yapabilirim" deyip araç çalıştırmaz → prompt'ta yasak.
- Klasör görme terminal gerektirmez (SAF + dosya API'leri yeter).
- Pollinations anahtarsız öldü. Bedava: LLM7, OVH, Duck.ai, Dahl,
  NaraRouter, KiraAI-mini, Atria.

## Sıradaki

### v1.0 — Proje + Kurallar
- [ ] Proje kökü, redhawk.json, tam terminal yetkisi (onaylı)
- [ ] Menüye "Agent Terminali" + "Kurallar" (o zaman gerçek olur)

### v1.1 — Entegrasyonlar + Gelir
- [ ] Git/GitHub, MCP, SSH, ADB + AdMob/Pro
- [ ] Menüye "GitHub depoları" vb. (o zaman gerçek olur)

## Test listesi (AndroidIDE build sonrası)

1. "bu klasörde ne var" → balonda "komutunu çalıştırdım" YOK,
   sonuç cümlesi VAR; meta panelde
2. Akış sırasında panel sayacı 0.1, 0.2... diye canlı artıyor mu
3. Ana ekranda yıldızlar + nefes alan başlık var mı
4. Menü → Ajan izinleri → terminali KAPAT + KAYDET → ajan
   "izin vermedi" deyip alternatife yönleniyor mu
5. chmod 777 + KAYDET → write_file onaysız çalışıyor mu
6. delete_file/move_file onayı diff/uyarı içeriyor mu
