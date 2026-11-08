package com.typerapid.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typerapid.core.InvaderConfig;
import com.typerapid.core.InvaderGame;
import com.typerapid.words.WordSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Headless coverage of the renderer (the interactive loop needs a real tty). */
class InvaderTuiTest {

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

    @Test
    void frameShowsHudAndEnemyWords() {
        WordSet ws = singleWord("alpha");
        InvaderGame game = new InvaderGame(new InvaderConfig(6, 2, 2, 10, 100.0, 3), ws, new Random(1));
        String frame = new InvaderTui(game, ws).render();

        assertTrue(frame.contains("Wave 1"), "HUD should show the wave");
        assertTrue(frame.contains("Lives"), "HUD should show lives");
        assertTrue(frame.contains("alpha"), "enemy words should be drawn");
        assertTrue(frame.contains("/^\\"), "player ship should be drawn");
    }

    @Test
    void targetProgressIsHighlighted() {
        WordSet ws = singleWord("alpha");
        InvaderGame game = new InvaderGame(new InvaderConfig(6, 1, 1, 10, 100.0, 3), ws, new Random(1));
        game.type('a'); // lock target, one matched char
        String frame = new InvaderTui(game, ws).render();

        assertTrue(frame.contains("\033[1;32m"), "matched prefix should use the highlight color");
    }

    @Test
    void everyRenderedRowHasConsistentWidth() {
        WordSet ws = singleWord("alpha");
        InvaderConfig cfg = new InvaderConfig(6, 2, 2, 10, 100.0, 3);
        InvaderGame game = new InvaderGame(cfg, ws, new Random(1));
        InvaderTui tui = new InvaderTui(game, ws);

        int expectedWidth = cfg.arenaColumns() * ("alpha".length() + 2);
        // Playfield rows (skip the 3 HUD lines) should all be the arena width.
        String[] lines = tui.render().split("\n", -1);
        for (int i = 3; i < 3 + cfg.playerRow(); i++) {
            assertEquals(expectedWidth, lines[i].length(), "row " + i + " width");
        }
    }
}
