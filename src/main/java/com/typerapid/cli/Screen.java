package com.typerapid.cli;

/**
 * ANSI escape codes and frame fragments shared by the two TUIs. Everything here
 * is a pure string builder, so it is exercised headlessly through the TUIs'
 * {@code render()} tests. The real-time {@code play()} loops stay in their own
 * classes — only presentation is shared.
 */
final class Screen {

    static final String CLEAR = "\033[2J";
    static final String HOME = "\033[H";
    static final String HIDE_CURSOR = "\033[?25l";
    static final String SHOW_CURSOR = "\033[?25h";
    static final String RESET = "\033[0m";
    static final String MATCHED = "\033[1;32m"; // bright green
    static final String REMAIN = "\033[0;4m";   // underlined

    private Screen() {
    }

    /** Wraps the target word in-line: matched prefix bright, remainder underlined. */
    static String colorize(String line, int x, int matched, int wordLen) {
        int end = Math.min(line.length(), x + wordLen);
        int mid = Math.min(end, x + matched);
        return line.substring(0, x)
                + MATCHED + line.substring(x, mid)
                + REMAIN + line.substring(mid, end)
                + RESET + line.substring(end);
    }

    /** A full-width row with the player's ship centered in it. */
    static String shipLine(int width) {
        char[] line = new char[width];
        java.util.Arrays.fill(line, ' ');
        String ship = "/^\\";
        int start = Math.max(0, (width - ship.length()) / 2);
        for (int i = 0; i < ship.length() && start + i < width; i++) {
            line[start + i] = ship.charAt(i);
        }
        return new String(line);
    }

    /**
     * A quit-confirmation banner overlaid mid-playfield via absolute ANSI cursor
     * positioning ({@code playfieldRows} counts the rows below the 3-line HUD).
     */
    static String pauseBanner(int width, int playfieldRows) {
        return centeredBanner(width, playfieldRows,
                "  [ PAUSED ]  ",
                "  Do you really want to quit?  ",
                "  No (Esc)  ·  Yes (Enter)  ");
    }

    /** Same overlay style as {@link #pauseBanner}, shown once the game ends. */
    static String gameOverBanner(int width, int playfieldRows) {
        return centeredBanner(width, playfieldRows,
                "  [ GAME OVER ]  ",
                "  Try again?  ",
                "  Exit (Esc)  ·  Try again (Enter)  ");
    }

    /**
     * Centers reverse-video message lines mid-playfield via absolute ANSI cursor
     * positioning ({@code playfieldRows} counts the rows below the 3-line HUD).
     */
    private static String centeredBanner(int width, int playfieldRows, String... msgs) {
        int mid = 3 + playfieldRows / 2 - 1; // 1-based ANSI row, centered in playfield
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgs.length; i++) {
            String m = msgs[i];
            int col = Math.max(1, (width - m.length()) / 2 + 1);
            sb.append(String.format("\033[%d;%dH\033[7m%s\033[0m", mid + i, col, m));
        }
        return sb.toString();
    }
}
