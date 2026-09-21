#!/usr/bin/env python3
"""
find-text-matches.py - finds Java string literals that duplicate a real
C++ STRINGTABLE resource text, and so could be replaced by a Text.java (or
Texts.java) constant instead.

Background: Text.java holds every faithfully-ported C++ STRINGTABLE
(IDS_*) constant, and Texts.java holds UI text this Java port invents
itself with no C++ resource to mirror - see both classes' own javadoc.
When a dialog/message is ported, it's easy to accidentally hardcode the
same text as a Java string literal instead of referencing the constant
that already exists (or should exist) for it - this script finds those.

What it reports:
  A) Java literals whose text exactly equals a STRINGTABLE entry's text,
     where Text.java already has that field - a direct Text.<NAME>
     substitution.
  B) Same, but the STRINGTABLE entry has no Text.java field yet - port it
     into Text.java/Text.properties first, then reference it.
  C) Texts.java entries whose text happens to exactly match a real
     STRINGTABLE entry - these aren't actually "text this port introduces
     itself" per Texts.java's own convention, and arguably belong in
     Text.java instead.
  D) A fuzzy pass (difflib similarity + shared-word overlap) that also
     catches messages built by string concatenation ("Could not read '"
     + path + "'.") and near-miss literals (missing "Error: " prefix,
     trailing punctuation, etc.) that (A)/(B)'s exact-text match can't
     catch. Lower precision - every hit needs a human read before acting
     on it; expect real false positives (e.g. two different dialog
     titles that happen to share several words).

Read-only: never modifies any file. Requires Python 3, no third-party
packages.

.EXAMPLE
    python build\\find-text-matches.py
"""
import difflib
import re
import sys
from pathlib import Path

JAVA_ROOT = Path(__file__).resolve().parent.parent / "src" / "com" / "wudsn" / "tools" / "dis6502"
JAVA_REPO_ROOT = Path(__file__).resolve().parent.parent

# The sibling C++ project this Java port is ported from - see this repo's
# own CLAUDE.md for why this path is a fixed, documented cross-repo
# reference rather than something discovered at run time.
CPP_RC = Path(r"C:\jac\system\Windows\Programming\Repositories\dis6502\src\dis6502.rc")

TEXT_JAVA = JAVA_ROOT / "Text.java"
TEXT_PROPERTIES = JAVA_ROOT / "Text.properties"
TEXTS_JAVA = JAVA_ROOT / "Texts.java"
TEXTS_PROPERTIES = JAVA_ROOT / "Texts.properties"

MIN_LITERAL_LEN = 4  # Skip trivial/short literals (single chars, format specs, etc.)


def unescape_rc_string(raw: str) -> str:
    """Unescapes an RC STRINGTABLE string body: '""' -> '"', plus standard C escapes."""
    # RC's only quote-escape is doubling; do that first so a doubled quote
    # isn't later mistaken for the start/end of a \-escape sequence.
    s = raw.replace('""', '\x00QUOTE\x00')
    out = []
    i = 0
    while i < len(s):
        c = s[i]
        if c == '\\' and i + 1 < len(s):
            n = s[i + 1]
            if n == 'n':
                out.append('\n')
                i += 2
                continue
            if n == 't':
                out.append('\t')
                i += 2
                continue
            if n == '\\':
                out.append('\\')
                i += 2
                continue
        out.append(c)
        i += 1
    return ''.join(out).replace('\x00QUOTE\x00', '"')


def unescape_java_string(raw: str) -> str:
    """Unescapes a Java string-literal body to its runtime value."""
    out = []
    i = 0
    while i < len(raw):
        c = raw[i]
        if c == '\\' and i + 1 < len(raw):
            n = raw[i + 1]
            mapping = {'n': '\n', 't': '\t', 'r': '\r', '"': '"', "'": "'", '\\': '\\'}
            if n in mapping:
                out.append(mapping[n])
                i += 2
                continue
        out.append(c)
        i += 1
    return ''.join(out)


