package controller.drive;

import model.Direction;

/**
 * Robotu süren beyin soyutlaması. İki uygulaması vardır:
 * <ul>
 *   <li>{@link OmniscientDriver} — "Tanrı Modu": tüm odayı bilen, strateji + A*
 *       tabanlı verimli temizlik (mevcut davranış).</li>
 *   <li>{@link ReactiveDriver} — "Gerçekçi Mod": robot ortamı bilmez, yalnızca
 *       sensörlerinden öğrenir ve serbest açılı hareketle gezer.</li>
 * </ul>
 * {@code RobotController} aktif sürücüye delege eder; böylece mod değişimi tek
 * noktadan yönetilir ve mevcut çağrı yerleri ({@code controller().step()})
 * değişmeden çalışmaya devam eder.
 */
public interface Driver {

    /** Bir simülasyon tikini işler (dt saniye). */
    void step(double dt);

    /** Robotu istasyona ve başlangıç durumuna alır. */
    void resetToStation();

    /** Robotu temizlik moduna geçirir (Başlat). */
    void beginCleaning();

    /** Kullanıcı komutu: en kısa yoldan istasyona dön. */
    void requestReturnToStation();

    /** Robot hız çarpanını ayarlar. */
    void setSpeedMultiplier(double speedMultiplier);

    /** Robotun o anki yönü (ekranda gösterim için; bilinmiyorsa null). */
    Direction currentDirection();
}
