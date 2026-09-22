# Plan: can TestRunner's runTest(name, testCase) drop the redundant string?

**Status: decided, 2026-09-23 - Option 1 (leave as-is).** `TestRunner`
keeps `runTest("ClassName", ClassName::testMethod)` exactly as it is; no
code change. Kept as a record of the options considered, should the
tradeoffs ever need revisiting.

## The pattern in question

`TestRunner.execute()` registers each test with both a string name and a
method reference:

```java
runTest("ValueSetsTest", ValueSetsTest::testValueSets);
```

Of the 25 registrations, 20 follow this exact shape - the string is a
plain, information-free echo of the class already named right next to it
in the method reference. Only one of the two copies is refactor-safe: an
IDE "rename symbol" on `ValueSetsTest` updates the method reference
automatically; the string literal `"ValueSetsTest"` only follows along if
the IDE's rename is also told to search string literals, and nothing in
the build catches the drift if it doesn't - the log line would just
silently start mislabeling that test.

Three registrations don't fit the plain pattern, and matter for any
proposed fix:

- `runTest("AssemblerTest", () -> AssemblerTest.testAssembler(new Workspace(new ComputerSystemFactory())));`
- `runTest("ComputerSystemTest", () -> ComputerSystemTest.testSystems(new ComputerSystemFactory()));`
  (both need a constructed argument, so they're lambdas, not bare method
  references)
- `runTest("SegmentTest.testSegmentRangeEdit", SegmentTest::testSegmentRangeEdit);`
  (`SegmentTest` registers two different methods, so the plain class name
  alone would collide with the `SegmentTest::testSegment` entry)

## Options considered

### Option 1: Leave it as-is

No code change. The redundancy is real but minor, the code is completely
unambiguous to read with no reflection or cleverness involved, and
today's log output (`"Running ValueSetsTest."`) stays exactly as it is.

### Option 2: Derive the name from the method reference via `SerializedLambda`

A functional interface that extends `Serializable` gets a synthetic
`writeReplace()` method; calling it reflectively returns a
`java.lang.invoke.SerializedLambda`, which exposes the target class and
method name for an actual method reference. Verified working in this
exact environment (JDK 21, this project's plain classpath setup - no
`module-info.java` to fight):

```java
private interface TestCase extends Serializable {
    void run() throws Exception;
}

private static String nameOf(TestCase testCase) {
    try {
        Method writeReplace = testCase.getClass().getDeclaredMethod("writeReplace");
        writeReplace.setAccessible(true);
        SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(testCase);
        String implClass = lambda.getImplClass().replace('/', '.');
        String simpleClass = implClass.substring(implClass.lastIndexOf('.') + 1);
        return simpleClass + "." + lambda.getImplMethodName();
    } catch (ReflectiveOperationException ex) {
        throw new IllegalStateException(ex);
    }
}
```

Every plain-pattern call site becomes:

```java
runTest(ValueSetsTest::testValueSets);
runTest(SegmentTest::testSegment);
runTest(SegmentTest::testSegmentRangeEdit);
```

Consequences, all confirmed by a throwaway probe (not just assumed):

- **The log format changes for every test, not just the odd ones.**
  Deriving just the bare class name loses the ability to disambiguate
  `SegmentTest`'s two entries without hardcoding an exception for that one
  class, so the natural uniform output becomes `"Running
  ValueSetsTest.testValueSets."` etc. for every single test - more
  precise, but a visible change to today's log output, not a purely
  internal refactor.
- **The two argument-needing tests don't get a readable name this way.**
  A raw lambda body's implementation method is a compiler-generated
  synthetic - the probe returned `LambdaProbe.lambda$main$4cd2628a$1` for
  one, not something worth logging. Two sub-options:
  - (a) keep `AssemblerTest`/`ComputerSystemTest` on an explicit
    `runTest(String, TestCase)` overload - a small, permanent, documented
    exception for the two cases that truly need an argument; or
  - (b) give `AssemblerTest.testAssembler()`/`ComputerSystemTest.testSystems()`
    their own zero-arg overloads that build their own `Workspace`/
    `ComputerSystemFactory` internally, so the call site becomes a bare
    method reference too and the exception disappears entirely - a small,
    unrelated-to-naming change to two model test classes' public shape.
- **Cost:** one `setAccessible(true)` reflection helper using a
  JDK-standard but not widely-known mechanism - stable since Java 8, nothing
  exotic, but a future reader who hasn't seen this trick has to look it up.
  Performance is a non-issue (~0.008 ms per call in the probe, called
  ~25 times total).

### Option 3: `Class<?>` + method name as a plain string, via `Method.invoke`

```java
runTest(ValueSetsTest.class, "testValueSets");
```

Removes the redundant *class* name (`Class.getSimpleName()` derives it)
but keeps the method name as a bare string - trading one kind of
stringliness for a smaller one, and a worse one: a typo'd or renamed
method name becomes a runtime `NoSuchMethodException` instead of a
compile error, which is a real regression from today's `ClassName::method`
(an IDE renames it automatically; a typo can't even compile). Doesn't
help the two argument-needing tests any more than Option 2 does. Not
recommended over Option 2 - it gives up more type safety for less benefit.

### Option 4: Full reflective auto-discovery, no explicit method registration at all

Scan a list of test classes for every `public static void testXxx()`
method and invoke each automatically, instead of listing methods one by
one. The most invasive option: it would remove explicit control over test
*order*, which currently matters (the class javadoc's "fast headless
tests report first" convention is threaded through the registration
order today), so it would need a different mechanism to keep that (e.g.
register just the *classes* in order, discover each one's method(s)
reflectively). A bigger redesign than the specific redundancy flagged
here; noted for completeness, not explored further.

## Recommendation

Option 1 if the redundancy is tolerable as-is - it is the only option
with zero added complexity and zero behavior/log-format change. Option 2
is the only one that actually removes the redundant string while keeping
compile-time safety on the real test invocation, at the cost of one
small, self-contained, verified-working reflection helper and a visible
(arguably clearer) log-format change across all 25 lines; it also needs a
choice on sub-options (a)/(b) for the two argument-needing tests. Option 3
is not recommended. Option 4 is out of scope for what was asked.
