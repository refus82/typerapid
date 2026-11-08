package com.typerapid.core;

/** Outcome of feeding a single typed character to the game. */
public enum TypeResult {
    /** Character matched the next letter of the locked target. */
    HIT,
    /** Character completed a word and destroyed its enemy. */
    KILL,
    /** No enemy matched, or the character broke the locked word. */
    MISS,
    /** Game was not running; the character was discarded. */
    IGNORED
}
