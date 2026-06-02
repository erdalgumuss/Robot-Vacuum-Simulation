# 🤖 Robot Süpürge Simülasyonu (Robot Vacuum Cleaning Simulation)

JavaFX ile geliştirilen, bir robot süpürgenin oda içinde gezinerek engellerden
kaçtığı, farklı kir tiplerini temizlediği, bataryasını yönettiği ve gerektiğinde
şarj istasyonuna en kısa yoldan döndüğü bir masaüstü simülasyonu.

> Bu proje, **BZ 214 Visual Programming** dersi kapsamında geliştirilmiştir.
> Derse ve katkıda bulunanlara teşekkürler. (This project was developed as part
> of the BZ 214 Visual Programming course. Special thanks to the course
> instructor and contributors.)

## Mimari

Katı **MVC** + yönetici (manager/controller) deseni:

```
model/       → Saf veri & durum (JavaFX bağımsız): Cell, Room, Robot, enum'lar, stats
controller/  → Mantık: simülasyon döngüsü, robot kontrolü, batarya, algoritmalar (Strategy), yol bulma (BFS/A*)
util/        → Sabitler ve yardımcılar (SimConstants)
view/        → JavaFX UI & çizim: Main, RoomCanvas, ControlPanel, StatusPanel, CustomTitleBar
resources/   → app.css (koyu tema), png/ (görseller), sound/ (ses efektleri)
```

Robotun konumu **sürekli (piksel tabanlı)** modellenir; oda ise **parametrik bir
grid** olarak temsil edilir (çoklu oda düzenini destekler). Yol bulma grid
üzerinde waypoint listesi üretir, robot bu noktalara akışkan şekilde yönelir.

## Gereksinimler

- JDK 21+ (JDK 24 ile test edildi)
- Maven 3.9+
- JavaFX 21 (Maven bağımlılığı olarak otomatik gelir, `classifier=win`)

## Çalıştırma

```powershell
mvn clean compile
mvn javafx:run
```

## Testler

Çekirdek iş mantığı (`model/` + `controller/`) JavaFX'e bağımlı olmadığından,
**üçüncü parti kütüphane kullanmadan** (JUnit yok — ders kuralı) saf Java
assertion tabanlı bir test koşucusu ile doğrulanır:

```powershell
mvn test-compile
java -cp "target/classes;target/test-classes" apptest.TestRunner
```

48 test; yol bulma (BFS/A*), erişilebilirlik, kir temizleme süreleri, batarya,
düşük-batarya dönüşü, sıfırlama, ulaşılamaz alan ve döngü-kaçış garantilerini
kapsar. Tümü geçerse çıkış kodu 0.

## Durum

- [x] **Faz 0 — Temel altyapı:** paket iskeleti, model katmanı, parametrik oda,
      grid çizimi, panel yerleşimi, koyu tema, özel başlık çubuğu.
- [ ] Faz 1 — Robot hareketi, çarpışma, temizleme, batarya
- [ ] Faz 2 — Temizlik algoritmaları (Rastgele/Spiral/Duvar Takip) + BFS/A* dönüş
- [ ] Faz 3 — Tam UI etkileşimi (araç ekleme, kontroller, canlı istatistik)
- [ ] Faz 4 — Bonuslar (ulaşılamaz alan, animasyon, çoklu oda, ses) + rapor
