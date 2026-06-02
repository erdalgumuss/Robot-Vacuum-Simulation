package controller;

import model.DirtType;
import model.FurnitureType;
import model.LayoutType;
import model.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * Hazır oda düzenlerini kurar ("çoklu oda düzeni" bonusu). Her düzen, odaya
 * isimli mobilya parçaları yerleştirir ve varsayılan bir kir dağılımı döndürür.
 * Düzenler 14×20 grid ve sol-alt köşedeki (12,1) şarj istasyonu varsayımıyla
 * tasarlanmıştır.
 */
public final class LayoutFactory {

    private LayoutFactory() { }

    /** Bir düzendeki tek bir kir yerleşimi. */
    public record DirtSpec(int row, int col, DirtType type) { }

    /** Dikdörtgen bir bölgeyi halı yapar (yalnız zemin hücreleri etkilenir). */
    private static void carpet(Room room, int r0, int c0, int r1, int c1) {
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                room.setCarpet(r, c);
            }
        }
    }

    /**
     * Odaya seçilen düzenin mobilyalarını yerleştirir ve kir listesini döndürür.
     * (Odanın mevcut mobilya/kiri çağırandan önce temizlenmiş olmalıdır.)
     */
    public static List<DirtSpec> apply(Room room, LayoutType type) {
        return switch (type) {
            case LIVING_ROOM -> livingRoom(room);
            case BEDROOM -> bedroom(room);
            case STUDIO -> studio(room);
        };
    }

    private static List<DirtSpec> livingRoom(Room room) {
        room.placeFurniture(FurnitureType.DINING_TABLE, 1, 3);
        room.placeFurniture(FurnitureType.SOFA, 1, 12);
        room.placeFurniture(FurnitureType.COFFEE_TABLE, 3, 12);
        room.placeFurniture(FurnitureType.ARMCHAIR, 3, 9);
        room.placeFurniture(FurnitureType.PLANT, 6, 2);
        room.placeFurniture(FurnitureType.PLANT, 1, 17);
        carpet(room, 5, 8, 9, 14); // oturma grubu önünde halı
        // Ulaşılamaz cep (bonus demosu): köşeyi iki bitkiyle kapat
        room.placeFurniture(FurnitureType.PLANT, 11, 18);
        room.placeFurniture(FurnitureType.PLANT, 12, 17);

        List<DirtSpec> dirt = new ArrayList<>();
        dirt.add(new DirtSpec(4, 4, DirtType.DUST));
        dirt.add(new DirtSpec(2, 8, DirtType.DUST));
        dirt.add(new DirtSpec(8, 6, DirtType.DUST));
        dirt.add(new DirtSpec(10, 10, DirtType.DUST));
        dirt.add(new DirtSpec(9, 15, DirtType.DUST));
        dirt.add(new DirtSpec(6, 9, DirtType.LIQUID));
        dirt.add(new DirtSpec(5, 16, DirtType.LIQUID));
        dirt.add(new DirtSpec(10, 3, DirtType.STAIN));
        dirt.add(new DirtSpec(12, 18, DirtType.STAIN)); // ulaşılamaz
        return dirt;
    }

    private static List<DirtSpec> bedroom(Room room) {
        room.placeFurniture(FurnitureType.BED, 1, 3);
        room.placeFurniture(FurnitureType.ARMCHAIR, 2, 14);
        room.placeFurniture(FurnitureType.COFFEE_TABLE, 10, 14);
        room.placeFurniture(FurnitureType.PLANT, 10, 2);
        room.placeFurniture(FurnitureType.PLANT, 3, 17);
        carpet(room, 6, 7, 10, 13); // yatak odası halısı

        List<DirtSpec> dirt = new ArrayList<>();
        dirt.add(new DirtSpec(5, 6, DirtType.DUST));
        dirt.add(new DirtSpec(8, 9, DirtType.DUST));
        dirt.add(new DirtSpec(11, 11, DirtType.DUST));
        dirt.add(new DirtSpec(4, 12, DirtType.DUST));
        dirt.add(new DirtSpec(7, 4, DirtType.LIQUID));
        dirt.add(new DirtSpec(9, 16, DirtType.STAIN));
        dirt.add(new DirtSpec(2, 10, DirtType.LIQUID));
        return dirt;
    }

    private static List<DirtSpec> studio(Room room) {
        room.placeFurniture(FurnitureType.SOFA, 3, 5);
        room.placeFurniture(FurnitureType.COFFEE_TABLE, 5, 5);
        room.placeFurniture(FurnitureType.PLANT, 2, 15);
        room.placeFurniture(FurnitureType.ARMCHAIR, 8, 13);
        carpet(room, 7, 9, 10, 15); // stüdyo halısı

        List<DirtSpec> dirt = new ArrayList<>();
        dirt.add(new DirtSpec(2, 3, DirtType.DUST));
        dirt.add(new DirtSpec(6, 10, DirtType.DUST));
        dirt.add(new DirtSpec(10, 8, DirtType.DUST));
        dirt.add(new DirtSpec(4, 16, DirtType.DUST));
        dirt.add(new DirtSpec(8, 5, DirtType.LIQUID));
        dirt.add(new DirtSpec(11, 14, DirtType.STAIN));
        return dirt;
    }
}