def parse_stringtable(rc_path: Path):
    """Returns {IDS_NAME: unescaped_text} for every entry in the (first/only) STRINGTABLE block."""
    lines = rc_path.read_text(encoding='utf-8', errors='replace').splitlines()
    result = {}
    in_table = False
    in_begin = False
    entry_re = re.compile(r'^\s*(IDS_[A-Z0-9_]+)\s+"(.*)"\s*$')
    for line in lines:
        stripped = line.strip()
        if stripped.startswith('STRINGTABLE'):
            in_table = True
            continue
        if in_table and stripped == 'BEGIN':
            in_begin = True
            continue
        if in_table and in_begin and stripped == 'END':
            break
        if in_table and in_begin:
            m = entry_re.match(line)
            if m:
                name, body = m.group(1), m.group(2)
                result[name] = unescape_rc_string(body)
            elif stripped:
                print(f"WARNING: unparsed STRINGTABLE line: {line!r}", file=sys.stderr)
    return result


def parse_java_fields(java_path: Path):
    """Returns the set of 'public static String NAME;' field names declared in a Text.java-style class."""
    text = java_path.read_text(encoding='utf-8', errors='replace')
    return set(re.findall(r'public static String\s+(\w+)\s*;', text))


def parse_properties(props_path: Path):
    """Returns {key: value} for a .properties file, joining '\\'-continued lines and unescaping '\\n'/'\\t'."""
    raw = props_path.read_text(encoding='utf-8', errors='replace')
    # Join backslash-newline continuations into one logical line.
    raw = re.sub(r'\\\r?\n', '', raw)
    result = {}
    for line in raw.splitlines():
        if not line.strip() or line.strip().startswith('#'):
            continue
        if '=' not in line:
            continue
        key, _, value = line.partition('=')
        key = key.strip()
        value = value.replace('\\n', '\n').replace('\\t', '\t').replace('\\\\', '\\')
        result[key] = value
    return result


JAVA_STRING_LITERAL_RE = re.compile(r'"((?:[^"\\]|\\.)*)"')


def find_java_literals(java_root: Path, repo_root: Path):
    """Yields (file_relpath, line_number, unescaped_literal_text) for every string literal in every .java file."""
    for java_file in sorted(java_root.rglob('*.java')):
        rel = java_file.relative_to(repo_root)
        lines = java_file.read_text(encoding='utf-8', errors='replace').splitlines()
        for lineno, line in enumerate(lines, start=1):
            # Skip full-line comments outright (cheap heuristic - not a full Java parser).
            stripped = line.strip()
            if stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*'):
                continue
            for m in JAVA_STRING_LITERAL_RE.finditer(line):
                literal = unescape_java_string(m.group(1))
                if len(literal) >= MIN_LITERAL_LEN:
                    yield rel, lineno, literal


def normalize_for_fuzzy(s: str) -> str:
    """Collapses whitespace and strips for a slightly-looser comparison pass."""
    return re.sub(r'\s+', ' ', s).strip()


PLACEHOLDER_RE = re.compile(r'\{\d\}')
WORD_RE = re.compile(r'[A-Za-z]{4,}')


def skeleton(text: str) -> str:
    """The 'fixed text' skeleton used for fuzzy comparison: placeholders collapsed to one marker."""
    return normalize_for_fuzzy(PLACEHOLDER_RE.sub(' \x01 ', text)).lower()


def significant_words(text: str) -> set:
    return {w.lower() for w in WORD_RE.findall(text)}


