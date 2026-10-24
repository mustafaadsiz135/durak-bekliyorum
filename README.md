# Durakta Bekliyorum

Otobüs durağında bekleyen yolcuların hangi otobüsü beklediğini şoföre bildiren mobil uygulama prototipi. Yolcu ve şoför için iki arayüz tek uygulamadadır.

- **Yolcu:** hat → yön → durak seçer (isteğe bağlı GPS ile en yakın durak), isteğe bağlı "65 yaş üstü" veya "Engelli" kategorisini işaretler, **Bekliyorum** der. Otobüs yaklaşınca telefon titrer ve sesli uyarır. "Otobüse bindim" ile bildirim silinir; aksi halde 30 dakikada kendiliğinden düşer.
- **Şoför:** hat ve yön seçer, sefere başlar. Ekranda sonraki durak, uzaklığı, bekleyen toplam yolcu ile 65 yaş üstü ve engelli sayıları görünür; ileriki 4 durak listelenir. Bekleyen olan durağa 500 m kala sesli uyarı verilir. Ekran sefer boyunca açık kalır.
- **Eşleştirme:** Şoför telefonunun konumu durak listesine göre "sonraki durağı" bulur; yalnızca o hattın ve o yönün bildirimleri gösterilir.

## APK nasıl alınır

1. Bu klasörün içeriğini yeni bir GitHub deposuna yükleyin (ör. `durak-bekliyorum`).
2. **Actions** sekmesinde "APK derle" iş akışı kendiliğinden çalışır (yaklaşık 5 dk).
3. Tamamlanan çalışmanın altındaki **DurakBekliyorum-apk** dosyasını indirip telefona kurun.

## Çalışma modları

| Mod | Ne işe yarar |
|---|---|
| **Demo** (varsayılan) | Veriler yalnızca o telefonda tutulur. Tek telefonda yolcu bildirimi gönderip geri tuşuyla şoför ekranına geçerek deneyebilirsiniz. Şoför ekranındaki "Deneme sürüşü" GPS olmadan otobüsü duraklar arasında ilerletir. |
| **Canlı** | Yolcu ve şoför ayrı telefonlarda; bildirimler merkezi sunucu (Firebase) üzerinden iletilir. |

## Canlı mod kurulumu (Firebase, ücretsiz)

1. https://console.firebase.google.com adresinde yeni proje açın (Google Analytics gerekmez).
2. **Build → Realtime Database → Create database**, konum: Belçika (europe-west1), **test modunda başlat**.
3. **Rules** sekmesine aşağıdaki kuralları yapıştırıp **Publish** deyin:

```json
{
  "rules": {
    "v1": {
      ".read": true,
      "bekleyen": { "$hat": { "$durak": { "$id": {
        ".write": true,
        ".validate": "newData.hasChildren(['k','t']) && newData.child('k').val().matches(/^[gye]$/) && newData.child('t').isNumber()"
      } } } },
      "arac": { "$hat": { "$yon": { "$id": { ".write": true } } } },
      "hatlar": { ".write": true },
      "test": { "$id": { ".write": true } }
    }
  }
}
```

4. Veritabanı sayfasının üstündeki adresi kopyalayın (`https://...firebasedatabase.app`).
5. Her telefonda uygulamada **⚙ Ayarlar → Canlı → adresi yapıştır → Kaydet → Bağlantıyı dene**.

Bu kurallar prototip denemesi içindir: herkes okuyup yazabilir. Yayın aşamasında Firebase Authentication ile yazma yetkisi sınırlanmalıdır.

## Kendi durakların

Uygulamadaki duraklar **örnek veridir**, gerçek hat güzergahı değildir. Gerçek denemede:

1. Her durağın yanında **Ayarlar → Konumumu göster**'e basıp enlem/boylamı not edin.
2. **Hatlar (JSON)** kutusunu düzenleyin. Durakları güzergah sırasıyla yazın; dönüş yönü otomatik oluşur:

```json
[{"id":"h5","no":"5","ad":"Okul – Merkez","renk":"#1f5fd6","duraklar":[
  {"id":"1","ad":"Okul Önü","lat":36.99123,"lng":35.31456},
  {"id":"2","ad":"Merkez","lat":36.99500,"lng":35.32000}
]}]
```

3. **Bu cihaza kaydet**, canlı modda ayrıca **Sunucuya yükle**. Diğer telefonlar **Sunucudan al** ile aynı veriyi alır (uygulama açılışta da otomatik alır).

## Değerlendirme (test kayıtları)

Şoför ekranı her bildirimi ilk gördüğü anı kaydeder. **Ayarlar → Test kayıtları** bölümünde gönderim saati, hat, durak, kategori ve gecikme (sn) listelenir; ortalama gecikme hesaplanır. **CSV olarak paylaş** ile WhatsApp/e-posta üzerinden Excel'e aktarılabilir. Doğru gecikme için telefon saatleri "otomatik" ayarda olmalıdır.

Önerilen deneme: 2 şoför telefonu, 5–10 yolcu telefonu, 3–4 durak. Ölçütler: bildirimin doğru hat/durakta görünme oranı, ortalama gecikme, yolcuların bildirim gönderme süresi ve kullanım kolaylığı anketi.

## Kişisel veriler

Ad, telefon numarası ve yolcu konumu sunucuya gönderilmez. Yolcu konumu yalnızca telefonda en yakın durağı bulmak için kullanılır. Sunucuya giden tek bilgi: hat, durak, kategori (genel / 65+ / engelli) ve zaman. Cihaz kimliği rastgele üretilir.

## Dosyalar

- `app/src/main/assets/index.html` — arayüzün tamamı (HTML/JS). Değişiklik buradan yapılır.
- `app/src/main/java/.../MainActivity.java` — Android kabuğu: WebView, konum izni, sesli uyarı (TextToSpeech), titreşim, ekranı açık tutma, paylaşma.
