package com.typerapid.core;

/**
 * Tunable parameters for an Invaders-mode session.
 *
 * <p>The playfield is a grid of integer column-slots and rows. A wave is a block
 * of {@code formationColumns x formationRows} enemies that marches sideways within
 * {@code arenaColumns}; at a wall it drops one row and reverses. An enemy that
 * reaches {@code playerRow} costs a life.
 *
 * @param arenaColumns     total width of the playfield in column-slots
 * @param formationColumns enemies per row in a wave (must fit in the arena)
 * @param formationRows    rows of enemies per wave
 * @param playerRow        row index that counts as reaching the player
 * @param stepInterval     seconds between formation marches (must be &gt; 0)
 * @param lives            breaches the player can take before game over
 */
public record InvaderConfig(
        int arenaColumns,
        int formationColumns,
        int formationRows,
        int playerRow,
        double stepInterval,
        int lives) {

    public InvaderConfig {
        if (formationColumns <= 0 || formationRows <= 0) {
            throw new IllegalArgumentException("formation must be at least 1x1");
        }
        if (arenaColumns < formationColumns) {
            throw new IllegalArgumentException("arenaColumns must be >= formationColumns");
        }
        if (playerRow <= formationRows) {
            throw new IllegalArgumentException("playerRow must leave room for the formation");
        }
        if (stepInterval <= 0) {
            throw new IllegalArgumentException("stepInterval must be > 0");
        }
        if (lives <= 0) {
            throw new IllegalArgumentException("lives must be > 0");
        }
    }

    /** Terminal-friendly default: 8-wide arena, 5x3 wave, march every 0.8s, 3 lives. */
    public static InvaderConfig defaults() {
        return new InvaderConfig(8, 5, 3, 12, 0.8, 3);
    }

    /** Leftmost formation offset is 0; rightmost keeps the block in bounds. */
    public int maxFormationCol() {
        return arenaColumns - formationColumns;
    }
}
