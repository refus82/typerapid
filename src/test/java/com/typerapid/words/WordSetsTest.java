package com.typerapid.words;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WordSetsTest {

    @Test
    void builtInSetsAreRegistered() {
        assertNotNull(WordSets.byId("common-english"));
        assertNotNull(WordSets.byId("programming-keywords"));
    }

    @Test
    void defaultSetIsCommonEnglish() {
        assertEquals("common-english", WordSets.defaultSet().id());
    }

    @Test
    void unknownIdReturnsNull() {
        assertNull(WordSets.byId("does-not-exist"));
    }

    @Test
    void everyWordIsUsableAsTypingTarget() {
        for (WordSet set : WordSets.all()) {
            List<String> words = set.words();
            assertFalse(words.isEmpty(), set.id() + " must not be empty");
            for (String word : words) {
                assertFalse(word.isBlank(), set.id() + " has a blank word");
                assertFalse(word.chars().anyMatch(Character::isWhitespace),
                        set.id() + " word contains whitespace: '" + word + "'");
            }
        }
    }

    @Test
    void registerReplacesByIdAndIsRetrievable() {
        WordSet custom = new WordSet() {
            @Override
            public String id() {
                return "custom-test-set";
            }

            @Override
            public String displayName() {
                return "Custom Test Set";
            }

            @Override
            public List<String> words() {
                return List.of("alpha", "bravo");
            }
        };
        WordSets.register(custom);
        assertSame(custom, WordSets.byId("custom-test-set"));
        assertTrue(WordSets.all().contains(custom));
    }
}
