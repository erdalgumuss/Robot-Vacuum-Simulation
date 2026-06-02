package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import model.Cell;
import model.CellType;
import model.DirtType;
import model.FurniturePiece;
import model.KnownMap;
import model.Robot;
import model.Room;
import model.SensorReading;
import util.SimConstants;

/**
 * Odayi ve robotu cizen JavaFX Canvas. View katmanindadir; modeli yalnizca
 * <b>okur</b>, degistirmez. Surekli (piksel) koordinatlar dogrudan cizime
 * uygundur; grid hucreleri CELL_SIZE ile carpilarak piksele cevrilir.
 * <p>
 * Faz 0: grid, duvarlar, mobilya, kir, istasyon, iz ve robot govdesi cizilir.
 * Animasyon/efektler ileri fazda zenginlestirilecek.
 */
public class RoomCanvas extends Canvas {

    private static final Color BG = Color.web("#2b2118");
    private static final Color FLOOR = Color.web("#3a2e22");
    private static final Color GRID_LINE = Color.web("#ffffff", 0.06);
    private static final Color WALL = Color.web("#1c140d");
    private static final Color FURNITURE = Color.web("#6b4a2b");
    private static final Color STATION = Color.web("#16a34a");
    private static final Color TRAIL = Color.web("#38bdf8", 0.55);
    private static final Color ROBOT_BODY = Color.web("#e5e7eb");
    private static final Color ROBOT_EDGE = Color.web("#0f172a");
    private static final Color MISSED = Color.web("#ef4444", 0.32);   // atlanan erişilebilir hücre
    private static final Color CARPET_TINT = Color.web("#3f6f6f", 0.32);   // halı yüzeyi
    private static final Color FOG = Color.web("#070b14", 0.86);           // keşfedilmemiş alan
    private static final Color BELIEF_OBSTACLE = Color.web("#ef4444", 0.5);
    private static final Color BELIEF_DIRT = Color.web("#f59e0b", 0.85);

    private final double cell = SimConstants.CELL_SIZE;
    private final SpriteAssets sprites = new SpriteAssets();

    // Mantıksal dünyayı (cols*38 x rows*38) canvas'a sığdıran ölçek + ortalama ofseti.
    // Böylece pencere büyüdükçe saha da büyür (responsive).
    private double viewScale = 1;
    private double viewOffsetX = 0;
    private double viewOffsetY = 0;

    /** (row, col) tiklama geri cagrisi (Main tarafindan baglanir). */
    public interface CellClickHandler {
        void onCellClicked(int row, int col);
    }

    public RoomCanvas(int rows, int cols) {
        super(cols * SimConstants.CELL_SIZE, rows * SimConstants.CELL_SIZE);
    }

    /** Fare tiklama ve surukleme ile hucre duzenlemeyi etkinlestirir. */
    public void setCellClickHandler(CellClickHandler handler) {
        setOnMousePressed(e -> fire(handler, e.getX(), e.getY()));
        setOnMouseDragged(e -> fire(handler, e.getX(), e.getY()));
    }

    private void fire(CellClickHandler handler, double px, double py) {
        // Ekran pikselini -> mantıksal koordinata çevir (ölçek + ofset geri al)
        double lx = (px - viewOffsetX) / viewScale;
        double ly = (py - viewOffsetY) / viewScale;
        int col = (int) (lx / cell);
        int row = (int) (ly / cell);
        handler.onCellClicked(row, col);
    }

    /** Tanrı modu çizimi (geriye dönük uyumluluk). */
    public void render(Room room, Robot robot, boolean[][] reachable) {
        render(room, robot, reachable, false, false, false);
    }

