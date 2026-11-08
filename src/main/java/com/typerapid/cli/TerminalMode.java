package com.typerapid.cli;

/**
 * Switches a POSIX terminal between cooked and cbreak mode, and queries its size,
 * by shelling out to {@code stty} on {@code /dev/tty}.
 *
 * <p>Cbreak (not full raw) is used deliberately: it delivers keystrokes one at a
 * time with no echo, but leaves signal handling on so Ctrl-C still aborts — a
 * safety net if the game loop ever wedges. Always pair {@link #enableCbreak()}
 * with {@link #restore(String)} (a shutdown hook plus a {@code finally} block).
 */
final class TerminalMode {

    private TerminalMode() {
    }

    /**
     * Saves the current terminal settings and switches to cbreak mode.
     *
     * @return an opaque token (the prior {@code stty} state) to pass back to
     *         {@link #restore(String)}, or {@code null} if the terminal could not
     *         be configured (e.g. stdin is not a real tty).
     */
    static String enableCbreak() {
        String saved = run("stty -g < /dev/tty");
        if (saved == null || saved.isBlank()) {
            return null;
        }
        // -icanon: per-keystroke; -echo: don't print keys; min 1/time 0: blocking read.
        if (run("stty -icanon -echo min 1 time 0 < /dev/tty") == null) {
            return null;
        }
        return saved;
    }

    /** Restores terminal settings previously captured by {@link #enableCbreak()}. */
    static void restore(String savedState) {
        if (savedState != null && !savedState.isBlank()) {
            run("stty " + savedState + " < /dev/tty");
        }
    }

    /**
     * Returns the current terminal dimensions as {@code {rows, cols}}.
     * Falls back to {@code {24, 80}} if the terminal size cannot be determined.
     */
    static int[] terminalSize() {
        String out = run("stty size < /dev/tty");
        if (out != null) {
            String[] parts = out.trim().split("\\s+");
            if (parts.length == 2) {
                try {
                    int rows = Integer.parseInt(parts[0]);
                    int cols = Integer.parseInt(parts[1]);
                    if (rows > 0 && cols > 0) {
                        return new int[]{rows, cols};
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new int[]{24, 80};
    }

    /** Runs a shell command, returning trimmed stdout, or {@code null} on failure. */
    private static String run(String command) {
        try {
            Process p = new ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            int code = p.waitFor();
            return code == 0 ? out : null;
        } catch (Exception e) {
            return null;
        }
    }
}
