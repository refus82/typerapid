package com.typerapid.cli;

import com.typerapid.core.Enemy;
import com.typerapid.core.Game;
import com.typerapid.core.GameConfig;
import com.typerapid.core.GameState;
import com.typerapid.core.Stats;
import com.typerapid.words.WordSet;
import com.typerapid.words.WordSets;
import java.io.IOException;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Real-time terminal front-end for {@link Game} (drift mode) — "falling words".
 *
 * <p>Enemies descend from the top (far, {@code distance≈1.0}) toward the player's
 * ship at the bottom ({@code distance≈0.0}); each word spawns at its {@code lane}
 * across the width and converges on the ship's column as it nears. Typing the
 * first letter locks the nearest (closest to the player) matching word; clearing
 * it destroys the enemy. The same terminal handling as {@link InvaderTui} applies:
 * cbreak mode
 * (Ctrl-C still works), a daemon input thread, cursor-home redraw, and terminal
 * state restored via both a shutdown hook and {@code finally}.
 *
 * <p>Run from a real terminal (not {@code mvn exec:java}):
 * <pre>{@code
 *   mvn -q -DskipTests package
 *   java -cp target/classes com.typerapid.cli.DriftTui [word-set-id]
 *   java -cp target/classes com.typerapid.cli.DriftTui --selftest
 * }</pre>
 */
public final class DriftTui {

    private static final long FRAME_MILLIS = 33;
    private int width = 72;   // updated in play() to match actual terminal
    private int height = 16;

    private Game game;

    public DriftTui(Game game) {
        this.game = game;
    }

    public static void main(String[] args) throws Exception {
        boolean selfTest = false;
        String wordSetId = null;
        for (String arg : args) {
            if (arg.equals("--selftest")) {
                selfTest = true;
            } else {
                wordSetId = arg;
            }
        }

        WordSet wordSet = wordSetId == null ? WordSets.defaultSet() : WordSets.byId(wordSetId);
        if (wordSet == null) {
            System.err.println("Unknown word-set: " + wordSetId);
            System.err.print("Available: ");
            WordSets.all().forEach(w -> System.err.print(w.id() + " "));
            System.err.println();
            return;
        }

        if (selfTest) {
            selfTest(wordSet);
            return;
        }
        Game game = new Game(GameConfig.defaults(), wordSet);
        new DriftTui(game).play();
    }

    /** Runs the interactive loop. Requires a real terminal. */
    public void play() throws Exception {
        String savedTty = TerminalMode.enableCbreak();
        if (savedTty == null) {
            System.err.println("Could not switch the terminal to cbreak mode "
                    + "(are you running in a real terminal, not via mvn exec:java?).");
            return;
        }
        int[] size = TerminalMode.terminalSize();
        height = Math.max(10, size[0] - 5);  // 3 HUD + 1 player + 1 margin
        width = Math.max(40, size[1] - 1);   // -1 avoids wrap on terminals that advance on col N
        Runtime.getRuntime().addShutdownHook(new Thread(() -> TerminalMode.restore(savedTty)));

        BlockingQueue<Integer> keys = new LinkedBlockingQueue<>();
        Thread reader = new Thread(() -> {
            try {
                int ch;
                while ((ch = System.in.read()) != -1) {
                    keys.put(ch);
                }
            } catch (IOException | InterruptedException ignored) {
                // terminal closed or interrupted: reader simply stops.
            }
        }, "tui-input");
        reader.setDaemon(true);
        reader.start();

        System.out.print(Screen.CLEAR + Screen.HIDE_CURSOR);
        try {
            boolean exit = false;
            while (!exit) {
                long last = System.nanoTime();
                boolean paused = false;
                boolean quit = false;
                while (!quit && game.state() == GameState.RUNNING) {
                    long now = System.nanoTime();
                    double dt = (now - last) / 1_000_000_000.0;
                    last = now;

                    boolean wasRunning = !paused;
                    Integer key;
                    while ((key = keys.poll()) != null) {
                        if (key == 27 /* Esc */) {
                            paused = !paused;
                        } else if ((key == 10 || key == 13) && paused) {
                            quit = true;
                            break;
                        } else if (!paused) {
                            game.type((char) (int) key);
                        }
                    }

                    if (!quit && wasRunning) {
                        game.update(dt);
                    }
                    System.out.print(Screen.HOME + render());
                    if (paused) {
                        System.out.print(Screen.pauseBanner(width, height));
                    }
                    System.out.flush();

                    Thread.sleep(FRAME_MILLIS);
                }
                System.out.print(Screen.HOME + render());
                if (quit) {
                    exit = true;
                } else {
                    System.out.print(Screen.gameOverBanner(width, height));
                    System.out.flush();
                    Integer key;
                    do {
                        key = keys.take();
                    } while (key != 27 && key != 10 && key != 13);
                    if (key == 27 /* Esc */) {
                        exit = true;
                    } else {
                        System.out.print(Screen.CLEAR);
                        game = new Game(game.config(), game.wordSet());
                    }
                }
                System.out.flush();
            }
        } finally {
            System.out.print(Screen.CLEAR + Screen.HOME + Screen.SHOW_CURSOR);
            System.out.flush();
            TerminalMode.restore(savedTty);
        }
    }

