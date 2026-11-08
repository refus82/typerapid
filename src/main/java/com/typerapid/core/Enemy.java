package com.typerapid.core;

/**
 * A single approaching enemy (drift mode) carrying a word that must be typed to
 * destroy it.
 *
 * <p>Position is tracked as {@link #distance()} in [0, 1]: {@code 1.0} at the spawn
 * edge, {@code 0.0} once it reaches the player. A {@link #lane()} in [0, 1] gives a
 * fixed lateral position for rendering (it has no effect on gameplay). Typing
 * progress is delegated to a shared {@link WordProgress}.
 */
public final class Enemy {

    private final WordProgress progress;
    private final double speed;
    private final double lane;
    private double distance;

    public Enemy(String word, double distance, double lane, double speed) {
        this.progress = new WordProgress(word);
        this.distance = distance;
        this.lane = lane;
        this.speed = speed;
    }

    public String word() {
        return progress.word();
    }

    public double distance() {
        return distance;
    }

    /** Fixed lateral position in [0, 1], for rendering only. */
    public double lane() {
        return lane;
    }

    /** Characters already typed correctly, from the start of the word. */
    public int matched() {
        return progress.matched();
    }

    /** The next character the player must type to advance on this enemy. */
    char nextChar() {
        return progress.nextChar();
    }

    /** Moves the enemy toward the player by {@code speed * dt}. */
    void advance(double dt) {
        distance -= speed * dt;
    }

    /** @return whether the enemy has reached the player. */
    boolean reachedPlayer() {
        return distance <= 0;
    }

    /** Records one correctly typed character. */
    void registerHit() {
        progress.registerHit();
    }

    /** @return whether the whole word has been typed. */
    boolean isCleared() {
        return progress.isCleared();
    }
}
