package com.typerapid.core;

/**
 * Live performance metrics for a session. Mutated by the game engines as the
 * player types and time passes; exposes derived WPM and accuracy on demand.
 */
public final class Stats {

    private int keystrokes;
    private int correct;
    private int kills;
    private double elapsedSeconds;

    void addTime(double dt) {
        elapsedSeconds += dt;
    }

    void recordKeystroke() {
        keystrokes++;
    }

    void recordCorrect() {
        correct++;
    }

    void recordKill() {
        kills++;
    }

    public int keystrokes() {
        return keystrokes;
    }

    public int correct() {
        return correct;
    }

    public int errors() {
        return keystrokes - correct;
    }

    public int kills() {
        return kills;
    }

    public double elapsedSeconds() {
        return elapsedSeconds;
    }

    /**
     * Words per minute using the standard 5-characters-per-word convention,
     * based on correctly typed characters. Returns 0 before any time elapses.
     */
    public double wpm() {
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (correct / 5.0) / (elapsedSeconds / 60.0);
    }

    /** Fraction of keystrokes that were correct, in [0, 1]; 1.0 before any input. */
    public double accuracy() {
        if (keystrokes == 0) {
            return 1.0;
        }
        return (double) correct / keystrokes;
    }
}
