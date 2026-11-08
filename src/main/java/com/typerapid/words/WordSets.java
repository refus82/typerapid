package com.typerapid.words;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Central registry of available {@link WordSet}s, keyed by {@link WordSet#id()}.
 *
 * <p>Built-in sets are registered statically. Additional sets (including ones
 * defined elsewhere) can be added at runtime via {@link #register(WordSet)}.
 */
public final class WordSets {

    private static final Map<String, WordSet> REGISTRY = new LinkedHashMap<>();

    static {
        register(new CommonEnglishWordSet());
        register(new ProgrammingKeywordsWordSet());
    }

    private WordSets() {
    }

    /** Registers (or replaces) a set under its {@link WordSet#id()}. */
    public static void register(WordSet wordSet) {
        Objects.requireNonNull(wordSet, "wordSet");
        REGISTRY.put(wordSet.id(), wordSet);
    }

    /** @return the set with the given id, or {@code null} if none is registered. */
    public static WordSet byId(String id) {
        return REGISTRY.get(id);
    }

    /** @return all registered sets in registration order. */
    public static Collection<WordSet> all() {
        return REGISTRY.values();
    }

    /** @return the set used when none is chosen explicitly. */
    public static WordSet defaultSet() {
        return byId("common-english");
    }
}