def find_java_concat_candidates(java_root: Path, repo_root: Path):
    """
    Yields (file_relpath, line_number, joined_literal_text) for each Java
    statement (a ';'-delimited chunk, a crude but adequate approximation)
    that concatenates two or more string literals with '+' - e.g.
    "Could not read disk image '" + path + "': " + err.getMessage() -
    which a single-literal exact match can never catch, since each half is
    only a text fragment. Note this groups every string literal in the
    same statement, including ones that are actually separate method-call
    arguments (e.g. a JOptionPane message and its title) rather than truly
    '+'-concatenated with each other - a known source of noisier, but
    still often informative, hits in section D.
    """
    for java_file in sorted(java_root.rglob('*.java')):
        rel = java_file.relative_to(repo_root)
        text = java_file.read_text(encoding='utf-8', errors='replace')
        for stmt_match in re.finditer(r'[^;]*;', text):
            stmt = stmt_match.group(0)
            if '+' not in stmt:
                continue
            literals = [unescape_java_string(m.group(1)) for m in JAVA_STRING_LITERAL_RE.finditer(stmt)]
            literals = [lit for lit in literals if len(lit) >= 2]
            if len(literals) < 2:
                continue
            start_line = text.count('\n', 0, stmt_match.start()) + 1
            joined = ' '.join(literals)
            if len(joined) >= MIN_LITERAL_LEN:
                yield rel, start_line, joined


