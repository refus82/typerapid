package com.typerapid.cli;

import com.typerapid.core.GameState;
import com.typerapid.core.GridEnemy;
import com.typerapid.core.InvaderConfig;
import com.typerapid.core.InvaderGame;
import com.typerapid.core.Stats;
import com.typerapid.words.WordSet;
import com.typerapid.words.WordSets;
import java.io.IOException;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Real-time terminal front-end for {@link InvaderGame} — the playable client.
 *
 * <p>It puts the terminal into cbreak mode (per-keystroke input, no echo, but
 * Ctrl-C still works), reads keys on a daemon thread, and redraws ~30fps using
 * ANSI cursor-home so the frame updates in place. The terminal state is always
 * restored, via both a shutdown hook and a {@code finally} block.
 *
 * <p>Run it from a real terminal:
 * <pre>{@code
 *   mvn -q -DskipTests package
 *   java -cp target/classes com.typerapid.cli.InvaderTui [word-set-id]
 * }</pre>
 * Do <em>not</em> launch it via {@code mvn exec:java}: Maven's stdin handling
 * swallows raw keystrokes. Pass {@code --selftest} to run a headless, scripted
 * render check that needs no terminal.
 */
public final class InvaderTui {

    private static final long FRAME_MILLIS = 33;
    private static final int COLUMN_GAP = 2;

    private InvaderGame game;
    private final int slotWidth;
    private final int width;

    public InvaderTui(InvaderGame game, WordSet wordSet) {
        this.game = game;
        int maxWordLen = wordSet.words().stream().mapToInt(String::length).max().orElse(4);
        this.slotWidth = maxWordLen + COLUMN_GAP;
        this.width = game.config().arenaColumns() * slotWidth;
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
        InvaderGame game = new InvaderGame(adaptedConfig(wordSet), wordSet);
        new InvaderTui(game, wordSet).play();
    }

    /** Runs the interactive loop. Requires a real terminal. */
    public void play() throws Exception {
        String savedTty = TerminalMode.enableCbreak();
        if (savedTty == null) {
            System.err.println("Could not switch the terminal to cbreak mode "
                    + "(are you running in a real terminal, not via mvn exec:java?).");
            return;
        }
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
                        System.out.print(Screen.pauseBanner(width, game.config().playerRow()));
                    }
                    System.out.flush();

                    Thread.sleep(FRAME_MILLIS);
                }
                System.out.print(Screen.HOME + render());
                if (quit) {
                    exit = true;
                } else {
                    System.out.print(Screen.gameOverBanner(width, game.config().playerRow()));
                    System.out.flush();
                    Integer key;
                    do {
                        key = keys.take();
                    } while (key != 27 && key != 10 && key != 13);
                    if (key == 27 /* Esc */) {
                        exit = true;
                    } else {
                        System.out.print(Screen.CLEAR);
                        game = new InvaderGame(game.config(), game.wordSet());
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

    /** Builds one full frame (HUD + playfield + player) as a printable string. */
    public String render() {
        InvaderConfig cfg = game.config();
        int playerRow = cfg.playerRow();

        char[][] grid = new char[playerRow][width];
        for (char[] row : grid) {
            java.util.Arrays.fill(row, ' ');
        }

        GridEnemy target = game.target();
        int targetX = -1;
        int targetMatched = 0;
        int targetLen = 0;
        for (GridEnemy enemy : game.enemies()) {
            int x = game.absCol(enemy) * slotWidth;
            int y = game.absRow(enemy);
            if (y < 0 || y >= playerRow) {
                continue;
            }
            String word = enemy.word();
            for (int i = 0; i < word.length() && x + i < width; i++) {
                grid[y][x + i] = word.charAt(i);
            }
            if (enemy == target) {
                targetX = x;
                targetMatched = enemy.matched();
                targetLen = word.length();
            }
        }

        StringBuilder sb = new StringBuilder();
        appendHud(sb);
        for (int y = 0; y < playerRow; y++) {
            String line = new String(grid[y]);
            if (targetX >= 0 && isTargetRow(target, y)) {
                line = Screen.colorize(line, targetX, targetMatched, targetLen);
            }
            sb.append(line).append('\n');
        }
        sb.append(Screen.shipLine(width)).append('\n');
        return sb.toString();
    }

    private boolean isTargetRow(GridEnemy target, int y) {
        return target != null && game.absRow(target) == y;
    }

    private void appendHud(StringBuilder sb) {
        Stats stats = game.stats();
        sb.append(pad(String.format("TypeRapid · Invaders   Wave %d   Lives %s   Kills %d",
                game.wave(), "<3 ".repeat(Math.max(0, game.livesRemaining())).trim(), stats.kills()))).append('\n');
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

    /** Derives an {@link InvaderConfig} sized to the current terminal. */
    private static InvaderConfig adaptedConfig(WordSet wordSet) {
        int maxWordLen = wordSet.words().stream().mapToInt(String::length).max().orElse(4);
        int slotWidth = maxWordLen + COLUMN_GAP;
        int[] size = TerminalMode.terminalSize();
        int arenaColumns = Math.max(4, size[1] / slotWidth);
        int formationColumns = Math.max(2, arenaColumns * 2 / 3);
        int formationRows = 3;
        int playerRow = Math.max(formationRows + 2, size[0] - 5);
        return new InvaderConfig(arenaColumns, formationColumns, formationRows, playerRow, 0.8, 3);
    }

    /** Headless scripted run: auto-types toward the nearest enemy, prints frames. */
    private static void selfTest(WordSet wordSet) {
        InvaderGame game = new InvaderGame(InvaderConfig.defaults(), wordSet, new Random(7));
        InvaderTui tui = new InvaderTui(game, wordSet);
        for (int frame = 0; frame < 40 && game.state() == GameState.RUNNING; frame++) {
            game.update(0.1);
            // Auto-play: lock the lowest enemy and type its next letter.
            GridEnemy aim = game.target();
            if (aim == null) {
                aim = game.enemies().stream()
                        .max(Comparator.comparingInt(game::absRow))
                        .orElse(null);
            }
            if (aim != null && aim.matched() < aim.word().length()) {
                game.type(aim.word().charAt(aim.matched()));
            }
            System.out.println("--- frame " + frame + " ---");
            System.out.println(tui.render());
        }
        Stats s = game.stats();
        System.out.printf("selftest done: state=%s wave=%d kills=%d wpm=%.1f acc=%.0f%%%n",
                game.state(), game.wave(), s.kills(), s.wpm(), s.accuracy() * 100);
    }
}
