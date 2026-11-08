package com.typerapid.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typerapid.core.Game;
import com.typerapid.core.GameConfig;
import com.typerapid.words.WordSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Headless coverage of the drift-mode renderer (interactive loop needs a tty). */
class DriftTuiTest {

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
    void frameShowsHudPlayerAndDescendingWords() {
        WordSet ws = singleWord("alpha");
        Game game = new Game(new GameConfig(0.5, 0.05, 3), ws, new Random(1));
        game.update(1.0); // spawn a couple of enemies
        String frame = new DriftTui(game).render();

        assertTrue(frame.contains("Drift"), "HUD should label the mode");
        assertTrue(frame.contains("Lives"), "HUD should show lives");
        assertTrue(frame.contains("alpha"), "descending words should be drawn");
        assertTrue(frame.contains("/^\\"), "player ship should be drawn");
    }

    @Test
    void targetProgressIsHighlighted() {
        WordSet ws = singleWord("alpha");
        Game game = new Game(new GameConfig(0.5, 0.05, 3), ws, new Random(1));
        game.update(1.0);
        game.type('a'); // lock nearest, one matched char
        String frame = new DriftTui(game).render();

        assertTrue(frame.contains("\033[1;32m"), "matched prefix should be highlighted");
    }

    @Test
    void everyRenderedRowHasConsistentWidth() {
        WordSet ws = singleWord("alpha");
        Game game = new Game(new GameConfig(0.5, 0.05, 3), ws, new Random(1));
        game.update(1.0);
        String[] lines = new DriftTui(game).render().split("\n", -1);

        // Skip the 3 HUD lines; the 16 playfield rows should all be 72 wide.
        for (int i = 3; i < 3 + 16; i++) {
            assertEquals(72, lines[i].length(), "row " + i + " width");
        }
    }
}
