package view;

import controller.SimulationManager;
import model.RobotState;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.Random;

/**
 * Ses efektleri — <b>dosyasız ve üçüncü partisiz</b>. Tüm sesler {@code javax.sound.sampled}
 * (Java SE) ile çalışma anında sentezlenir: temizlerken sürekli vınlama (loop),
 * şarjda bip, çarpmada tık, bitişte melodi.
 * <p>
 * Tüm çağrılar best-effort'tur (ses donanımı yoksa sessizce devre dışı kalır);
 * yalnızca view katmanından kullanılır.
 */
public class SoundManager {

    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);
    private static final long CLICK_THROTTLE_MS = 140;

    private Clip hum;
    private Clip beep;
    private Clip click;
    private Clip chime;
    private boolean available;

    private boolean humPlaying;
    private RobotState lastState = RobotState.IDLE;
    private boolean lastBump;
    private long lastClickAt;

    public SoundManager() {
        try {
            hum = clipFrom(humBuffer());
            beep = clipFrom(tone(0.18, 880, 0.4, true));
            click = clipFrom(noise(0.05, 0.5));
            chime = clipFrom(chimeBuffer());
            available = true;
        } catch (Exception ignored) {
            available = false; // ses donanımı yok -> sessiz
        }
    }

    /** Her karede çağrılır; simülasyon durumuna göre sesleri tetikler. */
    public void update(SimulationManager sim, boolean enabled) {
        if (!available) {
            return;
        }
        if (!enabled) {
            stopHum();
            lastState = sim.robot().state();
            return;
        }
        RobotState state = sim.robot().state();

        // Vınlama: temizlerken ve çalışırken sürekli
        boolean shouldHum = sim.isRunning() && state == RobotState.CLEANING;
        if (shouldHum && !humPlaying) {
            startHum();
        } else if (!shouldHum && humPlaying) {
            stopHum();
        }

        // Durum geçişleri
        if (state != lastState) {
            if (state == RobotState.CHARGING) {
                play(beep);
            } else if (state == RobotState.FINISHED) {
                play(chime);
            }
            lastState = state;
        }

        // Çarpma tıkı (gerçek temas, yükselen kenar + throttle)
        boolean bump = sim.robot().isContact() && sim.mode() == model.SimulationMode.REALISTIC;
        if (bump && !lastBump) {
            long now = System.currentTimeMillis();
            if (now - lastClickAt > CLICK_THROTTLE_MS) {
                play(click);
                lastClickAt = now;
            }
        }
        lastBump = bump;
    }

    public void shutdown() {
        stopHum();
    }

    // --- iç ---

    private void startHum() {
        try {
            hum.setFramePosition(0);
            hum.loop(Clip.LOOP_CONTINUOUSLY);
            humPlaying = true;
        } catch (Exception ignored) { }
    }

    private void stopHum() {
        try {
            if (hum != null) {
                hum.stop();
            }
        } catch (Exception ignored) { }
        humPlaying = false;
    }

    private void play(Clip clip) {
        try {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception ignored) { }
    }

    private static Clip clipFrom(byte[] pcm) throws Exception {
        Clip clip = AudioSystem.getClip();
        clip.open(FORMAT, pcm, 0, pcm.length);
        return clip;
    }

    /** Saf sinüs ton (10 ms attack/release zarfı ile). */
    private static byte[] tone(double seconds, double freq, double amp, boolean fade) {
        int sr = 44100;
        int n = (int) (seconds * sr);
        byte[] b = new byte[n * 2];
        int edge = (int) (0.01 * sr);
        for (int i = 0; i < n; i++) {
            double env = fade ? Math.min(1.0, Math.min(i, n - i) / (double) edge) : 1.0;
            double s = Math.sin(2 * Math.PI * freq * i / sr) * amp * env;
            writeSample(b, i, s);
        }
        return b;
    }

    private static byte[] noise(double seconds, double amp) {
        int sr = 44100;
        int n = (int) (seconds * sr);
        byte[] b = new byte[n * 2];
        Random rnd = new Random(13);
        for (int i = 0; i < n; i++) {
            double env = 1.0 - (double) i / n; // hızlı sönüm -> "tık"
            writeSample(b, i, (rnd.nextDouble() * 2 - 1) * amp * env);
        }
        return b;
    }

    /** Vınlama: 90 Hz + harmonik + hafif gürültü, dikişsiz loop (tam çevrim sayısı). */
    private static byte[] humBuffer() {
        int sr = 44100;
        double dur = 0.4; // 90Hz*0.4 = 36 tam çevrim -> dikişsiz
        int n = (int) (dur * sr);
        byte[] b = new byte[n * 2];
        Random rnd = new Random(7);
        for (int i = 0; i < n; i++) {
            double t = (double) i / sr;
            double s = 0.55 * Math.sin(2 * Math.PI * 90 * t)
                    + 0.22 * Math.sin(2 * Math.PI * 180 * t)
                    + 0.05 * (rnd.nextDouble() * 2 - 1);
            writeSample(b, i, s * 0.32);
        }
        return b;
    }

    /** Bitiş melodisi: 660 Hz -> 990 Hz iki nota. */
    private static byte[] chimeBuffer() {
        byte[] a = tone(0.18, 660, 0.4, true);
        byte[] c = tone(0.22, 990, 0.4, true);
        byte[] out = new byte[a.length + c.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(c, 0, out, a.length, c.length);
        return out;
    }

    private static void writeSample(byte[] b, int i, double s) {
        s = Math.max(-1.0, Math.min(1.0, s));
        short v = (short) (s * 32767);
        b[2 * i] = (byte) (v & 0xff);
        b[2 * i + 1] = (byte) ((v >> 8) & 0xff);
    }
}
