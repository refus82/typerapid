package com.typerapid.core;

import com.typerapid.words.WordSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Invaders-mode core: a Space-Invaders-style take on Typ'O'Shooter. A wave of
 * word-carrying enemies marches sideways in formation; at a wall it drops a row
 * and reverses. The player is fixed at the bottom and destroys enemies by typing
 * their words. An enemy that reaches the player's row breaches and costs a life;
 * the game ends at zero lives. Clearing a wave spawns a faster one.
 *
 * <p>Like {@link Game}, this is headless and deterministic: {@link #update(double)}
 * advances time, {@link #type(char)} feeds a keystroke, and spawning uses an
 * injectable {@link Random}. It performs no I/O.
 */
public final class InvaderGame {

    /** Each cleared wave multiplies the march interval by this (gets faster). */
    private static final double WAVE_SPEEDUP = 0.85;
    private static final double MIN_STEP_INTERVAL = 0.15;

    private final InvaderConfig config;
    private final WordSet wordSet;
    private final Random rng;

    private final List<GridEnemy> enemies = new ArrayList<>();
    private final Stats stats = new Stats();

    private GameState state = GameState.RUNNING;
    private int livesRemaining;
    private int wave;
    private double stepInterval;
    private double stepAccumulator;
    private int formationCol;
    private int formationRow;
    private int direction = 1;
    private GridEnemy target;

    public InvaderGame(InvaderConfig config, WordSet wordSet) {
        this(config, wordSet, new Random());
    }

    public InvaderGame(InvaderConfig config, WordSet wordSet, Random rng) {
        this.config = Objects.requireNonNull(config, "config");
        this.wordSet = Objects.requireNonNull(wordSet, "wordSet");
        this.rng = Objects.requireNonNull(rng, "rng");
        this.livesRemaining = config.lives();
        this.stepInterval = config.stepInterval();
        spawnWave();
    }

    /** Advances time, marching the formation on each {@code stepInterval} boundary. */
    public void update(double dt) {
        if (state != GameState.RUNNING) {
            return;
        }
        stats.addTime(dt);
        stepAccumulator += dt;
        while (stepAccumulator >= stepInterval && state == GameState.RUNNING) {
            stepAccumulator -= stepInterval;
            step();
        }
    }

    /**
     * Feeds one typed character. With no locked target, locks onto the enemy
     * nearest the player whose word starts with the character; with a target
     * locked, the character must match its next letter (mismatch = error, lock
     * kept; completing the word destroys it and may trigger the next wave).
     */
    public TypeResult type(char c) {
        if (state != GameState.RUNNING) {
            return TypeResult.IGNORED;
        }
        stats.recordKeystroke();

        if (target == null) {
            target = acquireTarget(c);
            if (target == null) {
                return TypeResult.MISS;
            }
        } else if (target.nextChar() != c) {
            return TypeResult.MISS;
        }

        target.registerHit();
        stats.recordCorrect();

        if (target.isCleared()) {
            target.kill();
            stats.recordKill();
            target = null;
            if (waveCleared()) {
                startNextWave();
            }
            return TypeResult.KILL;
        }
        return TypeResult.HIT;
    }

    /** Closest-to-player untargeted enemy whose word begins with {@code c}. */
    private GridEnemy acquireTarget(char c) {
        return enemies.stream()
                .filter(e -> e.alive() && e.matched() == 0 && e.word().charAt(0) == c)
                .max(Comparator.comparingInt(this::absRow).thenComparing(e -> -absCol(e)))
                .orElse(null);
    }

    private void step() {
        int next = formationCol + direction;
        if (next < 0 || next > config.maxFormationCol()) {
            formationRow++;          // hit a wall: drop and reverse
            direction = -direction;
        } else {
            formationCol = next;
        }
        applyBreaches();
        if (state == GameState.RUNNING && waveCleared()) {
            startNextWave();
        }
    }

    /** Any enemy that has reached the player's row breaches: life lost, removed. */
    private void applyBreaches() {
        for (GridEnemy enemy : enemies) {
            if (enemy.alive() && absRow(enemy) >= config.playerRow()) {
                enemy.kill();
                if (enemy == target) {
                    target = null;
                }
                livesRemaining--;
                if (livesRemaining <= 0) {
                    livesRemaining = 0;
                    state = GameState.GAME_OVER;
                }
            }
        }
    }

    private boolean waveCleared() {
        return enemies.stream().noneMatch(GridEnemy::alive);
    }

    private void startNextWave() {
        stepInterval = Math.max(MIN_STEP_INTERVAL, stepInterval * WAVE_SPEEDUP);
        spawnWave();
    }

    private void spawnWave() {
        enemies.clear();
        formationCol = 0;
        formationRow = 0;
        direction = 1;
        target = null;
        wave++;
        List<String> words = wordSet.words();
        for (int row = 0; row < config.formationRows(); row++) {
            for (int col = 0; col < config.formationColumns(); col++) {
                String word = words.get(rng.nextInt(words.size()));
                enemies.add(new GridEnemy(word, col, row));
            }
        }
    }

    /** Absolute column-slot of an enemy = formation offset + its base column. */
    public int absCol(GridEnemy enemy) {
        return formationCol + enemy.baseCol();
    }

    /** Absolute row of an enemy = formation drop + its base row. */
    public int absRow(GridEnemy enemy) {
        return formationRow + enemy.baseRow();
    }

    /** Live, read-only view of enemies still alive in the current wave. */
    public List<GridEnemy> enemies() {
        return enemies.stream().filter(GridEnemy::alive).toList();
    }

    /** The currently locked target, or {@code null} if none. */
    public GridEnemy target() {
        return target;
    }

    public GameState state() {
        return state;
    }

    public int livesRemaining() {
        return livesRemaining;
    }

    public int wave() {
        return wave;
    }

    public InvaderConfig config() {
        return config;
    }

    /** The word-set this game draws enemy words from. */
    public WordSet wordSet() {
        return wordSet;
    }

    public Stats stats() {
        return stats;
    }
}
