package controller.algorithm;

import model.AlgorithmType;

/**
 * {@link AlgorithmType} enum'undan ilgili {@link CleaningStrategy} ornegini
 * uretir. UI'daki algoritma secimi ile controller'i birbirine baglar.
 */
public final class StrategyFactory {

    private StrategyFactory() { }

    public static CleaningStrategy create(AlgorithmType type) {
        return switch (type) {
            case RANDOM -> new RandomStrategy();
            case SPIRAL -> new SpiralStrategy();
            case WALL_FOLLOW -> new WallFollowStrategy();
            case SMART -> new SmartStrategy();
        };
    }
}
