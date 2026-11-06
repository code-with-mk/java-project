# Course Waitlist

A plain Java, in-memory service for one course offering. It supports enrollment,
FIFO waitlisting, dropping, and automatic promotion. A scripted console demo is
included; no frontend or database is required. The service is package-private; callers
and tests use the `com.assessment.waitlist` package.

## Setup and commands

Install JDK 21 or later and ensure `java` and `javac` are available. The project
targets Java 21. Maven Wrapper downloads Maven 3.9.9, so no global Maven
installation is needed. First use requires network access to Maven Central.

From this project directory in PowerShell:

```powershell
java -version
javac -version
.\mvnw.cmd compile
java -cp target/classes com.assessment.waitlist.Main
.\mvnw.cmd test
```

If Java is not found, set `JAVA_HOME` to your JDK installation before running
the wrapper. On Linux/macOS, use `sh ./mvnw compile` and `sh ./mvnw test`.

The demo uses three seats and eight learners to show enrollment, prerequisite
rejection, duplicates, waitlist cancellation, eligibility changes, permanent
removal of an ineligible learner, reapplication, and FIFO promotion. It finishes
with D, E, and G enrolled, an empty waitlist, and zero available seats. It is a
scripted example rather than an interactive CLI.

The 26 JUnit tests are organized into six focused test classes. The latest AI-run suite passed all
26 tests, and the candidate also reported a successful independent test run.
Per-class results are generated under `target/surefire-reports`.

## Behavior

- Capacity must be positive. Enrollment and waitlist start empty.
- Learner IDs must be non-null and non-blank. IDs are case-sensitive and are
  not trimmed or normalized.
- A null eligibility map means no prerequisite restriction. With a supplied
  map, only a `true` value qualifies; false, null, and missing values fail.
  An empty supplied map qualifies nobody.
- Eligibility is copied at construction and can be changed through the
  synchronized `updateEligibility` operation. Updates preserve queue position
  and do not revoke existing enrollment. An update on an unrestricted offering
  throws `IllegalStateException`; an invalid ID throws `IllegalArgumentException`.
- Eligible learners enroll if there is space; otherwise they join the waitlist
  once. Duplicate requests preserve the existing state and queue position.
- Dropping an enrolled learner promotes the earliest eligible waiting learner.
  Ineligible entries encountered before that learner are permanently removed.
- Cancelling a waitlist entry does not trigger promotion. Repeated drops return
  `NOT_FOUND`; malformed IDs return `INVALID_REQUEST`.

See [DESIGN.md](DESIGN.md) for invariants and limitations and
[AI_USAGE.md](AI_USAGE.md) for assistance and verification evidence.
