package com.typerapid.core;

/**
 * Tunable parameters for a game session.
 *
 * <p>Distances are normalised: an enemy spawns at {@code 1.0} (the far edge) and
 * reaches the player at {@code 0.0}. {@code enemySpeed} is therefore expressed in
 * fraction-of-arena per second.
 *
 * @param spawnInterval seconds between enemy spawns (must be &gt; 0)
 * @param enemySpeed    distance covered per second, in [0, 1] units (must be &gt; 0)
 * @param lives         hits the player can take before the game ends (must be &gt; 0)
 */
public record GameConfig(double spawnInterval, double enemySpeed, int lives) {

    public GameConfig {
        if (spawnInterval <= 0) {
            throw new IllegalArgumentException("spawnInterval must be > 0");
        }
        if (enemySpeed <= 0) {
            throw new IllegalArgumentException("enemySpeed must be > 0");
        }
        if (lives <= 0) {
            throw new IllegalArgumentException("lives must be > 0");
        }
    }

    /** Sensible starting difficulty: a spawn every 2s, ~20s to cross, 3 lives. */
    public static GameConfig defaults() {
        return new GameConfig(2.0, 0.05, 3);
    }
}
