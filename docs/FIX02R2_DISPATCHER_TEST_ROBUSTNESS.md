# FIX02R2 — dispatcher test robustness

The FIX02 production source compiled successfully after R1. The focused test
suite then failed only in:

`TripRepositoryTest.repositoryRunsStorageOperationsOnInjectedIoDispatcher`

The test recorded `Thread.currentThread().name` and required every name to equal
the literal `ferietur-io-test`.

That is not the actual CA-007 contract. Coroutine debug instrumentation may
temporarily decorate a thread's display name while still executing on the exact
same injected executor thread.

R2 changes **test code only**:

- the executor's `ThreadFactory` stores the created `Thread` in an
  `AtomicReference`;
- `RecordingSource` records `Thread` objects instead of their names;
- the assertion requires object identity (`===`) against the injected executor
  thread.

This proves the intended property directly: every blocking data-source call is
executed on the injected I/O dispatcher thread.

No production source changes.

Old test SHA256:
`8cf84dd37a42bf2e52033ef24c614c4f306adb4de9983db8a2d23bc4381e75ce`

R2 test SHA256:
`ed067c01d69b51f5911ada077db6559e56fdc357c7fde3e43ceb8532bf1391bc`
