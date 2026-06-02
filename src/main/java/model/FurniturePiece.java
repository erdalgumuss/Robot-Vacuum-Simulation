package model;

/**
 * Odaya yerleştirilmiş somut bir mobilya parçası: türü + sol-üst köşe (anchor)
 * konumu. Kapladığı tüm hücreler {@link CellType#FURNITURE} olarak işaretlenir
 * (çarpışma/yol bulma için); bu kayıt ise doğru sprite'ı doğru footprint'e
 * çizmek için kullanılır.
 *
 * @param type mobilya türü (footprint ve sprite bilgisini taşır)
 * @param row  sol-üst köşe satırı
 * @param col  sol-üst köşe sütunu
 */
public record FurniturePiece(FurnitureType type, int row, int col) {

    public int rows() { return type.rows(); }
    public int cols() { return type.cols(); }

    /** (r, c) hücresi bu parçanın footprint'i içinde mi? */
    public boolean covers(int r, int c) {
        return r >= row && r < row + type.rows()
                && c >= col && c < col + type.cols();
    }
}
