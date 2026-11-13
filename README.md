# Oto Tıklayıcı (Android)

Hafif ve kullanımı basit bir Android otomatik tıklayıcı. **Tekli** ve **Çoklu** mod destekler.

- Saf Java, hiçbir harici kütüphane yok (AndroidX bile yok) → çok küçük APK
- Tek izin: **Erişilebilirlik** (ayrıca "diğer uygulamaların üzerinde göster" izni gerekmez)
- İnternet izni yok, veri toplamaz, ekran içeriğini okumaz
- Android 7.0 (API 24) ve üzeri
- **v1.1.0:** Koyu tema, hızlı ayar karo'su, rastgele aralık/hedef sapması, canlı tıklama sayacı

## İndir

[Releases](../../releases/latest) sayfasından `OtoTiklayici.apk` dosyasını indirip kur.
(Bilinmeyen kaynaklardan yüklemeye izin vermen gerekebilir.)

## Kullanım

1. Uygulamayı aç, **İzni Aç**'a dokun ve listeden **Oto Tıklayıcı**'yı etkinleştir (sadece ilk sefer).
2. Modu seç:
   - **Tekli:** Tek bir noktaya sürekli tıklar.
   - **Çoklu:** Birden çok noktaya sırayla tıklar (en fazla 20 hedef).
3. Tıklama aralığını (ms) gir (`0` = durdurana kadar). İstersen:
   - **Rastgele Aralık Sapması (±ms):** Her bekleme süresine rastgele sapma ekler; tıklamalar sabit bir desene benzemez.
   - **Hedef Sapması (±px):** Her tıklama, hedef dairesi içinde rastgele bir noktaya iner.
4. Tekrar sayısını gir (`0` = durdurana kadar).
5. **Paneli Göster**'e bas. Ekranda yüzen bir panel ve hedef dairesi belirir.
6. Hedef dairelerini sürükleyip istediğin yere bırak (bıraktığın anda kaydedilir).
7. Paneldeki **▶** ile başlat, **■** ile durdur. Sayı gösterge, canlı tıklama sayısını görüntüler.

İpucu: **Hızlı ayarlardaki "Oto Tıklayıcı" karosunu** ekleyerek panele hiç dokunmadan tıklamayı açıp kapatabilirsin.

| Panel butonu | Görevi |
|---|---|
| ▶ / ■ | Başlat / Durdur |
| + | Yeni hedef ekle (Çoklu mod) |
| − | Son hedefi sil (Çoklu mod) |
| ✕ | Paneli kapat |
| 0:123 | Canlı tıklama sayacı (her tıklamada güncellenir) |

## v1.1.0 Yenilikleri

- Hızlı ayarlar (Quick Settings) karo'su: paneldeki butonlara dokunmadan aç/kapa
- Rastgele aralık sapması (±ms) ve hedef sapması (±px) — sabit desen yerine insansı tıklamalar
- Yüzen panelde canlı tıklama sayacı
- Hedef daireleri artık bırakıldığı anda kaydedilir
- Koyu tema arayüzü

Paneli herhangi bir butonundan basılı tutup sürükleyerek taşıyabilirsin. Hedef ve panel konumları
bir sonraki açılışta hatırlanır. Çoklu modda tekrar sayısı, tüm hedef listesinin kaç tur döneceğini belirler.

## Derleme

```bash
gradle assembleRelease   # Gradle 8.10+, JDK 17, Android SDK 35
```

Her `main` push'unda GitHub Actions APK derler; `v*` etiketi atıldığında otomatik release oluşturur.
