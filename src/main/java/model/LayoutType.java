package model;

/**
 * Hazır oda düzenleri ("çoklu oda düzeni" bonusu). Her düzen, bir mobilya
 * parçaları kümesi ve varsayılan kir dağılımıyla {@code LayoutFactory}
 * tarafından kurulur.
 */
public enum LayoutType {
    LIVING_ROOM("Oturma Odası"),
    BEDROOM("Yatak Odası"),
    STUDIO("Stüdyo");

    private final String label;

    LayoutType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
