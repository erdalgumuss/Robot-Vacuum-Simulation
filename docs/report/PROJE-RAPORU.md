# BZ 214 Visual Programming — Proje Raporu

## Robot Süpürge Temizleme Simülasyonu

**Grup Numarası: 30**

| Numara | Ad Soyad | Katkı |
|--------|----------|-------|
| 1030510069 | Erdal Gümüş | Proje mimarisi (MVC) ve çekirdek geliştirme: model katmanı, controller (sürücüler, gerçekçi mod, algoritmalar, yol bulma), ana arayüz yapısı ve testler. |
| 1030510573 | Melisa Şimşek | Dokümantasyon ve diyagramlar (proje raporu, kullanım ve sınıf diyagramları); hazır oda düzenleri. |
| 1030510592 | Batuhan Pehlivanoğlu | Görsel katman (PNG sprite'lar, CSS tema) ve ses efektleri; durum/telemetri panelleri. |

> Katkı dağılımı taslaktır; üyeler kendi kutularını gözden geçirip güncelleyebilir.

---

## Özet (Abstract)

Bu proje, bir robot süpürgenin bir oda içinde otonom olarak gezinerek farklı kir
tiplerini temizlediği bir masaüstü simülasyonudur. Uygulama **Java SE + JavaFX**
ile, **Model-View-Controller (MVC)** mimarisi ve nesne yönelimli tasarım
ilkeleriyle, **üçüncü parti kütüphane kullanılmadan** geliştirilmiştir. Robot,
duvar ve mobilya engellerinden kaçar; toz, sıvı ve leke kirlerini farklı süre ve
batarya maliyetiyle temizler; hareket ettikçe bataryasını tüketir ve düşük
bataryada en kısa yoldan (BFS/A\*) şarj istasyonuna döner. Kullanıcı; kir/engel
ekleyebilir, kir türü, hız, temizlik algoritması ve oda düzeni seçebilir,
simülasyonu başlat/duraklat/sıfırla yapabilir ve bataryayı elle ayarlayabilir.
Zorunlu gereksinimlerin tamamına ek olarak ulaşılamaz alan tespiti, çoklu oda
düzeni, temizlik animasyonları, ses efektleri ve robotun odayı bilmeden yalnızca
sensörleriyle keşfettiği bir "gerçekçi mod" bonus olarak eklenmiştir.

---

## Yazılım Tasarımı (Software Design)

### Proje tanımı

Sistem, satır × sütun bir **ızgara (grid)** olarak modellenen bir odada gezen bir
robot süpürgeyi simüle eder. Robotun konumu sürekli (piksel) tutulur; kararları
hücre düzeyinde alınır ve yalnızca yürünebilir komşulara doğru ilerler. Böylece
robot duvar/mobilyaya hiç değmeden akıcı şekilde hareket eder ve "çarpınca yön
değiştir" davranışı çarpışmasız, sağlam biçimde gerçeklenir.

### Kullanılan araçlar

- **Dil/platform:** Java SE (JDK 21+, JDK 24 ile test edildi)
- **Arayüz:** JavaFX 21 (Canvas tabanlı çizim + CSS tema)
- **Yapı:** Apache Maven (`pom.xml`); JavaFX bağımlılık olarak gelir
- **Üçüncü parti kütüphane yok** — testler dahil her şey saf Java SE

### Problem çözme yöntemleri (algoritmalar)

- **En kısa yol:** Şarja dönüşte **A\*** (tek hedef, Manhattan sezgiseli); en
  yakın kir ve erişilebilirlik taramalarında **BFS** (çok-hedefli/hedefsiz).
- **Gezinme (Tanrı modu):** **Strategy deseni** ile dört algoritma — Rastgele,
  Spiral, Duvar Takip, Akıllı (her adımda en yakın kire BFS).
- **Ulaşılamaz alan tespiti:** İstasyondan **flood-fill (BFS)** ile erişilemeyen
  kirler işaretlenir; robot bunları beklemeden işini bitirir.
- **Gerçekçi mod:** Robot odayı bilmez. **Işın-atma (ray-casting)** sensörleriyle
  etrafını tarar, gördüğünü bir **inanç haritasına** işler, **mekik (boustrophedon)**
  taramayla kapsar, haritada olmayan engele çarpınca **çarp-dön** (bump-and-turn)
  yapar; isteğe bağlı **odometri + scan-match** ile konumunu da tahmin eder.
- **Hibrit dünya modeli:** Mantık ızgara üzerinde, hareket sürekli koordinatta —
  sürekli çarpışma testiyle (daire-dikdörtgen kesişimi) birleştirilir.

### Kullanım senaryoları

1. Kullanıcı oda düzenini seçer, kir/mobilya ekler, hız ve algoritmayı belirler.
2. "Başlat" ile robot otonom temizliğe başlar; izini, batarya ve kapsama
   bilgisini gerçek zamanlı izler.
3. Batarya düşünce veya "İstasyona Dön" ile robot en kısa yoldan şarja döner,
   dolunca kaldığı yerden devam eder.
4. "Gerçekçi Mod"da robotun sis altında, yalnızca sensörleriyle keşfedişi izlenir.

### Arayüz özellikleri ve kullanıcı–sistem bilgi akışı

- **Sol panel (kullanıcıdan sisteme):** kir/mobilya ekleme araçları, kir türü,
  oda düzeni, mod, algoritma, hız ve manuel batarya ayarı, kontrol düğmeleri.
  Bölümler katlanabilir (TitledPane) olduğundan panel tek ekrana sığar.
- **Orta sahne:** oda, robot, hareket izi, kirler, mobilyalar ve şarj istasyonu.
- **Alt şerit (sistemden kullanıcıya):** toplam/temizlenen/kalan alan, kalan kir,
  ulaşılamaz sayısı, geçen süre, batarya — gerçek zamanlı.
- **Sağ telemetri paneli:** robotun öğrendiği iç harita, sensör radarı ve motor
  verileri (voltaj/zorlanma/devir) — gerçekçi modda anlamlı.

### Sınıf yapısı (UML) ve sınıfların işlevleri

Ayrıntılı UML için `class-diagram.puml` (PlantUML). Katmanlar:

- **model/** (saf veri, JavaFX'siz): `Cell` (tek hücre), `Room` (ızgara + engel/
  kir/istasyon), `Robot` (konum/yön/batarya/durum), `SimulationStats`,
  `FurniturePiece`, davranış taşıyan enum'lar (`CellType`, `DirtType`, `Direction`,
  `RobotState`…) ve gerçekçi-mod modeli (`KnownMap`, `SensorReading`, `RobotMemory`,
  `RobotTelemetry`).
- **controller/** (mantık, JavaFX'siz): `SimulationManager` (orkestratör, view'in
  tek kapısı), `RobotController` (iki sürücü için façade), `PathFinder` (BFS/A\*/
  erişilebilirlik), `LayoutFactory`; `drive/` (`Driver`, `OmniscientDriver`,
  `ReactiveDriver`, `Physics`, `MotorModel`); `perception/` (`PerceptionService`,
  `BeliefUpdater`); `algorithm/` (Strategy ailesi + `StrategyFactory`).
