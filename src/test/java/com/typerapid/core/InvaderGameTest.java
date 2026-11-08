package com.typerapid.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typerapid.words.WordSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class InvaderGameTest {

    /** Single-word set so every spawned enemy carries a predictable word. */
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

    private static TypeResult typeWord(InvaderGame game, String word) {
        TypeResult last = TypeResult.MISS;
        for (int i = 0; i < word.length(); i++) {
            last = game.type(word.charAt(i));
        }
        return last;
    }

    @Test
    void firstWaveSpawnsAtConstruction() {
        InvaderConfig cfg = new InvaderConfig(6, 2, 2, 10, 1.0, 3);
        InvaderGame game = new InvaderGame(cfg, singleWord("cat"), new Random(1));

        assertEquals(4, game.enemies().size()); // 2x2 formation
        assertEquals(1, game.wave());
    }

    @Test
    void typingAWordDestroysOneEnemy() {
        InvaderConfig cfg = new InvaderConfig(6, 2, 2, 10, 100.0, 3);
        InvaderGame game = new InvaderGame(cfg, singleWord("cat"), new Random(1));

        assertSame(TypeResult.KILL, typeWord(game, "cat"));
        assertEquals(3, game.enemies().size());
        assertEquals(1, game.stats().kills());
    }

    @Test
    void clearingTheWaveSpawnsTheNext() {
        InvaderConfig cfg = new InvaderConfig(6, 2, 2, 10, 100.0, 3);
        InvaderGame game = new InvaderGame(cfg, singleWord("cat"), new Random(1));

        for (int i = 0; i < 4; i++) {
            typeWord(game, "cat");
        }
        assertEquals(2, game.wave());
        assertEquals(4, game.enemies().size()); // fresh wave
        assertEquals(4, game.stats().kills());
    }

    @Test
    void formationMarchesAcrossThenDropsAndReverses() {
        // arena width 3, single column -> can occupy cols 0,1,2 before turning.
        InvaderConfig cfg = new InvaderConfig(3, 1, 1, 50, 1.0, 3);
        InvaderGame game = new InvaderGame(cfg, singleWord("cat"), new Random(1));
        GridEnemy e = game.enemies().get(0);

        assertEquals(0, game.absCol(e));
        assertEquals(0, game.absRow(e));

        game.update(1.0); // -> col 1
        assertEquals(1, game.absCol(e));
        game.update(1.0); // -> col 2 (right wall reached)
        assertEquals(2, game.absCol(e));
        game.update(1.0); // wall: drop a row, reverse, stay at col 2
        assertEquals(2, game.absCol(e));
        assertEquals(1, game.absRow(e));
        game.update(1.0); // -> col 1, now heading left
        assertEquals(1, game.absCol(e));
    }

    @Test
    void enemyReachingPlayerRowCostsLifeAndEndsGame() {
        // 1-wide arena forces a drop every step; playerRow 2, single life.
        InvaderConfig cfg = new InvaderConfig(1, 1, 1, 2, 1.0, 1);
        InvaderGame game = new InvaderGame(cfg, singleWord("cat"), new Random(1));

        game.update(1.0); // drop to row 1
        assertSame(GameState.RUNNING, game.state());
        game.update(1.0); // drop to row 2 -> breach
        assertSame(GameState.GAME_OVER, game.state());
        assertEquals(0, game.livesRemaining());
    }

    @Test
    void inputIgnoredAfterGameOver() {
        InvaderConfig cfg = new InvaderConfig(1, 1, 1, 2, 1.0, 1);
        InvaderGame game = new InvaderGame(cfg, singleWord("cat"), new Random(1));
        game.update(1.0);
        game.update(1.0);
        assertSame(GameState.GAME_OVER, game.state());
        assertSame(TypeResult.IGNORED, game.type('c'));
    }

    @Test
    void mismatchKeepsLockAndCountsError() {
        InvaderConfig cfg = new InvaderConfig(6, 1, 1, 10, 100.0, 3);
        InvaderGame game = new InvaderGame(cfg, singleWord("cat"), new Random(1));

        assertSame(TypeResult.HIT, game.type('c'));
        assertSame(TypeResult.MISS, game.type('x')); // wrong, lock retained
        assertSame(TypeResult.HIT, game.type('a'));
        assertSame(TypeResult.KILL, game.type('t'));
        assertEquals(1, game.stats().errors());
        assertTrue(game.enemies().isEmpty() || game.wave() == 2);
    }
}
