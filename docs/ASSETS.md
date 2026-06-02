# 🎨 Asset Üretim Kılavuzu — Robot Süpürge Simülasyonu

Bu döküman, premium görsel için gereken tüm asset'leri; **tam dosya adları, boyutlar,
footprint (hücre sayısı), format kuralları ve kopyala-yapıştır hazır üretim promtları**
ile listeler. Görsel üreticide (Codex/DALL·E/Midjourney/SDXL) sırayla üretebilirsin.

> **Güncel durum:** Faz 3 kapsamında zorunlu çekirdek set, premium mobilya seti,
> ahşap zemin, koyu duvar, dekoratif halı ve temizlik robotu varyantı üretilip
> `src/main/resources/png/` altına yerleştirildi. UI ikonları, uygulama ikonu ve
> ses efektleri opsiyonel cila olarak açık bırakılmıştır.

## Üretilen Asset Envanteri

| Durum | Dosya | Boyut | Not |
|------|-------|-------|-----|
| ✅ | `png/robot/robot.png` | 128×128 RGBA | Ana robot sprite'ı, kuzeye bakar |
| ✅ | `png/robot/robot_cleaning.png` | 128×128 RGBA | Temizlik durumunda kullanılan varyant |
| ✅ | `png/dirt/dirt_dust.png` | 128×128 RGBA | Toz |
| ✅ | `png/dirt/dirt_liquid.png` | 128×128 RGBA | Sıvı |
| ✅ | `png/dirt/dirt_stain.png` | 128×128 RGBA | Leke |
| ✅ | `png/charging_station.png` | 128×128 RGBA | Şarj istasyonu |
| ✅ | `png/floor/floor_tile.png` | 512×512 RGB | Opak ahşap zemin |
| ✅ | `png/floor/wall_tile.png` | 512×512 RGB | Opak koyu duvar texture'ı |
| ✅ | `png/floor/rug.png` | 384×256 RGBA | Dekoratif halı overlay'i |
| ✅ | `png/furniture/sofa.png` | 256×128 RGBA | 2×1 kanepe |
| ✅ | `png/furniture/coffee_table.png` | 256×128 RGBA | 2×1 orta sehpa |
| ✅ | `png/furniture/bookshelf.png` | 256×128 RGBA | 2×1 kitaplık/konsol |
| ✅ | `png/furniture/tv_console.png` | 256×128 RGBA | 2×1 TV ünitesi |
| ✅ | `png/furniture/dining_table.png` | 256×256 RGBA | 2×2 yemek masası |
| ✅ | `png/furniture/bed.png` | 256×256 RGBA | 2×2 yatak |
| ✅ | `png/furniture/armchair.png` | 128×128 RGBA | 1×1 koltuk |
| ✅ | `png/furniture/plant_large.png` | 128×128 RGBA | 1×1 büyük bitki |
| ✅ | `png/furniture/plant_small.png` | 128×128 RGBA | 1×1 küçük bitki |
| ✅ | `png/furniture/side_table.png` | 128×128 RGBA | 1×1 yan sehpa |
| ⏳ | `png/app_icon.png` | 256×256 | Opsiyonel |
| ⏳ | `png/ui/icon_*.png` | 64×64 | Opsiyonel |
| ⏳ | `sound/*.mp3` / `sound/*.wav` | değişken | Opsiyonel ses efektleri |

### Render Entegrasyonu

- `SpriteAssets`, dosyaları classpath üzerinden yükler ve eksik asset durumunda
  `null` döndürür.
- `RoomCanvas`, asset varsa PNG'yi, yoksa Canvas fallback çizimini kullanır.
- Halı (`rug.png`) zemin üstünde dekoratif overlay olarak çizilir; yol bulma veya
  çarpışma mantığını değiştirmez.
- Mobilyalar footprint'e göre çizilir: 1×1, 2×1 ve 2×2 sprite'lar grid'e uygun
  ölçekte yerleşir.
- Şeffaf sprite'lar chroma-key üretim sonrası alfa kanallı PNG'ye dönüştürülmüş,
  opak texture'lar RGB olarak bırakılmıştır.

---

## 0. Genel Kurallar (HEPSİ için geçerli)

