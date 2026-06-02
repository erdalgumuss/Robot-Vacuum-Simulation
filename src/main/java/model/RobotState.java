package model;

/**
 * Robotun yasam dongusundeki durumlari (durum makinesi).
 * <ul>
 *   <li>{@link #IDLE} - bekliyor, simulasyon baslatilmadi/duraklatildi</li>
 *   <li>{@link #CLEANING} - aktif temizlik yapiyor / geziyor</li>
 *   <li>{@link #RETURNING} - dusuk batarya veya kullanici komutuyla istasyona donuyor</li>
 *   <li>{@link #CHARGING} - istasyonda sarj oluyor</li>
 *   <li>{@link #STUCK} - sikisti / ulasilabilir hedef kalmadi</li>
 *   <li>{@link #FINISHED} - ulasilabilir tum alan temizlendi</li>
 * </ul>
 */
public enum RobotState {
    IDLE("Bekliyor"),
    CLEANING("Temizliyor"),
    RETURNING("İstasyona Dönüyor"),
    CHARGING("Şarj Oluyor"),
    STUCK("Sıkıştı"),
    FINISHED("Tamamlandı");

    private final String label;

    RobotState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
