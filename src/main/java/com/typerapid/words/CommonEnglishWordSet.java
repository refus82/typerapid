package com.typerapid.words;

import java.util.List;

/** Frequent short English words; a gentle default for warming up. */
public final class CommonEnglishWordSet implements WordSet {

    @Override
    public String id() {
        return "common-english";
    }

    @Override
    public String displayName() {
        return "Common English";
    }

    @Override
    public List<String> words() {
        return List.of(
                "the", "and", "for", "you", "this", "that", "with", "have",
                "from", "they", "what", "will", "your", "when", "make", "time",
                "just", "know", "take", "into", "year", "good", "some", "them",
                "than", "then", "look", "only", "come", "over", "think", "also",
                "back", "after", "work", "first", "well", "even", "want", "because");
    }
}