    /**
     * Tüm sahneyi çizer. Gerçekçi modda fog-of-war (robotun bilmediği alan
     * karartılır) ve sensör ışınları katmanları eklenir.
     */
    public void render(Room room, Robot robot, boolean[][] reachable,
                       boolean realistic, boolean showBelief, boolean showRays) {
        GraphicsContext g = getGraphicsContext2D();
        g.setFill(BG);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Mantıksal dünyayı canvas'a sığdır (en boy oranını koruyarak, ortalanmış)
        double worldW = room.cols() * cell;
        double worldH = room.rows() * cell;
        double s = Math.min(getWidth() / worldW, getHeight() / worldH);
        if (!(s > 0) || Double.isInfinite(s)) {
            s = 1;
        }
        viewScale = s;
        viewOffsetX = (getWidth() - worldW * s) / 2.0;
        viewOffsetY = (getHeight() - worldH * s) / 2.0;

        g.save();
        g.translate(viewOffsetX, viewOffsetY);
        g.scale(s, s);

        drawCells(g, room, reachable);
        drawFurnitureLayer(g, room);
        drawCoverageHeatmap(g, room, robot, reachable);

        boolean hasBelief = realistic && robot != null && robot.knownMap() != null;
        if (hasBelief && showBelief) {
            drawBeliefFog(g, robot.knownMap());
        }

        drawGridLines(g, room);
        if (robot != null) {
            drawTrail(g, robot);
            if (realistic && showRays) {
                drawSensorRays(g, robot);
            }
            drawRobot(g, robot, realistic);
        }

        g.restore();
    }

