package model;

/**
 * Robotun bir andaki sensör okuması — gerçek hayattaki gibi yalnızca yerel,
 * ölçülmüş veriler. Robot bu kayıttan kararlarını üretir; gerçek odaya bakmaz.
 * <p>
 * Saf veri olduğu ve robotun kendi duyusunu temsil ettiği için model katmanında
 * yer alır (üretici {@code controller.perception.PerceptionService}).
 *
 * @param rayAngles    her ışının dünya açısı (radyan)
 * @param rayDistances her ışının robot kenarından engele mesafesi (piksel)
 * @param bumpFront    ön sektörde temas
 * @param bumpLeft     sol sektörde temas
 * @param bumpRight    sağ sektörde temas
 * @param dirtDetected robotun altında kir algılandı mı
 * @param dirtRow      algılanan kirin satırı (-1 yoksa)
 * @param dirtCol      algılanan kirin sütunu (-1 yoksa)
 * @param onCarpet     robot şu an halı üstünde mi (motor direncinden hissedilir)
 * @param battery      batarya seviyesi (%)
 */
public record SensorReading(
        double[] rayAngles,
        double[] rayDistances,
        boolean bumpFront,
        boolean bumpLeft,
        boolean bumpRight,
        boolean dirtDetected,
        int dirtRow,
        int dirtCol,
        boolean onCarpet,
        double battery
) {
    public int rayCount() {
        return rayAngles.length;
    }

    public boolean anyBump() {
        return bumpFront || bumpLeft || bumpRight;
    }

    /** Boş/etkisiz okuma (henüz algılama yapılmadıysa). */
    public static SensorReading empty() {
        return new SensorReading(new double[0], new double[0],
                false, false, false, false, -1, -1, false, 100.0);
    }
}
