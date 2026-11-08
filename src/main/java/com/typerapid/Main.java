package com.typerapid;

import com.typerapid.core.Enemy;
import com.typerapid.core.Game;
import com.typerapid.core.GameConfig;
import com.typerapid.core.GameState;
import com.typerapid.core.Stats;
import com.typerapid.words.WordSet;
import com.typerapid.words.WordSets;
import java.util.Comparator;
import java.util.Random;

/**
 * Minimal, non-interactive console driver demonstrating the engine end-to-end.
 *
 * <p>This is a placeholder for the real UI (to be added later). It runs a short,
 * seeded simulation that auto-types the closest enemy's word each tick, then
 * prints the resulting stats — enough to sanity-check the {@code core} package
 * from the command line via {@code mvn exec:java}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("TypeRapid — available word-sets:");
        for (WordSet set : WordSets.all()) {
            System.out.printf("  %-22s %s (%d words)%n",
                    set.id(), set.displayName(), set.words().size());
        }

        WordSet wordSet = WordSets.defaultSet();
        Game game = new Game(GameConfig.defaults(), wordSet, new Random(42));
        System.out.printf("%nRunning a demo simulation on \"%s\"...%n", wordSet.displayName());

        double dt = 0.1;
        for (int tick = 0; tick < 600 && game.state() == GameState.RUNNING; tick++) {
            game.update(dt);
            // Auto-play: clear the closest enemy we can lock onto.
            game.enemies().stream()
                    .min(Comparator.comparingDouble(Enemy::distance))
                    .ifPresent(enemy -> enemy.word().chars().forEach(c -> game.type((char) c)));
        }

        Stats stats = game.stats();
        System.out.printf("Result: %s | kills=%d | wpm=%.1f | accuracy=%.0f%%%n",
                game.state(), stats.kills(), stats.wpm(), stats.accuracy() * 100);
    }
}