    /** Robotun yalnızca keşfettiği alanı gösterir; bilinmeyeni karartır (fog-of-war). */
    private void drawBeliefFog(GraphicsContext g, KnownMap map) {
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                double x = c * cell, y = r * cell;
                KnownMap.Belief b = map.at(r, c);
                if (b == KnownMap.Belief.UNKNOWN) {
                    g.setFill(FOG);
                    g.fillRect(x, y, cell, cell);
                } else if (b == KnownMap.Belief.OBSTACLE) {
                    g.setStroke(BELIEF_OBSTACLE);
                    g.setLineWidth(1.5);
                    g.strokeRect(x + 2, y + 2, cell - 4, cell - 4);
                }
                if (map.isDirtSeen(r, c)) {
                    g.setFill(BELIEF_DIRT);
                    g.fillOval(x + cell / 2 - 3, y + cell / 2 - 3, 6, 6);
                }
            }
        }
    }

    /** Sensör ışınlarını çizer (yakın=kırmızı, uzak=yeşil; çarpma noktası işaretli). */
    private void drawSensorRays(GraphicsContext g, Robot robot) {
        SensorReading rd = robot.lastReading();
        if (rd == null || rd.rayCount() == 0) {
            return;
        }
        double cx = robot.x(), cy = robot.y();
        double radius = SimConstants.ROBOT_RADIUS;
        for (int i = 0; i < rd.rayCount(); i++) {
            double angle = rd.rayAngles()[i];
            double dist = rd.rayDistances()[i];
            double len = dist + radius;
            double ex = cx + Math.cos(angle) * len;
            double ey = cy + Math.sin(angle) * len;
            boolean hit = dist < SimConstants.SENSOR_RANGE - 1e-6;
            g.setStroke(hit ? Color.web("#ef4444", 0.5) : Color.web("#22c55e", 0.35));
            g.setLineWidth(1.5);
            g.strokeLine(cx, cy, ex, ey);
            if (hit) {
                g.setFill(Color.web("#ef4444", 0.85));
                g.fillOval(ex - 2.5, ey - 2.5, 5, 5);
            }
        }
    }

    private void drawCells(GraphicsContext g, Room room, boolean[][] reachable) {
        for (int r = 0; r < room.rows(); r++) {
            for (int c = 0; c < room.cols(); c++) {
                Cell cellModel = room.cell(r, c);
                double x = c * cell;
                double y = r * cell;
                switch (cellModel.type()) {
                    case WALL -> fillCell(g, x, y, WALL, sprites.wallTile());
                    case FURNITURE -> fillCell(g, x, y, FLOOR, sprites.floorTile());
                    case STATION -> fillCell(g, x, y, FLOOR, sprites.floorTile());
                    default -> fillCell(g, x, y, FLOOR, sprites.floorTile());
                }

                if (cellModel.isCarpet()) {
                    g.setFill(CARPET_TINT);
                    g.fillRect(x, y, cell, cell);
                }

                if (cellModel.type() == CellType.WALL) {
                    drawWallFallback(g, x, y);
                }

                if (cellModel.type() == model.CellType.STATION) {
                    drawStation(g, x, y);
                }
                if (cellModel.isDirty()) {
                    drawDirt(g, cellModel, x, y);
                    boolean unreachable = reachable != null && !reachable[r][c];
                    if (unreachable) {
                        drawUnreachableMark(g, x, y);
                    }
                }
            }
        }
    }

    private void fillCell(GraphicsContext g, double x, double y, Color fallback, Image tile) {
        if (tile != null) {
            g.setFill(new ImagePattern(tile, x, y, cell, cell, false));
        } else {
            g.setFill(fallback);
        }
        g.fillRect(x, y, cell, cell);
    }

    /**
     * Kapsama ısı haritası: çok gezilen hücreler daha parlak (gereksiz tekrar),
     * temizlik bittiğinde erişilebilir ama hiç gezilmemiş ("atlanan") hücreler
     * kırmızı ile vurgulanır — kapsama raporu gibi.
     */
    private void drawCoverageHeatmap(GraphicsContext g, Room room, Robot robot, boolean[][] reachable) {
        boolean finished = robot != null && robot.state() == model.RobotState.FINISHED;
        for (int r = 0; r < room.rows(); r++) {
            for (int c = 0; c < room.cols(); c++) {
                Cell cellModel = room.cell(r, c);
                if (!cellModel.type().isCleanable()) {
                    continue;
                }
                double x = c * cell, y = r * cell;
                int vc = cellModel.visitCount();
                if (vc == 0) {
                    if (finished && reachable != null && reachable[r][c]) {
                        g.setFill(MISSED);
                        g.fillRoundRect(x + 3, y + 3, cell - 6, cell - 6, 10, 10);
                    }
                } else {
                    double t = Math.min(1.0, vc / 40.0); // gezme yoğunluğu
                    g.setFill(Color.web("#38bdf8", 0.10 + 0.24 * t));
                    g.fillRoundRect(x + 3, y + 3, cell - 6, cell - 6, 10, 10);
                }
            }
        }
    }

    private void drawFurnitureLayer(GraphicsContext g, Room room) {
        boolean[][] covered = new boolean[room.rows()][room.cols()];

        // 1) İsimli parçalar: doğru sprite, doğru footprint
        for (FurniturePiece piece : room.furniturePieces()) {
            double x = piece.col() * cell;
            double y = piece.row() * cell;
            double w = piece.cols() * cell;
            double h = piece.rows() * cell;
            Image img = sprites.furnitureByName(piece.type().asset());
            if (img != null) {
                drawImageFit(g, img, x + 2, y + 2, w - 4, h - 4);
            } else {
                drawFurnitureFallback(g, x, y, w, h);
            }
            for (int r = piece.row(); r < piece.row() + piece.rows(); r++) {
                for (int c = piece.col(); c < piece.col() + piece.cols(); c++) {
                    if (room.inBounds(r, c)) {
                        covered[r][c] = true;
                    }
                }
            }
        }

        // 2) İsimsiz (tekil) engel hücreleri: fallback kutu
        for (int r = 0; r < room.rows(); r++) {
            for (int c = 0; c < room.cols(); c++) {
                if (room.cell(r, c).type() == CellType.FURNITURE && !covered[r][c]) {
                    drawFurnitureFallback(g, c * cell, r * cell, cell, cell);
                }
            }
        }
    }

    private void drawFurnitureFallback(GraphicsContext g, double x, double y, double width, double height) {
        g.setFill(Color.web("#271b12", 0.35));
        g.fillRoundRect(x + 6, y + 8, width - 9, height - 8, 9, 9);
        g.setFill(FURNITURE);
        g.fillRoundRect(x + 4, y + 5, width - 10, height - 12, 8, 8);
        g.setStroke(Color.web("#c08457", 0.35));
        g.setLineWidth(1.2);
        g.strokeRoundRect(x + 4, y + 5, width - 10, height - 12, 8, 8);
    }

    private void drawWallFallback(GraphicsContext g, double x, double y) {
        if (sprites.wallTile() != null) {
            return;
        }
        g.setFill(Color.web("#0b0704", 0.35));
        g.fillRect(x, y + cell - 5, cell, 5);
    }

    /** Istasyondan erisilemeyen kiri kirmizi uyari halkasiyla isaretler (bonus). */
    private void drawUnreachableMark(GraphicsContext g, double x, double y) {
        g.setStroke(Color.web("#ef4444"));
        g.setLineWidth(2);
        g.strokeOval(x + 4, y + 4, cell - 8, cell - 8);
        g.strokeLine(x + 9, y + 9, x + cell - 9, y + cell - 9);
    }

    private void drawStation(GraphicsContext g, double x, double y) {
        Image image = sprites.chargingStation();
        if (image != null) {
            drawImageFit(g, image, x + 3, y + 3, cell - 6, cell - 6);
            return;
        }

        g.setFill(STATION.deriveColor(0, 1, 0.55, 1));
        g.fillRoundRect(x + 4, y + 5, cell - 8, cell - 10, 8, 8);
        g.setFill(Color.web("#052e16"));
        g.fillRoundRect(x + 9, y + 10, cell - 18, cell - 20, 6, 6);
        g.setLineWidth(2);
        g.setStroke(Color.web("#bbf7d0"));
        // basit simsek isareti
        double cx = x + cell / 2, cy = y + cell / 2;
        g.strokeLine(cx + 3, y + 8, cx - 4, cy + 1);
        g.strokeLine(cx - 4, cy + 1, cx + 2, cy + 1);
        g.strokeLine(cx + 2, cy + 1, cx - 3, y + cell - 8);
    }

    private void drawDirt(GraphicsContext g, Cell cellModel, double x, double y) {
        DirtType dirt = cellModel.dirt();
        Image image = sprites.dirt(dirt);
        if (image != null) {
            double alpha = 1.0 - cellModel.cleaningProgress();
            g.save();
            g.setGlobalAlpha(alpha);
            drawImageFit(g, image, x + 4, y + 4, cell - 8, cell - 8);
            g.restore();
            return;
        }

        Color color = Color.web(dirt.colorHex());
        // Temizlik ilerledikce kir solar (animasyon hissi)
        double alpha = 1.0 - cellModel.cleaningProgress();
        double cx = x + cell / 2, cy = y + cell / 2;
        switch (dirt) {
            case DUST -> {
                g.setFill(color.deriveColor(0, 1, 1, alpha));
                double[][] pts = {{-7, -5}, {3, -7}, {-2, 2}, {6, 4}, {-6, 6}, {1, 8}};
                for (double[] p : pts) {
                    g.fillOval(cx + p[0], cy + p[1], 3, 3);
                }
            }
            case LIQUID -> {
                g.setFill(color.deriveColor(0, 1, 1, 0.55 * alpha));
                g.fillOval(x + 6, y + 8, cell - 12, cell - 16);
            }
            case STAIN -> {
                g.setFill(color.deriveColor(0, 1, 1, 0.5 * alpha));
                g.fillRoundRect(x + 7, y + 7, cell - 14, cell - 14, 10, 10);
            }
        }
    }

    private void drawGridLines(GraphicsContext g, Room room) {
        g.setStroke(GRID_LINE);
        g.setLineWidth(1);
        for (int c = 0; c <= room.cols(); c++) {
            double x = c * cell;
            g.strokeLine(x, 0, x, room.rows() * cell);
        }
        for (int r = 0; r <= room.rows(); r++) {
            double y = r * cell;
            g.strokeLine(0, y, room.cols() * cell, y);
        }
    }

    private void drawTrail(GraphicsContext g, Robot robot) {
        g.setStroke(TRAIL);
        g.setLineWidth(2.5);
        double[] prev = null;
        for (double[] point : robot.trail()) {
            if (prev != null) {
                g.strokeLine(prev[0], prev[1], point[0], point[1]);
            }
            prev = point;
        }
    }

    private void drawRobot(GraphicsContext g, Robot robot, boolean realistic) {
        double radius = SimConstants.ROBOT_RADIUS;
        double cx = robot.x();
        double cy = robot.y();
        Image image = sprites.robot(robot.state() == model.RobotState.CLEANING);

        // Gerçekçi modda motor zorlanması parıltısı (yük arttıkça turuncu hale büyür)
        if (realistic) {
            double load = robot.motorLoad();
            if (load > 0.01) {
                double gl = radius * (1.5 + load * 1.3);
                g.setStroke(Color.web("#f59e0b", 0.12 + 0.35 * load));
                g.setLineWidth(2 + 4 * load);
                g.strokeOval(cx - gl, cy - gl, gl * 2, gl * 2);
            }
        }

        // Yumusayarak hareket eden temas golgesi
        g.setFill(Color.web("#000000", 0.28));
        g.fillOval(cx - radius * 0.9, cy + radius * 0.55, radius * 1.8, radius * 0.5);

        if (image != null) {
            drawRotatedRobot(g, image, cx, cy, radius, robot.heading());
        } else {
            // Govde
            g.setFill(ROBOT_BODY);
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g.setStroke(ROBOT_EDGE);
            g.setLineWidth(1);
            g.strokeOval(cx - radius + 2, cy - radius + 2, radius * 2 - 4, radius * 2 - 4);

            // Yonelim gostergesi (heading)
            double hx = cx + Math.cos(robot.heading()) * radius;
            double hy = cy + Math.sin(robot.heading()) * radius;
            g.setStroke(Color.web("#0ea5e9"));
            g.setLineWidth(3);
            g.strokeLine(cx, cy, hx, hy);
        }

        // Çarpma tamponu: bir engele değince ön tarafta kırmızı yay parlar
        if (realistic) {
            SensorReading rd = robot.lastReading();
            if (robot.isContact() || (rd != null && rd.anyBump())) {
                double br = radius + 5;
                double startDeg = -Math.toDegrees(robot.heading()) - 50;
                g.setStroke(Color.web("#ef4444", 0.9));
                g.setLineWidth(4);
                g.strokeArc(cx - br, cy - br, br * 2, br * 2, startDeg, 100,
                        javafx.scene.shape.ArcType.OPEN);
            }
        }

        // Batarya durumuna gore renkli halka (yesil -> sari -> kirmizi)
        Color ring = batteryColor(robot.battery());
        g.setStroke(ring);
        g.setLineWidth(3);
        g.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
        drawBatteryBar(g, robot, cx, cy, radius, ring);

        // Durum etiketi (kucuk yazi)
        g.setFill(Color.web("#e2e8f0"));
        g.fillText(robot.state().label(), cx + radius + 4, cy - radius);
    }

    private Color batteryColor(double battery) {
        if (battery > 50) return Color.web("#22c55e");
        if (battery > SimConstants.LOW_BATTERY_THRESHOLD) return Color.web("#eab308");
        return Color.web("#ef4444");
    }

    private void drawRotatedRobot(GraphicsContext g, Image image, double cx, double cy, double radius, double heading) {
        double size = radius * 2.25;
        g.save();
        g.translate(cx, cy);
        // Asset spec says robot faces north; Canvas heading 0 points east.
        g.rotate(Math.toDegrees(heading) + 90.0);
        g.drawImage(image, -size / 2, -size / 2, size, size);
        g.restore();
    }

    private void drawBatteryBar(GraphicsContext g, Robot robot, double cx, double cy, double radius, Color ring) {
        double width = radius * 2.05;
        double height = 4.0;
        double x = cx - width / 2;
        double y = cy + radius + 5;
        double fill = width * Math.max(0.0, Math.min(1.0, robot.battery() / 100.0));

        g.setFill(Color.web("#020617", 0.75));
        g.fillRoundRect(x, y, width, height, height, height);
        g.setFill(ring);
        g.fillRoundRect(x, y, fill, height, height, height);
    }

    private void drawImageFit(GraphicsContext g, Image image, double x, double y, double width, double height) {
        double iw = image.getWidth();
        double ih = image.getHeight();
        if (iw <= 0 || ih <= 0) {
            return;
        }
        double scale = Math.min(width / iw, height / ih);
        double dw = iw * scale;
        double dh = ih * scale;
        g.drawImage(image, x + (width - dw) / 2, y + (height - dh) / 2, dw, dh);
    }
}
