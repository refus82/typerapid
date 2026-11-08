package com.typerapid.words;

import java.util.List;

/** Java keywords and common identifiers; practice for programmers. */
public final class ProgrammingKeywordsWordSet implements WordSet {

    @Override
    public String id() {
        return "programming-keywords";
    }

    @Override
    public String displayName() {
        return "Programming Keywords";
    }

    @Override
    public List<String> words() {
        return List.of(
                "public", "private", "protected", "static", "final", "class",
                "interface", "extends", "implements", "void", "return", "import",
                "package", "new", "this", "super", "throws", "throw", "catch",
                "finally", "switch", "case", "default", "break", "continue",
                "boolean", "int", "long", "double", "String", "List", "Map",
                "stream", "filter", "collect", "lambda", "record", "sealed");
    }
}
