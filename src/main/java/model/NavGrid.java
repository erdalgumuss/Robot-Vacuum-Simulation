package model;

/**
 * Gezinme algoritmalarinin uzerinde calistigi soyut grid gorunumu.
 * <p>
 * Strateji ({@link controller.algorithm.CleaningStrategy}) ve yol bulma
 * ({@link controller.PathFinder}) artik somut {@link Room}'a degil bu arayuze
 * baglidir. Boylece ayni algoritmalar iki farkli grid uzerinde calisabilir:
 * <ul>
 *   <li><b>Tanri Modu:</b> gercek {@link Room} (robot her seyi bilir).</li>
 *   <li><b>Gercekci Mod:</b> robotun yalnizca sensorle ogrendigi ic haritasi
 *       ({@link KnownMap}); bilinmeyen hucreler "yurunebilir" sayilarak kesfe
 *       acik kalir.</li>
 * </ul>
 * Bu, Acik/Kapali ilkesinin dogal bir uygulamasidir: gezinme mantigi degismeden
 * farkli bir dunya gorunumune uygulanir.
 */
public interface NavGrid {

    int rows();

    int cols();

    boolean inBounds(int row, int col);

    /** Bu hucreye yurunebilir mi (engel/duvar degil mi)? */
    boolean isWalkable(int row, int col);

    /** Bu hucrenin uzerinden gecildi (temizlendi) mi? */
    boolean isVisited(int row, int col);

    /** Bu hucrede kir var (veya gercekci modda sezildi) mi? */
    boolean isDirty(int row, int col);
}
