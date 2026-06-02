package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Parametrik oda modeli: satir x sutun boyutlu bir {@link Cell} grid'i.
 * <p>
 * Oda boyutu yapilandirilabilir oldugu icin "coklu oda duzeni" bonusu
 * dogal olarak desteklenir. Bu sinif yalnizca verinin sahibidir; gezinme,
 * temizleme ve yol bulma mantigi controller katmanina aittir.
 * <p>
 * Robotun konumu surekli (piksel) oldugu icin Room, dunyayi mantiksal grid
 * olarak temsil eder; view katmani hucre boyutuyla carparak piksele cevirir.
 */
public class Room {

    private final int rows;
    private final int cols;
    private final Cell[][] grid;

    private int stationRow;
    private int stationCol;

    // Isimli mobilya parcalari (footprint + sprite icin). Kapladiklari hucreler
    // ayrica grid'de FURNITURE olarak isaretlidir (carpisma/yol bulma icin).
    private final List<FurniturePiece> furniture = new ArrayList<>();

    public Room(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Cell[rows][cols];
        buildEmptyRoom();
    }

    /** Dis sinirlari duvar, ici zemin olan bos bir oda kurar; istasyonu sol-alt koseye koyar. */
    private void buildEmptyRoom() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean border = (r == 0 || c == 0 || r == rows - 1 || c == cols - 1);
                grid[r][c] = new Cell(r, c, border ? CellType.WALL : CellType.FLOOR);
            }
        }
        // Varsayilan sarj istasyonu: sol-alt ic kose
        setStation(rows - 2, 1);
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public Cell cell(int row, int col) {
        return grid[row][col];
    }

    public boolean isWalkable(int row, int col) {
        return inBounds(row, col) && grid[row][col].isWalkable();
    }

    // --- Sarj istasyonu ---

    public void setStation(int row, int col) {
        if (!inBounds(row, col)) {
            return;
        }
        // Onceki istasyonu zemine cevir
        if (grid[stationRow][stationCol].type() == CellType.STATION) {
            grid[stationRow][stationCol].setType(CellType.FLOOR);
        }
        this.stationRow = row;
        this.stationCol = col;
        grid[row][col].setType(CellType.STATION);
        grid[row][col].resetDirt();
    }

    public int stationRow() { return stationRow; }
    public int stationCol() { return stationCol; }
    public Cell stationCell() { return grid[stationRow][stationCol]; }

    // --- Engel ve kir duzenleme ---

    public void setFurniture(int row, int col) {
        if (inBounds(row, col) && grid[row][col].type() == CellType.FLOOR) {
            grid[row][col].setType(CellType.FURNITURE);
        }
    }

    public void clearObstacle(int row, int col) {
        if (inBounds(row, col) && grid[row][col].type() == CellType.FURNITURE) {
            grid[row][col].setType(CellType.FLOOR);
        }
    }

    // --- Isimli mobilya parcalari ---

    public List<FurniturePiece> furniturePieces() {
        return furniture;
    }

    /** (row, col) hucresini kaplayan mobilya parcasi (yoksa null). */
    public FurniturePiece pieceAt(int row, int col) {
        for (FurniturePiece p : furniture) {
            if (p.covers(row, col)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Sol-ust kosesi (row, col) olan bir mobilya parcasi yerlestirir.
     * Tum footprint hucreleri sinir icinde ve bos zemin olmalidir.
     *
     * @return yerlestirildiyse true
     */
    public boolean placeFurniture(FurnitureType type, int row, int col) {
        for (int r = row; r < row + type.rows(); r++) {
            for (int c = col; c < col + type.cols(); c++) {
                if (!inBounds(r, c) || grid[r][c].type() != CellType.FLOOR) {
                    return false;
                }
            }
        }
        for (int r = row; r < row + type.rows(); r++) {
            for (int c = col; c < col + type.cols(); c++) {
                grid[r][c].setType(CellType.FURNITURE);
            }
        }
        furniture.add(new FurniturePiece(type, row, col));
        return true;
    }

    /**
     * (row, col)'u kaplayan mobilyayi kaldirir: isimli parca ise tum footprint'i,
     * tekil engel hucresi ise yalnizca o hucreyi zemine cevirir.
     *
     * @return bir sey kaldirildiysa true
     */
    public boolean removeFurnitureAt(int row, int col) {
        FurniturePiece piece = pieceAt(row, col);
        if (piece != null) {
            for (int r = piece.row(); r < piece.row() + piece.rows(); r++) {
                for (int c = piece.col(); c < piece.col() + piece.cols(); c++) {
                    grid[r][c].setType(CellType.FLOOR);
                }
            }
            furniture.remove(piece);
            return true;
        }
        if (inBounds(row, col) && grid[row][col].type() == CellType.FURNITURE) {
            grid[row][col].setType(CellType.FLOOR);
            return true;
        }
        return false;
    }

    /** Tum mobilyalari (isimli parcalar + tekil engeller) kaldirir; oda duzeni degisiminde kullanilir. */
    public void clearAllFurniture() {
        furniture.clear();
        for (Cell[] rowArr : grid) {
            for (Cell cell : rowArr) {
                if (cell.type() == CellType.FURNITURE) {
                    cell.setType(CellType.FLOOR);
                }
            }
        }
    }

    public void addDirt(int row, int col, DirtType dirt) {
        if (inBounds(row, col)) {
            grid[row][col].setDirt(dirt);
        }
    }

    // --- Yuzey (hali) ---

    public void setCarpet(int row, int col) {
        if (inBounds(row, col) && grid[row][col].type() == CellType.FLOOR) {
            grid[row][col].setSurface(SurfaceType.CARPET);
        }
    }

    public void clearCarpet(int row, int col) {
        if (inBounds(row, col)) {
            grid[row][col].setSurface(SurfaceType.HARD_FLOOR);
        }
    }

    /** Tum yuzeyleri sert zemine dondurur (oda duzeni degisiminde). */
    public void resetSurfaces() {
        for (Cell[] rowArr : grid) {
            for (Cell cell : rowArr) {
                cell.setSurface(SurfaceType.HARD_FLOOR);
            }
        }
    }

    // --- Istatistik yardimcilari ---

    /** Temizlenebilir (zemin) toplam hucre sayisi. */
    public int totalFloorCells() {
        int count = 0;
        for (Cell[] rowArr : grid) {
            for (Cell cell : rowArr) {
                if (cell.type().isCleanable()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int dirtyCellCount() {
        int count = 0;
        for (Cell[] rowArr : grid) {
            for (Cell cell : rowArr) {
                if (cell.isDirty()) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Robotun ustunden gectigi (temizledigi) zemin hucresi sayisi - kapsama olcumu. */
    public int visitedFloorCells() {
        int count = 0;
        for (Cell[] rowArr : grid) {
            for (Cell cell : rowArr) {
                if (cell.type().isCleanable() && cell.isVisited()) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Kirleri ve ziyaret izlerini temizler; oda yapisini (duvar/mobilya) korur. */
    public void resetDirtAndVisits() {
        for (Cell[] rowArr : grid) {
            for (Cell cell : rowArr) {
                cell.resetDirt();
            }
        }
    }

    /** Tum kirli hucrelerin listesi (yol bulma / hedef secimi icin). */
    public List<Cell> dirtyCells() {
        List<Cell> result = new ArrayList<>();
        for (Cell[] rowArr : grid) {
            for (Cell cell : rowArr) {
                if (cell.isDirty()) {
                    result.add(cell);
                }
            }
        }
        return result;
    }
}
