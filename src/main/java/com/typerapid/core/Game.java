package com.typerapid.core;

import com.typerapid.words.WordSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * The UI-agnostic core of TypeRapid, modelling the Typ'O'Shooter loop.
 *
 * <p>Enemies spawn at the far edge carrying words from a {@link WordSet}, drift
 * toward the player, and are destroyed by typing their words. The player loses a
 * life each time an enemy reaches them; the game ends when lives run out.
 *
 * <p>The class performs no input or output. A driver advances simulated time with
 * {@link #update(double)} and feeds characters with {@link #type(char)}; rendering
 * reads {@link #enemies()}, {@link #stats()} and {@link #state()}. Spawning is
 * driven by an injectable {@link Random}, so a seeded instance makes runs fully
 * deterministic and testable.
 */
public final class Game {

    private final GameConfig config;
    private final WordSet wordSet;
    private final Random rng;

    private final List<Enemy> enemies = new ArrayList<>();
    private final Stats stats = new Stats();

    private GameState state = GameState.RUNNING;
    private int livesRemaining;
    private double spawnAccumulator;
    private Enemy target;

    /** Creates a game with a random seed (non-deterministic spawns). */
    public Game(GameConfig config, WordSet wordSet) {
        this(config, wordSet, new Random());
    }

    public Game(GameConfig config, WordSet wordSet, Random rng) {
        this.config = Objects.requireNonNull(config, "config");
        this.wordSet = Objects.requireNonNull(wordSet, "wordSet");
        this.rng = Objects.requireNonNull(rng, "rng");
        this.livesRemaining = config.lives();
    }

    /**
     * Advances the simulation by {@code dt} seconds: accrues time, spawns due
     * enemies, moves all enemies forward, and applies life loss for any that
     * reach the player. No-op once the game is over.
     */
    public void update(double dt) {
        if (state != GameState.RUNNING) {
            return;
        }
        stats.addTime(dt);

        spawnAccumulator += dt;
        while (spawnAccumulator >= config.spawnInterval()) {
            spawnAccumulator -= config.spawnInterval();
            spawnEnemy();
        }

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            enemy.advance(dt);
            if (enemy.reachedPlayer()) {
                it.remove();
                if (enemy == target) {
                    target = null;
                }
                loseLife();
            }
        }
    }

    /**
     * Feeds one typed character to the game.
     *
     * <p>With no locked target, the character locks onto the closest enemy whose
     * word starts with it. With a target locked, the character must match the
     * target's next letter; a match advances (and may destroy) it, a mismatch is
     * recorded as an error and leaves the lock intact.
     *
     * @return the outcome; see {@link TypeResult}
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
            enemies.remove(target);
            stats.recordKill();
            target = null;
            return TypeResult.KILL;
        }
        return TypeResult.HIT;
    }

    /** Closest untargeted enemy whose word begins with {@code c}, or null. */
    private Enemy acquireTarget(char c) {
        return enemies.stream()
                .filter(e -> e.matched() == 0 && e.word().charAt(0) == c)
                .min(Comparator.comparingDouble(Enemy::distance))
                .orElse(null);
    }

    private void spawnEnemy() {
        List<String> words = wordSet.words();
        String word = words.get(rng.nextInt(words.size()));
        double lane = rng.nextDouble(); // random lateral position, for rendering
        enemies.add(new Enemy(word, 1.0, lane, config.enemySpeed()));
    }

    private void loseLife() {
        livesRemaining--;
        if (livesRemaining <= 0) {
            state = GameState.GAME_OVER;
        }
    }

    /** The currently locked target, or {@code null} if none. */
    public Enemy target() {
        return target;
    }

    public GameState state() {
        return state;
    }

    /** The configuration this game was created with (spawn rate, speed, lives). */
    public GameConfig config() {
        return config;
    }

    /** The word-set this game draws enemy words from. */
    public WordSet wordSet() {
        return wordSet;
    }

    public int livesRemaining() {
        return livesRemaining;
    }

    /** Live, read-only view of the enemies currently in play. */
    public List<Enemy> enemies() {
        return Collections.unmodifiableList(enemies);
    }

    public Stats stats() {
        return stats;
    }
}
