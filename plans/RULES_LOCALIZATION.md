# Rules: where user- and log-facing text lives

## Where text goes

Every user- or log-visible string lives in one of two repository classes
(see `plans/MEMORY.md`'s top section for the history of why a third class,
`Text.java`, was retired and merged into `Texts.java`):

- `Texts.java`/`Texts.properties` - plain `String` fields for UI text the
  Java port introduces itself (dialog titles, labels, confirmation/error
  messages shown via `JOptionPane`), named `<ClassName>_<Purpose>`.
- `Messages.java`/`Messages.properties` - severity-typed `Message` fields
  for anything sent through `Application#sendMessage`/`sendInfoMessage`/
  `sendErrorMessage` (log/status/error), named `<Letter><sequence>` where
  the leading letter encodes severity (`S`=status, `I`=info, `E`=error)
  and the sequence number is shared across every severity, never reset per
  letter.

A literal string handed directly to one of these known text sinks belongs
in one of the two classes above, not inline in the calling code:

- `JOptionPane.show*Dialog(...)` - message and title arguments
- `setTitle(...)` / `setDialogTitle(...)`
- `setToolTipText(...)`
- `throw new IOException(...)`

## What stays out of `Texts.java`/`Messages.java`, permanently

These never move, no matter how many of them exist - they are
programming-error guards, not text meant for a user or a log, and none of
them trace back to a C++ resource:

- `throw new IllegalArgumentException(...)` - a parameter-validation guard
  on a method's own arguments.
- `throw new IllegalStateException(...)` - an invariant the calling code
  itself violated (wrong order, wrong pass, wrong object state).
- `throw new RuntimeException(...)` - same reasoning as the two above;
  used for a caller-supplied value that fails an internal sanity check
  (e.g. `ElementFactory`'s mnemonic-label validation).