    /** Builds one full frame (HUD + falling-word field + player) as a string. */
    public String render() {
        char[][] grid = new char[height][width];
        for (char[] row : grid) {
            java.util.Arrays.fill(row, ' ');
        }

        Enemy target = game.target();
        int targetX = -1;
        int targetY = -1;
        int targetMatched = 0;
        int targetLen = 0;
        for (Enemy enemy : game.enemies()) {
            int y = rowFor(enemy.distance());
            int x = colFor(enemy.lane(), enemy.distance(), enemy.word().length());
            String word = enemy.word();
            for (int i = 0; i < word.length() && x + i < width; i++) {
                grid[y][x + i] = word.charAt(i);
            }
            if (enemy == target) {
                targetX = x;
                targetY = y;
                targetMatched = enemy.matched();
                targetLen = word.length();
            }
        }

        StringBuilder sb = new StringBuilder();
        appendHud(sb);
        for (int y = 0; y < height; y++) {
            String line = new String(grid[y]);
            if (y == targetY && targetX >= 0) {
                line = Screen.colorize(line, targetX, targetMatched, targetLen);
            }
            sb.append(line).append('\n');
        }
        sb.append(Screen.shipLine(width)).append('\n');
        return sb.toString();
    }

    /** distance 1.0 (far) -> top row 0; distance 0.0 (player) -> bottom row. */
    private int rowFor(double distance) {
        double d = Math.max(0.0, Math.min(1.0, distance));
        return (int) Math.round((1.0 - d) * (height - 1));
    }

    /**
     * Horizontal position of a word: it starts at its spawn {@code lane} (scattered
     * across the top) and converges on the ship's column as it approaches, so
     * enemies funnel toward the player rather than dropping straight down.
     */
    private int colFor(double lane, double distance, int wordLen) {
        double l = Math.max(0.0, Math.min(1.0, lane));
        double progress = 1.0 - Math.max(0.0, Math.min(1.0, distance)); // 0 far -> 1 at ship
        int range = Math.max(0, width - wordLen);
        int spawnLeft = (int) Math.round(l * range);
        int shipLeft = Math.max(0, Math.min(range, (width / 2) - (wordLen / 2)));
        return (int) Math.round(spawnLeft + (shipLeft - spawnLeft) * progress);
    }

    private void appendHud(StringBuilder sb) {
        Stats stats = game.stats();
        sb.append(pad(String.format("TypeRapid · Drift   Lives %s   Kills %d",
                "<3 ".repeat(Math.max(0, game.livesRemaining())).trim(), stats.kills()))).append('\n');
        sb.append(pad(String.format("WPM %.1f   Acc %.0f%%   (type to shoot · Esc pause)",
                stats.wpm(), stats.accuracy() * 100))).append('\n');
        sb.append("-".repeat(width)).append('\n');
    }

    /** Pads (or truncates) a HUD line to the full frame width so every frame
     * fully overwrites the row, leaving no stale characters from a shorter,
     * earlier line (e.g. from a previous round after "try again"). */
    private String pad(String line) {
        return line.length() >= width ? line.substring(0, width) : line + " ".repeat(width - line.length());
    }

    /** Headless scripted run: auto-types toward the nearest enemy, prints frames. */
    private static void selfTest(WordSet wordSet) {
        // Brisk spawning so frames have visible action without a terminal.
        Game game = new Game(new GameConfig(0.5, 0.05, 3), wordSet, new Random(7));
        DriftTui tui = new DriftTui(game);
        for (int frame = 0; frame < 40 && game.state() == GameState.RUNNING; frame++) {
            game.update(0.1);
            Enemy aim = game.target();
            if (aim == null) {
                aim = game.enemies().stream()
                        .min(Comparator.comparingDouble(Enemy::distance))
                        .orElse(null);
            }
            if (aim != null && aim.matched() < aim.word().length()) {
                game.type(aim.word().charAt(aim.matched()));
            }
            System.out.println("--- frame " + frame + " ---");
            System.out.println(tui.render());
        }
        Stats s = game.stats();
        System.out.printf("selftest done: state=%s kills=%d wpm=%.1f acc=%.0f%%%n",
                game.state(), s.kills(), s.wpm(), s.accuracy() * 100);
    }
}