| Kural | Değer |
|------|-------|
| Görünüm | **Tepeden (kuş bakışı / top-down) ortografik** — yan perspektif YOK |
| Format | **PNG-24, alfa kanalı (şeffaf arka plan)** — _floor/wall texture'lar hariç (opak)_ |
| Arka plan | **Tamamen şeffaf**, zemin/gölge ZEMİNİ çizilmeyecek (gölge nesnenin altında yumuşak olabilir) |
| Işık | Tek yumuşak ışık kaynağı, **sol-üstten**; tüm asset'lerde tutarlı |
| Ölçek birimi | **1 grid hücresi = 128×128 px** (uygulamada hücre 38px; 128px net küçültme sağlar) |
| Çok hücreli parça | footprint × 128px (örn. 2×1 sofa = **256×128**, 2×2 masa = **256×256**) |
| Dolgu | Nesne, footprint'inin **~%90'ını** doldursun, kenarda ~%5 boşluk |
| Stil | Modern **flat-design + yumuşak 3B gölge**, sıcak/cozy oturma odası paleti, temiz vektör-benzeri gölgeleme, ürün-render kalitesi |
| Renk paleti | Sıcak ahşap zemin (#3a2e22), aksanlar: mavi #3b82f6, yeşil #16a34a, krem/sage döşeme |

### 🔑 STİL ÇAPASI (her promtun başına ekle — tutarlılık için)
> *"Top-down (bird's-eye) orthographic view, modern flat design with soft 3D shading and a gentle soft shadow, warm cozy living-room palette, single soft light source from the top-left, centered and isolated on a fully transparent background, no floor, no text, no watermark, clean game-asset quality, crisp edges, 1:1 perspective."*

> 💡 **İpucu:** Tüm mobilyaları **tek oturumda, aynı stil çapasıyla** üret → perspektif/ölçek/ışık tutarlı olur. Üretim sonrası arka planı şeffaf yap (gerekirse remove.bg) ve footprint oranına göre kırp.

---

## 1. Robot (zorunlu) → `src/main/resources/png/robot/`

> ⚠️ **Yön:** Robotu **YUKARI (kuzey) bakacak** şekilde çiz. Kod, robotu `heading` açısına göre döndürür (offset kodda ayarlanır). Ön tarafı (sensör/tampon) üstte olsun ki dönüş okunabilsin.

| Dosya | Footprint | px | Açıklama |
|------|-----------|-----|----------|
| `robot.png` | 1×1 | 128×128 | Ana robot gövdesi |
| `robot_cleaning.png` _(ops.)_ | 1×1 | 128×128 | Temizlerken (hafif parıltı/fırça efekti) |

**Promt — `robot.png`:**
```
[STİL ÇAPASI] + a sleek modern round robot vacuum cleaner seen directly from
above (top-down), glossy pearl-white and dark charcoal two-tone body, a thin
glowing blue LED accent line, a small subtle front sensor bump at the TOP edge
indicating the front, a faint circular side-brush hint, premium minimalist
product design, soft contact shadow beneath, perfectly circular silhouette
filling ~85% of the square frame.
```

**Promt — `robot_cleaning.png` (opsiyonel):**
```
[STİL ÇAPASI] + same round top-down robot vacuum as before but actively
cleaning: a soft swirling dust/air motion glow around its base, faint blue
suction shimmer, identical body design and orientation (front at the TOP).
```

---

## 2. Kir Tipleri (zorunlu) → `src/main/resources/png/dirt/`

> Kir, temizlendikçe kod tarafından **soldurulur (alpha)**; bu yüzden net, ortalanmış olsun.

| Dosya | Footprint | px | Açıklama |
|------|-----------|-----|----------|
| `dirt_dust.png` | 1×1 | 128×128 | Toz (hızlı temizlenir) |
| `dirt_liquid.png` | 1×1 | 128×128 | Sıvı (yavaş) |
| `dirt_stain.png` | 1×1 | 128×128 | Leke (en yavaş) |

**`dirt_dust.png`:**
```
[STİL ÇAPASI] + a small scattered cluster of grey dust bunnies, lint clumps and
a few thin hair strands seen from above, light fluffy soft debris, irregular
natural scatter, very subtle shadow, centered, occupying ~70% of the frame.
```
**`dirt_liquid.png`:**
```
[STİL ÇAPASI] + a glossy translucent blue water spill puddle seen from above,
smooth irregular organic blob shape, glistening light reflections and a wet
sheen, slightly darker rim, centered, occupying ~75% of the frame.
```
**`dirt_stain.png`:**
```
[STİL ÇAPASI] + a dried dark blotchy stain mark seen from above, irregular
muddy brown-purple spot with faded soft edges and a darker core, matte texture,
centered, occupying ~70% of the frame.
```

---

## 3. Şarj İstasyonu (zorunlu) → `src/main/resources/png/`

| Dosya | Footprint | px | Açıklama |
|------|-----------|-----|----------|
| `charging_station.png` | 1×1 | 128×128 | Dock |

**Promt:**
```
[STİL ÇAPASI] + a modern robot vacuum charging dock seen from above, sleek
dark charcoal base plate, a glowing green charging pad with two metallic charging
contact strips and a white lightning-bolt symbol in the center, subtle green LED
glow ring, premium minimalist, centered, filling ~85% of the frame.
```

---

## 4. Zemin & Duvar Texture (önerilen) → `src/main/resources/png/floor/`

> ⚠️ Bunlar **OPAK** (şeffaf değil) ve **seamless (kesintisiz döşenebilir)** olmalı.

| Dosya | px | Açıklama |
|------|-----|----------|
| `floor_tile.png` | 512×512 | Kesintisiz ahşap parke zemin |
| `wall_tile.png` _(ops.)_ | 512×512 | Koyu duvar/süpürgelik |
| `rug.png` _(ops.)_ | 384×256 (3×2) | Dekoratif halı (yarı saydam kenar) |

**`floor_tile.png`:**
```
Seamless tileable top-down warm wooden parquet floor texture, light oak planks
with subtle natural grain, cozy living-room flooring, even soft lighting, no
shadows, no objects, perfectly tileable on all four edges, high detail, square.
```
**`wall_tile.png` (ops.):**
```
Seamless tileable top-down dark matte wall / baseboard texture, deep charcoal
with very subtle vertical grain, even lighting, no shadows, perfectly tileable,
square.
```
**`rug.png` (ops.):**
```
[STİL ÇAPASI] + a rectangular decorative area rug seen from above, soft geometric
pattern in muted teal/cream/terracotta, fringed edges, flat woven texture,
centered, filling the frame.
```

---

## 5. Mobilya Seti (önerilen, premium oda için) → `src/main/resources/png/furniture/`

> 📐 Footprint, kaç grid hücresi kapladığını belirtir. **px = footprint × 128.**
> Kod, çok hücreli mobilya parçalarını destekleyecek şekilde genişletilecek
> (her parça bir footprint + sprite ile yerleşir). Hepsini aynı stil/ışıkla üret.

| Dosya | Footprint | px | Açıklama |
|------|-----------|-----|----------|
| `sofa.png` | 2×1 | 256×128 | 3'lü kanepe |
| `armchair.png` | 1×1 | 128×128 | Tekli koltuk |
| `coffee_table.png` | 2×1 | 256×128 | Orta sehpa |
| `dining_table.png` | 2×2 | 256×256 | 4 sandalyeli yemek masası |
| `bookshelf.png` | 2×1 | 256×128 | Kitaplık/konsol (duvara dayalı) |
| `tv_console.png` | 2×1 | 256×128 | TV ünitesi |
| `bed.png` | 2×2 | 256×256 | Yatak (yatak odası düzeni — çoklu oda bonusu) |
| `plant_small.png` | 1×1 | 128×128 | Küçük saksı bitki |
| `plant_large.png` | 1×1 | 128×128 | Büyük yapraklı bitki (monstera) |
| `side_table.png` | 1×1 | 128×128 | Yan sehpa |

**`sofa.png`:**
```
[STİL ÇAPASI] + a modern 3-seat fabric sofa seen from directly above, sage-green
cushions with two soft throw pillows, slim wooden legs barely visible, plush and
cozy, rectangular footprint wider than tall (2:1), filling ~90% of the frame.
```
**`armchair.png`:**
```
[STİL ÇAPASI] + a single cozy accent armchair seen from above, cream/beige
upholstery, rounded plush back and arms, wooden legs hint, square footprint,
centered.
```
**`coffee_table.png`:**
```
[STİL ÇAPASI] + a rectangular walnut wooden coffee table seen from above, smooth
top surface with a small stack of books and a tiny plant, two-tone wood, 2:1
footprint, centered.
```
**`dining_table.png`:**
```
[STİL ÇAPASI] + a square wooden dining table with four matching chairs neatly
tucked around it, seen from directly above, light oak top, a small centerpiece
bowl, balanced square 2x2 composition, centered.
```
**`bookshelf.png`:**
```
[STİL ÇAPASI] + a low wooden bookshelf / console seen from above, rows of
colorful book spines (tops visible), a couple of decor items and a small plant,
designed to sit against a wall, 2:1 footprint, centered.
```
**`tv_console.png`:**
```
[STİL ÇAPASI] + a sleek dark media TV console/cabinet seen from above, matte
charcoal top with a slim soundbar and a small decor object, 2:1 footprint,
centered.
```
**`bed.png`:**
```
[STİL ÇAPASI] + a neatly made double bed seen from directly above, soft white
duvet with two pillows and a folded throw blanket at the foot, light wooden
frame, square 2x2 footprint, centered.
```
**`plant_small.png`:**
```
[STİL ÇAPASI] + a small potted house plant seen from above, lush rounded green
leaves spilling over a terracotta pot, healthy and vibrant, square footprint,
centered.
```
**`plant_large.png`:**
```
[STİL ÇAPASI] + a large leafy monstera house plant in a woven basket pot seen
from above, big split green leaves filling the frame, square footprint, centered.
```
**`side_table.png`:**
```
[STİL ÇAPASI] + a small round side table seen from above, wooden top with a
lamp base and a coffee mug, compact square footprint, centered.
```

---

## 6. Uygulama İkonu (önerilen) → `src/main/resources/png/`

| Dosya | px | Açıklama |
|------|-----|----------|
| `app_icon.png` | 256×256 | Pencere/görev çubuğu ikonu (arka plan olabilir, yuvarlatılmış) |

**Promt:**
```
A friendly modern robot vacuum cleaner mascot icon, three-quarter cute view,
glossy white and charcoal body with a glowing blue LED smile, on a soft rounded
dark-navy gradient square background, app-icon style, clean, centered, 256x256.
```

---

## 7. UI İkon Seti (opsiyonel polisaj) → `src/main/resources/png/ui/`

> Şu an panelde emoji kullanıyoruz; istersen bu ikon setiyle değiştiririz.
> Hepsi **64×64, şeffaf, flat duotone** (mavi #3b82f6 + açık gri), tutarlı çizgi kalınlığı.

`icon_dust.png`, `icon_liquid.png`, `icon_stain.png`, `icon_furniture.png`,
`icon_play.png`, `icon_pause.png`, `icon_reset.png`, `icon_home.png`,
`icon_speed.png`, `icon_battery.png`, `icon_robot.png`, `icon_algorithm.png`

**Toplu promt:**
```
A set of minimalist flat duotone UI icons, 64x64 each, blue (#3b82f6) and light
grey, consistent 2px stroke, rounded, on transparent background: a dust cloud, a
water droplet, a stain blotch, a sofa, a play triangle, a pause bars, a reset
circular arrow, a home, a speedometer, a battery, a round robot vacuum, a spiral
path. Clean, modern, cohesive icon family.
```

---

## 8. Ses Efektleri (bonus — Faz 4) → `src/main/resources/sound/`

> 🔊 Görsel üretici ses üretmez. Bunları **bir ses kütüphanesinden** (freesound.org,
> pixabay/zapsplat) indir ya da bir ses-üretim aracı kullan. Format: **MP3 veya WAV**.

| Dosya | Süre | Açıklama / Arama kelimesi |
|------|------|---------------------------|
| `vacuum_loop.mp3` | ~2-3 sn (loopable) | Yumuşak elektrikli süpürge uğultusu — "vacuum hum loop" |
| `charge_beep.mp3` | ~1 sn | Şarj başlama bip'i — "device charging beep" |
| `bump.mp3` | ~0.3 sn | Engele değme — "soft bump thud" |
| `clean_ding.mp3` | ~0.5 sn | Bir kir temizlendi — "soft pop / sparkle" |
| `complete_chime.mp3` | ~1.5 sn | Görev tamamlandı — "success chime" |
| `button_click.mp3` _(ops.)_ | ~0.1 sn | UI tık sesi — "ui click" |

---

## 9. Teslim & Yerleşim Özeti

```
src/main/resources/
├── png/
│   ├── robot/        robot.png, robot_cleaning.png
│   ├── dirt/         dirt_dust.png, dirt_liquid.png, dirt_stain.png
│   ├── floor/        floor_tile.png, wall_tile.png, rug.png
│   ├── furniture/    sofa.png, armchair.png, coffee_table.png, dining_table.png,
│   │                 bookshelf.png, tv_console.png, bed.png, plant_small.png,
│   │                 plant_large.png, side_table.png
│   ├── ui/           icon_*.png  (opsiyonel)
│   ├── charging_station.png
│   └── app_icon.png
└── sound/            vacuum_loop.mp3, charge_beep.mp3, bump.mp3,
                      clean_ding.mp3, complete_chime.mp3, button_click.mp3
```

> `pom.xml` zaten `png/jpg/css/mp3/wav` dosyalarını paketliyor. Alt klasörler sorun değil.

### Öncelik sırası (minimum → premium)
1. **Zorunlu:** `robot.png`, `dirt_dust/liquid/stain.png`, `charging_station.png`, `floor_tile.png`
2. **Premium oda:** `furniture/*` + `rug.png`
3. **Cila:** `app_icon.png`, `ui/icon_*`, `robot_cleaning.png`, sesler

> Sadece 1. grubu üretsen bile premium bir sıçrama olur; gerisini sonra ekleriz.
