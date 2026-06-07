# Ekran Görüntüleri

Bu klasör, çalışan uygulamanın ekran görüntülerini içerir (teslim şartı).
Görselleri buraya `.png` olarak kaydedin; aşağıdaki liste her görüntünün hangi
gereksinimi kanıtladığını gösterir. Önerilen 6 kare tüm zorunlu maddeleri ve
bonusları kapsar.

| Dosya | Önerilen kare | Kanıtladığı gereksinim |
|-------|---------------|------------------------|
| `01-overview.png` | Açılış: oda grid'i, robot, şarj istasyonu, mobilyalar, kontrol paneli | 2.1 Arayüz, 2.2c engel, MVC GUI |
| `02-cleaning.png` | Temizlik çalışırken: hareket izi + kısmen solmuş kirler + canlı durum paneli | 2.3a Başlat, 2.5b temizleme, 2.6 gerçek zamanlı |
| `03-dirt-types.png` | Üç kir tipi (toz/sıvı/leke) odada görünür + kir türü seçimi | 2.2a/b kir ekleme & tür, 2.4 üç tip |
| `04-return-path.png` | Düşük batarya veya "İstasyona Dön": A* dönüş yolu | 2.3d / 2.7 en kısa yol |
| `05-unreachable.png` | Mobilyayla çevrelenmiş ulaşılamaz kir: kırmızı X + sayaç | Bonus: ulaşılamaz alan |
| `06-realistic-mode.png` | Gerçekçi mod: sensör ışınları + inanç sisi + telemetri radarı | Bonus: gerçekçi keşif modu |

> İpucu: Pencere kenarlıksız (custom title bar). Tek pencereyi yakalamak için
> Windows'ta **Alt + PrtScn** (yalnız aktif pencere) ya da **Win + Shift + S**
> (bölge seçimi) kullanın.

İsteğe bağlı: kısa bir ekran kaydı / GIF de eklenebilir (`07-demo.gif`).
