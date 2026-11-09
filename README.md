# Oto Tıklayıcı (Android)

Hafif ve kullanımı basit bir Android otomatik tıklayıcı. **Tekli** ve **Çoklu** mod destekler.

- Saf Java, hiçbir harici kütüphane yok (AndroidX bile yok) → çok küçük APK
- Tek izin: **Erişilebilirlik** (ayrıca "diğer uygulamaların üzerinde göster" izni gerekmez)
- İnternet izni yok, veri toplamaz, ekran içeriğini okumaz
- Android 7.0 (API 24) ve üzeri

## İndir

[Releases](../../releases/latest) sayfasından `OtoTiklayici.apk` dosyasını indirip kur.
(Bilinmeyen kaynaklardan yüklemeye izin vermen gerekebilir.)

## Kullanım

1. Uygulamayı aç, **İzni Aç**'a dokun ve listeden **Oto Tıklayıcı**'yı etkinleştir (sadece ilk sefer).
2. Modu seç:
   - **Tekli:** Tek bir noktaya sürekli tıklar.
   - **Çoklu:** Birden çok noktaya sırayla tıklar (en fazla 20 hedef).
3. Tıklama aralığını (ms) ve tekrar sayısını gir (`0` = durdurana kadar).
4. **Paneli Göster**'e bas. Ekranda yüzen bir panel ve hedef dairesi belirir.
5. Hedef dairelerini sürükleyip istediğin yere bırak.
6. Paneldeki **▶** ile başlat, **■** ile durdur.

| Panel butonu | Görevi |
|---|---|
| ▶ / ■ | Başlat / Durdur |
| + | Yeni hedef ekle (Çoklu mod) |
| − | Son hedefi sil (Çoklu mod) |
| ✕ | Paneli kapat |

Paneli herhangi bir butonundan basılı tutup sürükleyerek taşıyabilirsin. Hedef ve panel konumları
bir sonraki açılışta hatırlanır. Çoklu modda tekrar sayısı, tüm hedef listesinin kaç tur döneceğini belirler.

## Derleme

```bash
gradle assembleRelease   # Gradle 8.10+, JDK 17, Android SDK 35
```

Her `main` push'unda GitHub Actions APK derler; `v*` etiketi atıldığında otomatik release oluşturur.
