package controller;

import model.Room;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Grid uzerinde en kisa yol bulma.
 * <p>
 * Sarj istasyonuna donus icin <b>BFS</b> kullanilir: agirliksiz grid'de BFS,
 * adim sayisi olarak en kisa yolu garanti eder. (A* da eklenebilir; engelsiz
 * uniform maliyetli bu problemde BFS optimal ve sadedir.)
 * <p>
 * Sonuc, baslangic hucresi <i>haric</i>, hedef hucresi <i>dahil</i> izlenecek
 * {@code [row, col]} adimlarinin listesidir. Hedef ulasilamazsa bos liste doner.
 */
public final class PathFinder {

    private PathFinder() { }

    public static List<int[]> shortestPath(Room room, int startRow, int startCol,
                                           int goalRow, int goalCol) {
        if (!room.isWalkable(startRow, startCol) || !room.isWalkable(goalRow, goalCol)) {
            return Collections.emptyList();
        }

        int rows = room.rows();
        int cols = room.cols();
        boolean[][] visited = new boolean[rows][cols];
        int[][] parentRow = new int[rows][cols];
        int[][] parentCol = new int[rows][cols];

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;

        final int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            if (r == goalRow && c == goalCol) {
                return reconstruct(parentRow, parentCol, startRow, startCol, goalRow, goalCol);
            }
            for (int[] m : moves) {
                int nr = r + m[0];
                int nc = c + m[1];
                if (room.isWalkable(nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentRow[nr][nc] = r;
                    parentCol[nr][nc] = c;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return Collections.emptyList(); // ulasilamaz
    }

    private static List<int[]> reconstruct(int[][] parentRow, int[][] parentCol,
                                           int startRow, int startCol,
                                           int goalRow, int goalCol) {
        List<int[]> path = new ArrayList<>();
        int r = goalRow, c = goalCol;
        while (!(r == startRow && c == startCol)) {
            path.add(new int[]{r, c});
            int pr = parentRow[r][c];
            int pc = parentCol[r][c];
            r = pr;
            c = pc;
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * A* ile en kisa yol (Manhattan sezgiseli). Uniform maliyetli bu grid'de
     * BFS ile ayni sonucu verir; oncelik kuyrugu sayesinde hedefe dogru daha
     * az hucre acarak ulasir. Rapordaki algoritma karsilastirmasi icin BFS'in
     * yaninda sunulur.
     */
    public static List<int[]> aStar(Room room, int startRow, int startCol,
                                    int goalRow, int goalCol) {
        if (!room.isWalkable(startRow, startCol) || !room.isWalkable(goalRow, goalCol)) {
            return Collections.emptyList();
        }
        int rows = room.rows();
        int cols = room.cols();
        int[][] gScore = new int[rows][cols];
        boolean[][] closed = new boolean[rows][cols];
        int[][] parentRow = new int[rows][cols];
        int[][] parentCol = new int[rows][cols];
        for (int[] row : gScore) {
            java.util.Arrays.fill(row, Integer.MAX_VALUE);
        }

        PriorityQueue<int[]> open = new PriorityQueue<>(Comparator.comparingInt(n -> n[2]));
        gScore[startRow][startCol] = 0;
        open.add(new int[]{startRow, startCol, heuristic(startRow, startCol, goalRow, goalCol)});

        final int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!open.isEmpty()) {
            int[] cur = open.poll();
            int r = cur[0], c = cur[1];
            if (closed[r][c]) {
                continue;
            }
            closed[r][c] = true;
            if (r == goalRow && c == goalCol) {
                return reconstruct(parentRow, parentCol, startRow, startCol, goalRow, goalCol);
            }
            for (int[] m : moves) {
                int nr = r + m[0];
                int nc = c + m[1];
                if (!room.isWalkable(nr, nc) || closed[nr][nc]) {
                    continue;
                }
                int tentative = gScore[r][c] + 1;
                if (tentative < gScore[nr][nc]) {
                    gScore[nr][nc] = tentative;
                    parentRow[nr][nc] = r;
                    parentCol[nr][nc] = c;
                    open.add(new int[]{nr, nc, tentative + heuristic(nr, nc, goalRow, goalCol)});
                }
            }
        }
        return Collections.emptyList();
    }

    private static int heuristic(int r, int c, int gr, int gc) {
        return Math.abs(r - gr) + Math.abs(c - gc);
    }

    /**
     * Baslangic hucresinden yuruyerek erisilebilen tum hucreleri isaretler
     * (flood-fill). "Ulasilamaz alan tespiti" bonusunun temelidir.
     */
    public static boolean[][] reachable(Room room, int startRow, int startCol) {
        boolean[][] seen = new boolean[room.rows()][room.cols()];
        if (!room.isWalkable(startRow, startCol)) {
            return seen;
        }
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        seen[startRow][startCol] = true;
        final int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] m : moves) {
                int nr = cur[0] + m[0];
                int nc = cur[1] + m[1];
                if (room.isWalkable(nr, nc) && !seen[nr][nc]) {
                    seen[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return seen;
    }

    /**
     * Baslangictan en yakin <b>kirli</b> hucreye giden yol (BFS, en az adim).
     * "Akilli" temizlik stratejisi bunu kullanir. Erisilebilir kir yoksa bos doner.
     */
    public static List<int[]> pathToNearestDirt(Room room, int startRow, int startCol) {
        if (!room.isWalkable(startRow, startCol)) {
            return Collections.emptyList();
        }
        int rows = room.rows();
        int cols = room.cols();
        boolean[][] visited = new boolean[rows][cols];
        int[][] parentRow = new int[rows][cols];
        int[][] parentCol = new int[rows][cols];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        final int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            if (room.cell(r, c).isDirty() && !(r == startRow && c == startCol)) {
                return reconstruct(parentRow, parentCol, startRow, startCol, r, c);
            }
            for (int[] m : moves) {
                int nr = r + m[0];
                int nc = c + m[1];
                if (room.isWalkable(nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentRow[nr][nc] = r;
                    parentCol[nr][nc] = c;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Baslangictan en yakin <b>henuz temizlenmemis</b> hucreye giden yol (BFS):
     * hedef, kirli VEYA daha once ziyaret edilmemis yuruyebilir bir hucredir.
     * <p>
     * Kapsama algoritmalari (Spiral/Duvar-Takip/Rastgele) yerel olarak sikistiginda
     * (tum komsular ziyaretli) bu "kacis" yolu ile en yakin yeni alana giderek
     * sonsuz donguden cikar ve tam kapsamayi/bitisi garanti eder.
     */
    public static List<int[]> pathToNearestUncleaned(Room room, int startRow, int startCol) {
        if (!room.isWalkable(startRow, startCol)) {
            return Collections.emptyList();
        }
        int rows = room.rows();
        int cols = room.cols();
        boolean[][] visited = new boolean[rows][cols];
        int[][] parentRow = new int[rows][cols];
        int[][] parentCol = new int[rows][cols];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        final int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            boolean isTarget = room.cell(r, c).isDirty() || !room.cell(r, c).isVisited();
            if (isTarget && !(r == startRow && c == startCol)) {
                return reconstruct(parentRow, parentCol, startRow, startCol, r, c);
            }
            for (int[] m : moves) {
                int nr = r + m[0];
                int nc = c + m[1];
                if (room.isWalkable(nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentRow[nr][nc] = r;
                    parentCol[nr][nc] = c;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Bir hucrenin istasyondan ulasilabilir olup olmadigini soyler
     * ("ulasilamaz alan tespiti" bonusu icin temel).
     */
    public static boolean isReachable(Room room, int startRow, int startCol,
                                      int goalRow, int goalCol) {
        return !shortestPath(room, startRow, startCol, goalRow, goalCol).isEmpty()
                || (startRow == goalRow && startCol == goalCol);
    }
}
