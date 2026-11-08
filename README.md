# TypeRapid

A typing-speed trainer in the spirit of **Typ'O'Shooter** (the typing mode of
*Crimsonland*): enemies spawn carrying words and drift toward you; type a word to
destroy its enemy before it reaches you. Lose a life on each enemy that lands; the
game ends when you run out of lives. Practice content comes from **customizable
word-sets** defined in Java code.

## Status

The game **logic core is complete and tested**, with two modes, **both playable**
in a real terminal:

- **Drift mode** — the original Typ'O'Shooter model: words approach from across the
  top and converge on your ship at the bottom; type-shoot them before they reach you.
- **Invaders mode** — a Space-Invaders take: a wave of word-enemies marches sideways,
  drops and reverses at the walls, and you type-shoot them from the bottom.

Both modes detect the terminal dimensions on launch and fill the available space.

## Requirements

- JDK 17 or newer
- Maven 3.9+

## Build & test

```bash
mvn test        # compile and run the unit tests
mvn package     # build the jar
```

## Run the auto-playing demo

```bash
mvn exec:java   # lists word-sets and runs a short seeded drift-mode simulation
```

## Play (interactive)

Needs a real terminal (macOS/Linux). **Do not** launch with `mvn exec:java` —
Maven swallows raw keystrokes.

```bash
mvn -q -DskipTests package
java -cp target/classes com.typerapid.cli.DriftTui                      # drift / falling words
java -cp target/classes com.typerapid.cli.InvaderTui                    # marching formation
java -cp target/classes com.typerapid.cli.DriftTui programming-keywords # pick a word-set
```

`bin/drift` and `bin/invaders` are shortcuts for the two commands above (same
`-cp target/classes` invocation, just shorter to type):

```bash
bin/drift                     # drift mode, default word-set
bin/drift programming-keywords
bin/invaders
```

Type the letters of a word to lock on and shoot it; clear the word to destroy its
enemy. An enemy that reaches you costs a life. Press `Esc` to pause — the game
freezes and asks "Do you really want to quit?": `Enter` quits, `Esc` resumes.
Run out of lives and the same overlay style asks "Try again?": `Enter` starts a
fresh round (same word-set and difficulty), `Esc` exits.
Each mode has a headless render check (no terminal needed), e.g.
`java -cp target/classes com.typerapid.cli.DriftTui --selftest`.

## Word-sets

Word-sets are Java classes implementing `com.typerapid.words.WordSet`, registered
in `com.typerapid.words.WordSets`. To add one:

1. Implement `WordSet` (`id()`, `displayName()`, `words()`).
2. Register it: `WordSets.register(new MyWordSet());` — built-ins register
   themselves in the `WordSets` static initializer.

Built-in sets: `common-english` (default), `programming-keywords`.

## Project layout

```
src/main/java/com/typerapid/
  core/    UI-agnostic engines: drift (Game) + Invaders (InvaderGame), shared types
  words/   word-set system (WordSet, WordSets, built-in sets)
  cli/     real-time terminal UIs (DriftTui, InvaderTui, TerminalMode)
  Main.java  auto-playing drift-mode console demo
src/test/java/com/typerapid/   unit tests mirroring the main packages
```
