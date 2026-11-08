package com.typerapid.core;

import java.util.Objects;

/**
 * Typing progress over a single word: tracks how many leading characters have
 * been matched. Shared by the enemy types of both game modes so the
 * type-to-shoot rules live in exactly one place.
 */
final class WordProgress {

    private final String word;
    private int matched;

    WordProgress(String word) {
        this.word = Objects.requireNonNull(word, "word");
        if (word.isBlank()) {
            throw new IllegalArgumentException("word must not be blank");
        }
    }

    String word() {
        return word;
    }

    int matched() {
        return matched;
    }

    /** The next character that must be typed to advance. */
    char nextChar() {
        return word.charAt(matched);
    }

    /** Records one correctly typed character. */
    void registerHit() {
        matched++;
    }

    /** @return whether the whole word has been typed. */
    boolean isCleared() {
        return matched >= word.length();
    }
}
