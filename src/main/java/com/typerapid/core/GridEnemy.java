package com.typerapid.core;

/**
 * A single enemy in the Invaders-mode formation: a word at a fixed slot
 * {@code (baseCol, baseRow)} within the marching block. Its absolute screen
 * position is the formation's offset plus this base, computed by
 * {@link InvaderGame}. Typing progress is delegated to a shared
 * {@link WordProgress}.
 */
public final class GridEnemy {

    private final WordProgress progress;
    private final int baseCol;
    private final int baseRow;
    private boolean alive = true;

    GridEnemy(String word, int baseCol, int baseRow) {
        this.progress = new WordProgress(word);
        this.baseCol = baseCol;
        this.baseRow = baseRow;
    }

    public String word() {
        return progress.word();
    }

    public int matched() {
        return progress.matched();
    }

    /** Column slot within the formation block (0-based from its left edge). */
    public int baseCol() {
        return baseCol;
    }

    /** Row within the formation block (0-based from its top). */
    public int baseRow() {
        return baseRow;
    }

    public boolean alive() {
        return alive;
    }

    char nextChar() {
        return progress.nextChar();
    }

    void registerHit() {
        progress.registerHit();
    }

    boolean isCleared() {
        return progress.isCleared();
    }

    void kill() {
        alive = false;
    }
}
