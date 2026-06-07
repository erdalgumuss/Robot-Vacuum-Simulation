package view;

import javafx.scene.control.Label;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 * Küçük, durumsuz JavaFX view yardımcıları. View paketinin geneli (kontrol,
 * durum ve telemetri panelleri) tarafından paylaşılır.
 */
final class UiUtil {

    private UiUtil() { }

    /**
     * Etiketi yalnızca metni gerçekten değiştiyse günceller. Paneller her karede
     * (~60 fps) çağrıldığından, değişmeyen metni tekrar yazmak gereksiz layout/CSS
     * yeniden hesaplaması doğurur; bu kontrol onu önler (davranış birebir aynıdır).
     */
    static void setIfChanged(Label label, String text) {
        if (!label.getText().equals(text)) {
            label.setText(text);
        }
    }

    /**
     * Bir enum (ya da herhangi bir tür) için, görünen metni {@code labelFn} ile
     * üreten salt-gösterim ComboBox dönüştürücüsü. Düzenleme yapılmadığından
     * {@code fromString} kullanılmaz.
     */
    static <T> StringConverter<T> labelConverter(Function<T, String> labelFn) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : labelFn.apply(value); }
            @Override public T fromString(String s) { return null; }
        };
    }
}
