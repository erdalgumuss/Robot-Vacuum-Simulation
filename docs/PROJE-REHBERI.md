# Proje Rehberi — Uçtan Uca, Sade Dil

> Bu belge, projeyi **ekipteki herkesin** (kod yazan da, az bilen de) baştan sona
> anlayabilmesi için yazıldı. Amaç: dosyaları ezberlemek değil, "bu uygulama
> neden böyle çalışıyor" sorusunu rahatça cevaplayabilmek. Jargon geçtikçe yanında
> sade açıklaması var.

---

## 1. Bu uygulama nedir? (tek paragraf)

Bir **robot süpürge simülasyonudur**. Ekranda bir oda var (kareli bir zemin),
odanın içinde gezen bir robot, yerlerde kirler, mobilyalar ve bir şarj istasyonu.
Robot otonom olarak gezip kirleri temizliyor, mobilyalara çarpmıyor, hareket
ettikçe bataryası azalıyor, bitince şarja dönüyor. Kullanıcı (biz) kir/mobilya
ekleyebiliyor, hız ve temizlik yöntemini seçebiliyor, başlat/durdur/sıfırla
yapabiliyor. Java diliyle, **JavaFX** (Java'nın pencere/arayüz kütüphanesi) ile
yazıldı.

---

## 2. En önemli fikir: MVC (her şeyin temeli)

Eğer bu projeden tek bir şey öğreneceksen, o **MVC** olsun. MVC, kodu üç ayrı
göreve bölmek demek. Bir **tiyatro** gibi düşün:

- **Model = sahnedeki gerçeklik.** Oda, robot, kirler... "dünya neyse o". Sadece
  veri tutar; izlendiğini bile bilmez. (Klasör: `model/`)
- **Controller = yönetmen + robotun beyni.** "Bu an ne olacak?" kararını verir;
  robotu hareket ettirir, kiri temizletir, bataryayı azaltır. (Klasör: `controller/`)
- **View = seyirciye bakan sahne + kumanda.** Olan biteni ekrana çizer ve senin
  tıklarını/kaydırmalarını alır. (Klasör: `view/`)

**Altın kural:** Bilgi *aşağı* akar, komut *içeri* akar. Yani:
- Her karede **view, modeli okuyup çizer** (model → view).
- Sen bir düğmeye basınca **view, controller'a haber verir, o da modeli değiştirir**
  (view → controller → model).
- **Model asla yukarıyı çağırmaz.** Oda, ekranı veya beyni tanımaz.

Bunu nereden anlıyoruz? `model/` ve `controller/` klasörlerinde **tek bir JavaFX
satırı bile yok.** Yani iş mantığı, arayüz olmadan da çalışır ve test edilebilir.
Nitekim 74 otomatik testimiz hiç pencere açmadan tüm mantığı sınıyor. Bu, "katı
MVC" iddiamızın kanıtı.

---

## 3. Klasör haritası (hangi kutu ne işe yarıyor)

```
src/main/java/
├── model/        → SAF veri. "Dünya neyse o." (oda, robot, kir, enum'lar)
├── controller/   → MANTIK. "Dünya nasıl değişir?"
│   ├── algorithm/   → temizlik gezinme yöntemleri (4 farklı algoritma)
│   ├── drive/       → robotu süren "beyin" (iki mod: Tanrı / Gerçekçi)
│   └── perception/  → gerçekçi modun gözü: sensör + harita öğrenme
├── util/         → SimConstants: tüm ayar sayıları tek yerde (hız, batarya vb.)
└── view/         → JavaFX arayüz: çizim + kontrol panelleri (gördüğümüz her şey)

src/main/resources/png/   → robot, kir, mobilya, zemin görselleri (sprite)
src/test/java/apptest/    → 74 testlik saf-Java test koşucusu
docs/                     → rapor, diyagramlar, bu rehber
target/                   → derleme çıktısı (Maven üretir, elle dokunulmaz)
```

---

## 4. Çalışmanın kalbi: "bir karenin hayatı"

Uygulama saniyede ~60 kez kendini yeniler. Her yenilenmeye **kare (frame)** denir.
Başlangıç noktası `view/Main.java` içindeki `AnimationTimer` (JavaFX'in "her karede
beni çağır" zamanlayıcısı). Her karede sırayla şunlar olur:

1. **`sim.update(now)`** → `SimulationManager` geçen süreyi hesaplar (`dt` =
   bir önceki kareden bu yana geçen saniye).
2. **`controller.step(dt)`** → robotun beyni bir **tık** ilerler: yön seç, hareket
   et, altındaki kiri temizle, bataryayı azalt. *Burada model değişir.*
3. **`canvas.render(...)` + paneller** → view, artık güncellenmiş modeli okuyup
   ekranı çizer.

Yani döngü hep aynı: **karar ver → modeli güncelle → çiz.** Tekrar tekrar.

Komut ise ters yönde gider. Sen "Başlat"a bastığında:
`ControlPanel` → `SimulationManager.start()` → robotun durumu "CLEANING" olur →
bir sonraki karede beyin onu hareket ettirmeye başlar. **View hiçbir zaman
doğrudan odaya/robota elini sokmaz; her komut `SimulationManager`'dan geçer.**
Bu yüzden `SimulationManager` "tek kapı"dır — view'in tanıması gereken tek
controller sınıfı odur.

---

## 5. Robot tam olarak nerede? (grid + piksel hibrit)

Bu, projenin en zekice kısmı; sorulursa puan getirir.

- **Oda bir ızgaradır (grid):** satır × sütun kareler. Her kareye **hücre (Cell)**
  denir. Bir hücre ya zemin, ya duvar, ya mobilya, ya da istasyondur; zeminse
  üstünde kir olabilir. Mantık (kir nerede, duvar nerede, yol var mı) hep bu
  ızgara üzerinden döner.
- **Robotun konumu ise süreklidir (piksel):** robot "3. satır 5. sütun" diye değil,
  gerçek bir `(x, y)` koordinatı ve bir bakış açısı (`heading`) ile durur. Bu yüzden
  kareden kareye **akıcı** hareket eder, zıplamaz.

İkisini nasıl birleştiriyoruz? Robot kararlarını hücre düzeyinde alır ("şu komşu
hücreye gideyim"), ama oraya piksel piksel, çarpışma kontrolüyle ilerler. Böylece
robot **asla duvarın içine giremez** ama hareketi de doğal görünür. Bu sayede
"çarpınca yön değiştir" şartını, gerçekten çarpışmadan, sağlam biçimde karşılarız.

Hangi sınıflar?
- `Cell` — tek bir kare (tip + kir + temizlik ilerlemesi + ziyaret edildi mi).
- `Room` — hücrelerden oluşan ızgaranın sahibi (oda). Engel/kir/istasyon işlemleri burada.
- `Robot` — sürekli konum, yön, batarya, durum ve hareket izi. **Sadece durumu tutar;
  nasıl hareket edeceğini bilmez** (onu controller bilir).

---

## 6. Kurallar: kir, batarya, temizleme

Üç kir tipi var ve hepsi farklı davranır (`model/DirtType`):

| Kir | Temizleme süresi | Ek batarya maliyeti |
|-----|------------------|---------------------|
| Toz | 0.8 sn (hızlı) | %1 |
| Sıvı | 2.5 sn | %3 |
| Leke | 4.0 sn (en zor) | %5 |

- Robot bir kirin üstüne gelince **orada bekler** ve kiri yavaşça temizler; kir
  görsel olarak solar (animasyon).
- **Hareket ederken** batarya sürekli azalır (saniyede ~%0.6).
- **Kir temizlerken** kir tipine göre **ek** batarya harcar (yukarıdaki tablo).
- Batarya **%20'nin altına** düşünce robot işi bırakıp en kısa yoldan şarja döner,
  dolunca kaldığı yerden devam eder.

Bütün bu sayılar tek bir yerde, `util/SimConstants` içinde. Dengeyi değiştirmek
istersen (örn. robot daha hızlı boşalsın) sadece orayı düzenlersin — koda dağılmaz.

---

## 7. Robot nasıl karar veriyor? İki mod + "Driver"

Robotun beyni `controller/drive/` klasöründe. İki farklı "beyin" var ve ortak bir
sözleşmeye (`Driver` arayüzü) uyarlar:

- **Tanrı Modu (`OmniscientDriver`):** Robot **tüm odayı bilir.** Seçilen algoritmayla
  verimli gezer, şarja dönerken A* ile en kısa yolu hesaplar. (Varsayılan mod.)
- **Gerçekçi Mod (`ReactiveDriver`):** Robot odayı **bilmez.** Sadece sensörleriyle
  etrafını tarar, gördükçe kendi iç haritasını ("inanç haritası") çizer, gerçek bir
  engele çarpınca seker ve yön değiştirir. Gerçek bir robot vakuma çok daha yakın.

Bu iki beyni kim yönetiyor? `RobotController`. O bir **façade**'dir (sade bir
"ön yüz"): her ikisini de içinde tutar ve hangi mod seçiliyse ona iş yaptırır.
Dışarıdan bakınca tek bir `controller.step()` çağrısı vardır; modun değişmesi
çağıran tarafı hiç etkilemez. Bu, "kodun geri kalanını değiştirmeden davranışı
değiştirme" demektir — temiz tasarımın güzel bir örneği.

> Yan not (gerçekçilik): Gerçekçi modda bile robot, **kendi konumunu** varsayılan
> olarak tam bilir. Onu da gerçek hayattaki gibi tahmine çevirmek için "Gerçek
> Odometri (deneysel)" seçeneği var — açıkken robot konumunu tekerlek dönüşünden
> tahmin eder, hata birikir (drift), duvarları gördükçe ve şarja oturunca düzeltir.

---

## 8. Temizlik algoritmaları (Strategy deseni)

`controller/algorithm/` içinde 4 farklı gezinme yöntemi var. Hepsi aynı arayüze
(`CleaningStrategy`) uyar; sadece "bir sonraki adımda hangi yöne gideyim?" sorusuna
farklı cevap verirler:

- **Rastgele:** Ziyaret etmediği komşulara meyleder; takılmayı azaltır.
- **Spiral:** Tutarlı bir dönüş önceliğiyle içe doğru sarmal çizer.
- **Duvar Takip:** Sol-el kuralıyla çeperleri ve köşeleri sistematik tarar.
- **Akıllı:** Her adımda **en yakın kire giden en kısa yolu** bulup oraya yönelir.

Bunun adı **Strategy (Strateji) deseni**: aynı işin (gezinme) birden çok yolu var;
her yolu ayrı bir sınıfa koyarız ve çalışırken birini "takarız". Yeni bir algoritma
eklemek, sadece yeni bir sınıf yazıp listeye eklemektir — mevcut kod değişmez.

Hangi enum'a hangi sınıf? Bunu `StrategyFactory` (bir **Factory/fabrika** deseni)
karar verir: "RANDOM seçildi → `RandomStrategy` üret" gibi. Tek karar noktası.

---

## 9. En kısa yol: BFS ve A* (sade) + ulaşılamaz alan

İki klasik algoritma `controller/PathFinder` içinde:

- **BFS (genişlik-öncelikli arama):** Başlangıçtan **dalga dalga** dışa yayılır;
  ağırlıksız ızgarada en kısa yolu garanti eder. Biz onu "en yakın kiri bul" ve
  "istasyondan nereye ulaşılabilir" gibi **hedefsiz/çok-hedefli** taramalarda
  kullanırız.
- **A\* (A-star):** BFS'in akıllı hâli. Hedefe doğru bir "tahmin" (Manhattan
  mesafesi) kullanarak aramayı hedefe yöneltir; daha az hücre açar. **Tek hedef**
  olan "şarja dön" işinde kullanırız.

**Ulaşılamaz alan tespiti (bonus):** İstasyondan BFS ile "nereye gidebilirim"
haritası çıkarılır. Mobilyalarla tamamen çevrelenmiş, erişilemeyen kirler varsa,
ekranda **kırmızı çarpı + sayaç** ile gösterilir. Robot bu kirleri bekleyip sonsuza
kadar takılmaz; ulaşabildiklerini bitirip durur.

---

## 10. Ekrandaki paneller (view ne gösteriyor)

- **Sol panel (`ControlPanel`):** Araçlar (kir/mobilya ekle), oda düzeni, mod,
  algoritma, hız, robot durumu ve kontrol düğmeleri. Bölümler katlanabilir.
- **Orta (`RoomCanvas`):** Odanın kendisi — zemin, duvar, mobilya, kir, robot,
  hareket izi. Her karede yeniden çizilir.
- **Alt şerit (`StatusPanel`):** Toplam/temizlenen/kalan alan, kalan kir,
  ulaşılamaz, geçen süre, batarya.
- **Sağ panel (`TelemetryPanel`):** Robotun "kendi gözü" — öğrendiği iç harita,
  sensör radarı ve motor verileri (voltaj, zorlanma, devir). Çoğu yalnız Gerçekçi
  modda anlamlı.

Çizim için isteğe bağlı **PNG sprite**'lar (hazır görseller) kullanılır; bir görsel
eksik olsa bile uygulama çökmez, basit şekil çizimine düşer.

---

## 11. Kullandığımız tasarım desenleri (viva için isimleriyle)

Bunları adıyla söyleyebilmek iyi puan getirir:

- **MVC:** Model / View / Controller ayrımı (projenin omurgası).
- **Strategy:** 4 temizlik algoritması, takılıp çıkarılabilen yöntemler.
- **Factory:** `StrategyFactory` (enum → strateji üretir).
- **Façade:** `RobotController` (iki sürücüyü tek sade yüz ardında gizler),
  ayrıca `SimulationManager` (view için tek kapı).
- **Enum'larda davranış:** `DirtType`, `Direction` gibi enum'lar sadece isim değil,
  veri ve davranış da taşır (örn. `DirtType.cleaningSeconds()`).

Java Collections kullanımı da şart gereği var: `Queue`/`ArrayDeque` (BFS ve dönüş
yolu), `PriorityQueue` (A*), `Map`/`HashMap`/`EnumMap`, `List`.

---

## 12. Nasıl çalıştırılır / test edilir

```powershell
# Çalıştırma
mvn clean compile
mvn javafx:run

# Testler (üçüncü parti yok, saf Java)
mvn test-compile
java -cp "target/classes;target/test-classes" apptest.TestRunner
```

> Not: Bilgisayarda `java` komutu eski bir sürüme gidiyorsa, derlemeyi JDK 21+
> ile yapmak gerekir (projede JAVA_HOME JDK 24'e ayarlı).

---

## 13. Mini sözlük

- **MVC:** Kodu Model (veri) / View (arayüz) / Controller (mantık) diye üçe bölme.
- **Grid / hücre:** Odanın kareli ızgarası; her kare bir "hücre".
- **heading:** Robotun baktığı açı (radyan).
- **dt:** İki kare arasında geçen süre (saniye); hareketi zamana bağlar.
- **sprite:** Hazır çizilmiş PNG görsel (robot, mobilya vb.).
- **Strategy / Factory / Façade:** Yaygın tasarım kalıpları (bkz. bölüm 11).
- **BFS / A\*:** En kısa yol bulan iki algoritma.
- **odometri / drift:** Robotun konumunu tekerlekten tahmin etmesi / o tahminin
  zamanla gerçekten sapması.
- **belief map (inanç haritası):** Robotun sensörle gördükçe çizdiği kendi haritası.

---

## 14. Ekip için öneri: kim neyi anlatsın? (3 kişi)

Sunumda/viva'da herkesin bir alanı sahiplenmesi iyi olur. Önerilen bölüşüm
(serbestçe değiştirin):

- **Kişi 1 — Dünya modeli (`model/`):** MVC mantığı, grid+piksel hibrit,
  `Cell`/`Room`/`Robot`, kir ve batarya kuralları. (Bölümler 2, 5, 6)
- **Kişi 2 — Beyin (`controller/`):** kare döngüsü, iki mod ve `Driver` façade,
  Strategy ile 4 algoritma, Factory. (Bölümler 4, 7, 8, 11)
- **Kişi 3 — Yol bulma + Arayüz:** BFS/A* ve ulaşılamaz alan, JavaFX panelleri ve
  çizim, testler. (Bölümler 9, 10, 12)

Herkes en azından **bölüm 2 (MVC)** ve **bölüm 4 (kare döngüsü)**'ni bilsin — bunlar
projenin ortak dili.
