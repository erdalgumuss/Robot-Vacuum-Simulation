# ✅ Ödev Uyum Denetimi (Requirement Compliance) — Uçtan Uca

BZ 214 Visual Programming — Robot Vacuum Cleaning Simulation
Bu döküman, ödev PDF'indeki **her maddeyi** kodumuzdaki karşılığına eşler.
Durum: ✅ tam · 🟡 kısmî/planlı · ⏳ yapılacak

**Grup:** 30
**Üyeler:** Erdal Gümüş (1030510069), Melisa Şimşek, Batuhan Pehlivanoğlu

---

## 2. Fonksiyonel Gereksinimler

| # | Gereksinim | Durum | Nerede |
|---|-----------|:-----:|--------|
| 2.1 | Arayüz: oda grid'i, robot, engeller (mobilya/masa), kir, şarj istasyonu, hareket izi | ✅ | `view/RoomCanvas` (grid çizgileri, sprite/fallback, iz `drawTrail`) |
| 2.2a | Kir ekleme | ✅ | `ControlPanel` "Kir Ekle" + canvas tık → `SimulationManager.addDirt` |
| 2.2b | Kir türü seçimi | ✅ | `ControlPanel` Toz/Sıvı/Leke chip'leri → `selectedDirt` |
| 2.2c | Engel ekleme | ✅ | `ControlPanel` "Mobilya Ekle" → `SimulationManager.toggleFurniture` |
| 2.2d | Robot hızını değiştirme | ✅ | `ControlPanel` hız slider'ı → `setSpeedMultiplier` |
| 2.2e | Temizlik algoritması seçme | ✅ | `ControlPanel` radyolar → `setAlgorithm` (Rastgele/Spiral/Duvar Takip/Akıllı) |
| 2.3a | Başlat | ✅ | `SimulationManager.start` |
| 2.3b | Duraklat | ✅ | `SimulationManager.pause` |
| 2.3c | Sıfırla | ✅ | `SimulationManager.reset` (kir blueprint geri yükler) |
| 2.3d | İstasyona gönder | ✅ | `SimulationManager.returnToStation` → A* |
| 2.3e | **Bataryayı elle güncelle** | ✅ | `ControlPanel` batarya slider'ı → `setBattery` |
| 2.4 | En az 3 kir tipi: toz, sıvı, leke | ✅ | `model/DirtType` (DUST, LIQUID, STAIN) |
| 2.5a | Duvar/mobilyadan geçmez; çarpınca yön değiştirir | ✅ | `RobotController` yalnız yürünebilir komşuya gider; strateji yön değiştirir |
| 2.5b | Üstünden geçince kirli hücreyi temizler | ✅ | `RobotController.cleanHere` |
| 2.5c | Hareket ederken batarya azalır | ✅ | `BATTERY_MOVE_COST_PER_SEC` (`moveTowardTarget`) |
| 2.5d | Kir tipine göre **ek** batarya tüketir | ✅ | `DirtType.batteryCost` (toz 1 / sıvı 3 / leke 5) |
| 2.5e | Batarya düşünce istasyona döner | ✅ | `LOW_BATTERY_THRESHOLD` → `beginReturn` |
| 2.6 | Gerçek zamanlı: konum, yön, batarya, temizlenen %, kalan kirli alan, geçen süre | ✅ | `StatusPanel` + `ControlPanel` Robot Durumu (+ Ulaşılamaz bonusu) |
| 2.7 | En kısa yolla istasyona dönüş (BFS/A*) — düşük batarya **veya** kullanıcı komutu | ✅ | `PathFinder.aStar` (dönüş), her iki tetikleyici de bağlı |
| 2.8 | Farklı kir tipleri farklı sürelerde temizlenir (toz hızlı, sıvı/leke yavaş) | ✅ | `DirtType.cleaningSeconds` (0.8 / 2.5 / 4.0) |

**Sonuç: 8 zorunlu fonksiyonel başlığın tamamı karşılanıyor.**

---

## 3. Teknik Şartlar