def main():
    stringtable = parse_stringtable(CPP_RC)
    print(f"Parsed {len(stringtable)} STRINGTABLE entries from dis6502.rc\n")

    text_java_fields = parse_java_fields(TEXT_JAVA)
    text_properties = parse_properties(TEXT_PROPERTIES)
    texts_java_fields = parse_java_fields(TEXTS_JAVA)
    texts_properties = parse_properties(TEXTS_PROPERTIES)

    # Sanity cross-check: every Text.java field should have a STRINGTABLE counterpart
    # with matching text (catches transcription drift, not this script's main job,
    # but cheap to report).
    mismatches = []
    for field in sorted(text_java_fields):
        if field not in stringtable:
            mismatches.append(f"  {field}: declared in Text.java but no longer in dis6502.rc's STRINGTABLE")
        elif field in text_properties and text_properties[field] != stringtable[field]:
            mismatches.append(
                f"  {field}: Text.properties={text_properties[field]!r} != dis6502.rc={stringtable[field]!r}")
    if mismatches:
        print("=== Sanity check: Text.java fields whose text diverges from dis6502.rc ===")
        print('\n'.join(mismatches))
        print()

    # Build reverse index: exact text -> [IDS_NAME, ...] (a text can appear more than once).
    text_to_ids = {}
    for name, text in stringtable.items():
        text_to_ids.setdefault(text, []).append(name)

    exact_matches = []  # (file, line, literal, [ids_names])
    for rel, lineno, literal in find_java_literals(JAVA_ROOT, JAVA_REPO_ROOT):
        if literal in text_to_ids:
            exact_matches.append((rel, lineno, literal, text_to_ids[literal]))

    print(f"=== Exact matches: Java literal text == a real STRINGTABLE entry's text ({len(exact_matches)} found) ===\n")
    already_ported = []
    not_yet_ported = []
    for rel, lineno, literal, ids_names in exact_matches:
        ported = [n for n in ids_names if n in text_java_fields]
        unported = [n for n in ids_names if n not in text_java_fields]
        if ported:
            already_ported.append((rel, lineno, literal, ported))
        if unported:
            not_yet_ported.append((rel, lineno, literal, unported))

    print(f"--- A) {len(already_ported)}: Text.java already has the field - just needs Text.<NAME> to replace the literal ---")
    for rel, lineno, literal, ported in already_ported:
        display = literal if len(literal) <= 70 else literal[:67] + "..."
        print(f"  {rel}:{lineno}  {display!r}  ->  Text.{'/Text.'.join(ported)}")

    print(f"\n--- B) {len(not_yet_ported)}: matches a STRINGTABLE entry not yet ported into Text.java ---")
    for rel, lineno, literal, unported in not_yet_ported:
        display = literal if len(literal) <= 70 else literal[:67] + "..."
        print(f"  {rel}:{lineno}  {display!r}  ->  {'/'.join(unported)} (add to Text.java first)")

    # Reverse check: Texts.java entries whose value coincidentally matches a real STRINGTABLE text.
    print(f"\n=== C) Texts.java entries whose text matches a real STRINGTABLE entry (should these be in Text.java instead?) ===")
    misclassified = []
    for field in sorted(texts_java_fields):
        value = texts_properties.get(field)
        if value is not None and value in text_to_ids:
            misclassified.append((field, value, text_to_ids[value]))
    if not misclassified:
        print("  (none)")
    for field, value, ids_names in misclassified:
        display = value if len(value) <= 70 else value[:67] + "..."
        print(f"  Texts.{field} = {display!r}  matches  {'/'.join(ids_names)}")

    # === Tier 2: fuzzy pass, for messages built by string concatenation
    # ("Could not read disk image '" + path + "': " + err) that an exact
    # single-literal match can never catch, plus near-miss single literals
    # (punctuation/capitalization drift). Lower precision - for human
    # review, not automatic replacement.
    exact_locations = {(rel, lineno) for rel, lineno, *_ in exact_matches}
    stringtable_skeletons = {name: skeleton(text) for name, text in stringtable.items()}
    stringtable_words = {name: significant_words(text) for name, text in stringtable.items()}

    candidates = []  # (rel, lineno, java_text, kind)
    for rel, lineno, literal in find_java_literals(JAVA_ROOT, JAVA_REPO_ROOT):
        if (rel, lineno) in exact_locations or literal in text_to_ids:
            continue
        candidates.append((rel, lineno, literal, 'literal'))
    for rel, lineno, joined in find_java_concat_candidates(JAVA_ROOT, JAVA_REPO_ROOT):
        candidates.append((rel, lineno, joined, 'concat'))

    FUZZY_THRESHOLD = 0.55
    MIN_SHARED_WORDS = 2
    fuzzy_hits = []
    for rel, lineno, java_text, kind in candidates:
        java_skel = skeleton(java_text)
        java_words = significant_words(java_text)
        best = None
        for name, st_skel in stringtable_skeletons.items():
            if len(java_words & stringtable_words[name]) < MIN_SHARED_WORDS:
                continue
            ratio = difflib.SequenceMatcher(None, java_skel, st_skel).ratio()
            if ratio >= FUZZY_THRESHOLD and (best is None or ratio > best[1]):
                best = (name, ratio)
        if best:
            fuzzy_hits.append((rel, lineno, java_text, kind, best[0], best[1]))

    fuzzy_hits.sort(key=lambda h: -h[5])
    print(f"\n=== D) Fuzzy/concatenation matches - needs human review ({len(fuzzy_hits)} found, threshold {FUZZY_THRESHOLD}) ===")
    for rel, lineno, java_text, kind, name, ratio in fuzzy_hits:
        display = java_text if len(java_text) <= 80 else java_text[:77] + "..."
        st_display = stringtable[name] if len(stringtable[name]) <= 80 else stringtable[name][:77] + "..."
        ported = " (ported)" if name in text_java_fields else " (NOT ported into Text.java)"
        print(f"  [{ratio:.2f}] {rel}:{lineno} ({kind})")
        print(f"         java: {display!r}")
        print(f"         rc:   {st_display!r}  ->  {name}{ported}")

    print(f"\n=== Summary ===")
    print(f"  STRINGTABLE entries: {len(stringtable)}")
    print(f"  Text.java fields (ported): {len(text_java_fields)}")
    print(f"  Exact-match Java literals found: {len(exact_matches)}")
    print(f"    -> replaceable with an existing Text.java field: {len(already_ported)}")
    print(f"    -> need the field ported into Text.java first: {len(not_yet_ported)}")
    print(f"  Texts.java entries that look misclassified: {len(misclassified)}")
    print(f"  Fuzzy/concatenation candidates (human review): {len(fuzzy_hits)}")


if __name__ == '__main__':
    main()
