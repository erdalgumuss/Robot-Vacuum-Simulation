# Robot Süpürge Simülasyonu

JavaFX ile geliştirilen bu masaüstü uygulaması, bir robot süpürgenin oda içinde
gezinerek engellerden kaçmasını, farklı kir tiplerini temizlemesini, bataryasını
yönetmesini ve gerektiğinde en kısa yoldan şarj istasyonuna dönmesini simüle eder.

Bu proje **BZ 214 Visual Programming** dersi kapsamında geliştirilmiştir. Special
thanks to the course instructor and contributors.

## Grup Bilgileri

**Grup:** 20

| Üye | Öğrenci No |
|-----|------------|
| Erdal Gümüş | 1030510069 |
| Melisa Şimşek | 1030510573 |
| Batuhan Pehlivanoğlu | 1030510592 |

## Özellikler

- Oda grid'i, robot, hareket izi, şarj istasyonu, kirler ve mobilya engelleri.
- Üç kir tipi: toz, sıvı ve leke. Her tip farklı sürede temizlenir ve farklı
  batarya maliyetine sahiptir.
- Başlat, duraklat, sıfırla, istasyona dön ve manuel batarya ayarı.
- Robot hızı, temizlik algoritması, oda düzeni, çalışma modu ve mobilya türü seçimi.
- Temizlik algoritmaları: rastgele, spiral, duvar takip ve akıllı hedefleme.
- Düşük bataryada veya kullanıcı komutunda A* ile şarj istasyonuna en kısa dönüş.
- Ulaşılamaz alan tespiti ve ulaşılamaz kir göstergesi.
- Çoklu oda düzeni: oturma odası, yatak odası ve stüdyo.
- Gerçekçi mod: robot tüm odayı bilmeden sensör ışınlarıyla keşif yapar.
- Premium PNG görsel katman: robot, kir, mobilya, zemin, duvar, halı ve şarj
  istasyonu asset'leri.
- Canlı telemetri: konum, yön, batarya, temizlenen alan, kalan kir, geçen süre,
  motor/algı durumu ve kapsama bilgileri.

## Mimari

Proje katı **MVC** yaklaşımıyla yapılandırılmıştır. `model` ve `controller`
paketleri JavaFX'e bağımlı değildir; bu sayede iş mantığı arayüzden bağımsız
test edilebilir.

```text
model/       Saf veri ve durum: Room, Cell, Robot, enum'lar, stats, sensör modelleri
controller/  Simülasyon mantığı: manager, robot kontrolü, pathfinding, sürüş, algı
view/        JavaFX arayüz: Main, RoomCanvas, ControlPanel, StatusPanel, TelemetryPanel
util/        Genel sabitler
resources/   CSS ve PNG asset'ler
docs/        Gereksinim eşlemesi, asset kılavuzu, proje raporu ve diyagramlar
```

Robotun konumu sürekli piksel koordinatlarıyla tutulur; oda ise parametrik grid
olarak modellenir. Tanrı modunda robot oda bilgisini stratejilerle kullanır.
Gerçekçi modda robot sensörleriyle iç harita oluşturur.

## Gereksinimler

- JDK 21 veya üzeri
- Maven 3.9 veya üzeri
- JavaFX 21 Maven bağımlılığı olarak gelir

> Not: `pom.xml` JavaFX için Windows classifier kullanır. Proje JDK 24 ile test
> edilmiştir. Eğer `java` komutu eski bir JDK'ye gidiyorsa testleri JDK 21+
> binary'siyle çalıştırın.

## Çalıştırma

```powershell
mvn clean compile
mvn javafx:run
```

## Testler

Testler üçüncü parti test kütüphanesi kullanmadan, saf Java assertion mantığıyla
`src/test/java/apptest/TestRunner.java` içinde çalıştırılır.

```powershell
mvn test-compile
java -cp "target/classes;target/test-classes" apptest.TestRunner
```

Son doğrulama sonucu:

```text
72 geçti, 0 kaldı
```

Test kapsamı; BFS/A*, erişilebilirlik, kir temizleme süreleri, batarya yönetimi,
düşük batarya dönüşü, sıfırlama, ulaşılamaz alan, algoritma regresyonları,
sensör ışınları, inanç haritası, gerçekçi mod, motor modeli ve mod geçişlerini
içerir. Kirli hücreye mobilya yerleştirme regresyonu da ayrıca korunur.

## Ödev Gereksinimleriyle Uyum

Ödev PDF'indeki zorunlu maddeler karşılanmıştır:

- JavaFX GUI
- MVC mimarisi
- OOP ve Java Collections kullanımı
- Grid, robot, mobilya/engel, kir, şarj istasyonu ve hareket izi
- Kir ekleme, engel ekleme, kir tipi, hız ve algoritma seçimi
- Başlat/duraklat/sıfırla/istasyona dön/manuel batarya
- Üç kir tipi ve farklı temizlik süreleri
- Batarya tüketimi ve düşük batarya dönüşü
- BFS/A* ile en kısa rota
- Gerçek zamanlı durum ve istatistik göstergeleri

Bonus olarak ulaşılamaz alan tespiti, çoklu oda düzeni, animasyon/heatmap ve
premium görsel asset katmanı eklenmiştir. Ses altyapısı için `javafx-media`
hazırdır; ses efektleri opsiyonel cila olarak bırakılmıştır.

Detaylı madde eşlemesi için:

- `docs/REQUIREMENTS.md`
- `docs/report/REPORT.md`

## Dokümantasyon

| Dosya | Açıklama |
|------|----------|
| `docs/REQUIREMENTS.md` | Ödev maddeleri ile kod karşılıklarının eşlemesi |
| `docs/ASSETS.md` | Üretilen asset envanteri ve üretim kılavuzu |
| `docs/report/REPORT.md` | Detaylı proje raporu |
| `docs/report/class-diagram.puml` | PlantUML sınıf diyagramı |
| `docs/report/usecase-diagram.puml` | PlantUML use-case diyagramı |

## Assetler

Proje `src/main/resources/png/` altında 19 PNG asset içerir:

- Robot ve temizlik robotu
- Toz, sıvı ve leke sprite'ları
- Şarj istasyonu
- Ahşap zemin, koyu duvar ve dekoratif halı
- Footprint'li mobilyalar: kanepe, sehpa, yemek masası, yatak, koltuk, bitkiler,
  kitaplık ve TV konsolu

PNG dosyaları eksik olsa bile uygulama Canvas fallback çizimlerine düşecek şekilde
tasarlanmıştır.

## Teslim Notları

Ödev PDF'ine göre ZIP dosyası grup numarasıyla adlandırılmalıdır. Bu proje için
önerilen ad:

```text
group20.zip
```

ZIP içinde kaynak kodlar, JavaFX proje dosyaları, asset dosyaları, README, proje
raporu, diyagramlar ve ekran görüntüleri yer almalıdır.