| Şart | Durum | Kanıt |
|------|:-----:|-------|
| JavaFX GUI | ✅ | `view/*`, JavaFX 21 |
| Katı MVC | ✅ | `model/` ve `controller/` içinde **sıfır** JavaFX importu (grep ile doğrulandı) |
| İyi OOP | ✅ | Strategy (`CleaningStrategy`), Factory (`StrategyFactory`), enum'larda davranış, kapsülleme, tek-sorumluluk |
| Java Collections (List/Map/Queue) | ✅ | `Queue`/`ArrayDeque` (BFS, dönüş yolu), `PriorityQueue` (A*), `Map`/`LinkedHashMap`/`EnumMap`, `List` |
| 3. parti kütüphane YOK | ✅ | Sadece Java SE + JavaFX. Testler bile **saf Java** (JUnit yok) |

---

## 4. Bonus Özellikler

| Bonus | Durum | Not |
|------|:-----:|-----|
| Ulaşılamaz alan tespiti | ✅ | `PathFinder.reachable` + kırmızı X + "Ulaşılamaz" sayacı |
| Temizlik animasyonu | ✅ | Kir solma (alpha), yumuşak hareket+dönüş, temas gölgesi, kapsama heatmap'i |
| Çoklu oda düzeni | ✅ | `LayoutFactory` + `LayoutType` (Oturma Odası/Yatak Odası/Stüdyo) |
| Temizlik animasyonu / görsel cila | ✅ | PNG sprite'lar, halı overlay'i, kapsama heatmap'i, robot/batarya animasyonları |
| Gerçekçi keşif modu | ✅ | Sensör ışınları, inanç haritası, motor modeli ve gerçekçi sürüş |
| Ses efektleri | 🟡 | `javafx-media` pom'da hazır; ses dosyaları opsiyonel cila olarak açık |

---

## 5. Dokümantasyon (teslim şartı)

| Öğe | Durum | Not |
|-----|:-----:|-----|
| Detaylı proje raporu | ✅ | `docs/report/REPORT.md` |
| Sınıf diyagramı | ✅ | `docs/report/class-diagram.puml` + REPORT içinde Mermaid |
| Use-case diyagramı | ✅ | `docs/report/usecase-diagram.puml` |
| README / kullanım kılavuzu | ✅ | `README.md` |

---

## 7. Teslim Kontrol Listesi

| Öğe | Durum |
|-----|:-----:|
| Tüm `.java` kaynaklar | ✅ |
| JavaFX proje dosyaları (pom.xml vb.) | ✅ |
| Görsel/asset dosyaları | ✅ |
| README / kullanım kılavuzu | ✅ (`README.md`) |
| Proje raporu | ✅ |
| Çalışan uygulama ekran görüntüleri | ⏳ (alınacak) |
| Opsiyonel video/GIF | ⏳ (opsiyonel) |

## 8. Public Paylaşım & Teşekkür
✅ `README.md` BZ 214 dersini ve teşekkür metnini içeriyor.

---

## 🔎 Denetimde bulunan & düzeltilen tutarsızlıklar
- **A\* iddiası ama kullanılmıyordu** → istasyona dönüş artık gerçekten `PathFinder.aStar` kullanıyor; "Akıllı" stratejisi (çok-hedefli en-yakın-kir) doğru biçimde BFS kullanıyor. Etiketten yanıltıcı "(A*)" kaldırıldı.
- Kapsama döngüsü, Sıfırla bug'ı, mobilya edge-case'leri (önceki audit turunda giderildi).

## 🧭 Tasarım kararları (bilerek böyle — raporda gerekçelendirilecek)
- **Sürekli (piksel) hareket + grid kararları:** robot duvara hiç değmeden, hücre merkezinde yön değiştirir → "çarpınca yön değiştir" şartını çarpışmasız ve sağlam karşılar.
- **Dönüş sırasında temizlik yapmaz:** düşük batarya aciliyeti gereği en kısa yoldan döner (CLEANING modunda üstünden geçtiği her kiri temizler).
- **Batarya %0'da bile istasyona sürünür:** kullanıcıyı kilitlememek için (gerçekçilik istenirse "%0'da dur" seçeneğine çevrilebilir).

## ⚠️ Küçük açık uçlar (opsiyonel iyileştirme)
- `SimulationStats.cleanedPercentage()/remainingDirtyCount()` tanımlı ama panelde kapsama metriği tercih edildiğinden çağrılmıyor (ölü kod değil, alternatif gösterim; istenirse panele eklenir).
- `RoomCanvas.fillCell` kare-başına `ImagePattern` üretiyor; mevcut grid boyutunda sorun yok,
  daha büyük gridlerde tek desenle global döşemeye optimize edilebilir.
