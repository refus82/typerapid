package com.typerapid.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typerapid.words.WordSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GameTest {

    /** A deterministic single-word set so spawns are fully predictable. */
    private static WordSet singleWord(String word) {
        return new WordSet() {
            @Override
            public String id() {
                return "test-" + word;
            }

            @Override
            public String displayName() {
                return "Test " + word;
            }

            @Override
            public List<String> words() {
                return List.of(word);
            }
        };
    }

    private static void type(Game game, String text) {
        text.chars().forEach(c -> game.type((char) c));
    }

    @Test
    void enemySpawnsAfterSpawnInterval() {
        Game game = new Game(new GameConfig(1.0, 0.01, 3), singleWord("cat"), new Random(1));
        assertTrue(game.enemies().isEmpty());

        game.update(0.5);
        assertTrue(game.enemies().isEmpty(), "no spawn before the interval elapses");

        game.update(0.6);
        assertEquals(1, game.enemies().size(), "one spawn once the interval is crossed");
    }

    @Test
    void typingFullWordKillsEnemyAndScores() {
        Game game = new Game(new GameConfig(1.0, 0.001, 3), singleWord("cat"), new Random(1));
        game.update(1.0);

        assertSame(TypeResult.HIT, game.type('c'));
        assertSame(TypeResult.HIT, game.type('a'));
        assertSame(TypeResult.KILL, game.type('t'));

        assertTrue(game.enemies().isEmpty());
        assertEquals(1, game.stats().kills());
        assertEquals(3, game.stats().correct());
        assertEquals(0, game.stats().errors());
    }

    @Test
    void wrongCharIsAnErrorAndKeepsTargetLocked() {
        Game game = new Game(new GameConfig(1.0, 0.001, 3), singleWord("cat"), new Random(1));
        game.update(1.0);

        assertSame(TypeResult.HIT, game.type('c'));
        assertSame(TypeResult.MISS, game.type('x')); // wrong, lock retained
        assertSame(TypeResult.HIT, game.type('a'));  // resume where we left off
        assertSame(TypeResult.KILL, game.type('t'));

        assertEquals(1, game.stats().errors());
        assertEquals(3, game.stats().correct());
    }

    @Test
    void spawnedEnemiesGetALaneInUnitRange() {
        Game game = new Game(new GameConfig(0.5, 0.001, 3), singleWord("cat"), new Random(1));
        game.update(2.0); // several spawns
        assertFalse(game.enemies().isEmpty());
        for (var enemy : game.enemies()) {
            double lane = enemy.lane();
            assertTrue(lane >= 0.0 && lane < 1.0, "lane out of range: " + lane);
        }
    }

    @Test
    void characterWithNoMatchingEnemyIsAMiss() {
        Game game = new Game(new GameConfig(1.0, 0.001, 3), singleWord("cat"), new Random(1));
        game.update(1.0);

        assertSame(TypeResult.MISS, game.type('z'));
        assertEquals(1, game.stats().errors());
    }

    @Test
    void enemyReachingPlayerCostsALifeAndEndsGameAtZero() {
        // speed 1.0/s, spawn each second; a single update of 3s lands the spawn
        // at the player and drains the one remaining... use 1 life for clarity.
        Game game = new Game(new GameConfig(1.0, 1.0, 1), singleWord("cat"), new Random(1));
        game.update(1.0); // spawn at distance 1.0, then advanced by full dt
        // After 1s at speed 1.0 the enemy is already at distance 0 -> reached.
        assertEquals(GameState.GAME_OVER, game.state());
        assertEquals(0, game.livesRemaining());
    }

    @Test
    void inputIsIgnoredAfterGameOver() {
        Game game = new Game(new GameConfig(1.0, 1.0, 1), singleWord("cat"), new Random(1));
        game.update(1.0);
        assertEquals(GameState.GAME_OVER, game.state());
        assertSame(TypeResult.IGNORED, game.type('c'));
    }

    @Test
    void wpmAndAccuracyReflectTypedCharacters() {
        Game game = new Game(new GameConfig(1.0, 0.001, 3), singleWord("hello"), new Random(1));
        game.update(1.0);   // 1 second elapsed
        game.update(59.0);  // total 60 seconds elapsed

        type(game, "hello"); // 5 correct chars in 60s -> 1 WPM
        assertEquals(1.0, game.stats().wpm(), 1e-9);
        assertEquals(1.0, game.stats().accuracy(), 1e-9);

        assertSame(TypeResult.MISS, game.type('z')); // 1 error -> 5/6 accuracy
        assertEquals(5.0 / 6.0, game.stats().accuracy(), 1e-9);
        assertFalse(game.enemies().isEmpty() && game.stats().kills() == 0);
    }
}
