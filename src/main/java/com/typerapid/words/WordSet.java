package com.typerapid.words;

import java.util.List;

/**
 * A named collection of words used to populate enemies during a game.
 *
 * <p>Word-sets are defined as Java code (one class per set) and registered in
 * {@link WordSets}. To add a new set, implement this interface and register it.
 */
public interface WordSet {

    /** Short, unique identifier used to look the set up (e.g. {@code "common-english"}). */
    String id();

    /** Human-readable name shown in menus. */
    String displayName();

    /**
     * The words belonging to this set. Must be non-empty and contain no blank
     * entries; words should not contain whitespace (each word is typed as a unit).
     */
    List<String> words();
}