- **view/** (JavaFX): `Main`, `RoomCanvas` (çizim), `ControlPanel`, `StatusPanel`,
  `TelemetryPanel`, `SoundManager`, `SpriteAssets`.

Kullanılan **tasarım desenleri:** MVC, Strategy (algoritmalar), Factory
(`StrategyFactory`, `LayoutFactory`), Façade (`RobotController`,
`SimulationManager`) ve davranış taşıyan enum'lar. Java Collections çerçevesi
(`List`, `Map`, `Queue`/`ArrayDeque`, `PriorityQueue`) aktif olarak kullanılır.
`model` ve `controller` katmanlarında **hiç JavaFX bağımlılığı yoktur**; bu sayede
iş mantığı 74 saf-Java testiyle arayüz olmadan doğrulanır.

---

## Sonuç (Conclusion)

**Operasyonel senaryolar.** Uygulama; farklı oda düzenlerinde, dört temizlik
algoritmasıyla ve iki modda (Tanrı/Gerçekçi) sorunsuz çalışır. Robot tüm
erişilebilir kiri toplar, düşük bataryada şarja döner ve işi bitince durur;
ulaşılamaz kirlerde sonsuza dek takılmaz. 74 otomatik test (BFS/A\*, batarya,
strateji, sensör, inanç haritası, mod geçişleri) tüm bu davranışları korur.

**Eksikler ve kısıtlamalar.** Gerçekçi modda robot, varsayılan olarak kendi
konumunu tam bilir; konumu da tahmine çeviren "Gerçek Odometri" deneysel olarak
sunulmuştur. Bu modda pozisyon scan-match ile sürekli düzeltilir, ancak çıkarılan
harita geriye dönük düzeltilmez (loop closure yoktur); drift yaklaşık yarım hücreyle
sınırlandığından bunun etkisi küçüktür. `ReactiveDriver` sınıfı görece büyüktür ve ileride alt sorumluluklara
(planlama/lokalizasyon) bölünebilir; iki sürücü arasında bir miktar yardımcı-metot
tekrarı vardır. Bunlar doğruluğu etkilemeyen, ileride iyileştirilebilecek
kalemlerdir. Ses efektleri opsiyonel olarak bırakılmıştır.

**Amaca uygunluk.** Proje, ödevin tüm zorunlu fonksiyonel ve teknik
gereksinimlerini karşılar ve birçok bonus özelliği (ulaşılamaz alan, çoklu düzen,
animasyon, ses, gerçekçi sensör modu) ekler. Katı MVC ve temiz OOP sayesinde kod
okunur, test edilebilir ve genişletilebilir; amacına etkin biçimde hizmet eder.

**Kişisel görüşler ve geliştirme süreci.** Projeyi katmanlara ayırmak (MVC) ve
davranışı enum/strateji gibi küçük, sorumluluğu net birimlere dağıtmak,
geliştirmeyi ve hata ayıklamayı belirgin biçimde kolaylaştırdı. En öğretici kısım,
robotun "her şeyi bilen" modundan, yalnızca sensörleriyle dünyayı keşfeden
gerçekçi moda geçişti; bu, gerçek robotik problemlerini (algı, harita çıkarımı,
konum belirsizliği) küçük ölçekte deneyimletti.

> *Not: Bu rapor üç sayfayı aşmayacak biçimde özettir; ayrıntılı teknik anlatım
> için `REPORT.md`, gereksinim eşlemesi için `REQUIREMENTS.md`, sade dil rehber
> için `PROJE-REHBERI.md` belgelerine bakılabilir.*
