# Design

## Boundary and state

`CourseEnrollmentService` owns one offering's capacity, eligibility, enrollment,
and waitlist. `enroll` returns `EnrollmentResult`; `drop` returns `DropResult`.
Snapshot getters expose enrolled IDs, waitlist order, and available seats.
The implementation uses Java 21, Maven, and JUnit 5 without a framework.

A `HashSet<String>` provides enrollment uniqueness and average constant-time
membership operations. A `LinkedHashSet<String>` provides waitlist uniqueness
and insertion order without maintaining a separate membership index. A copied
`Map<String, Boolean>` represents the prerequisite condition. Capacity is fixed;
available seats are derived from the enrolled set rather than a mutable counter.

## Validation and prerequisites

Reject nonpositive capacity. Reject null and blank learner IDs in public
operations. Other strings are accepted exactly as supplied, including surrounding
spaces; there is no learner registry or identity normalization.

Null eligibility means the prerequisite is not required. Otherwise only
`Boolean.TRUE` qualifies. Missing, false, and null values are ineligible. A
supplied empty map qualifies nobody. Constructor copying prevents external
mutation from changing service state.

Eligibility may change while a learner is waitlisted. The synchronized
`updateEligibility(String learnerId, boolean eligible)` operation updates an
individual learner's eligibility under the same lock as enrollment and dropping.
It rejects null or blank IDs with `IllegalArgumentException` and adds or replaces
the map entry. Changing the caller's original map is not an update.
The offering's prerequisite mode remains fixed: a null constructor map means
unrestricted enrollment throughout that instance's lifetime.

For an unrestricted offering, a valid-ID eligibility update must be rejected
with `IllegalStateException`, leaving enrollment, waitlist, and available seats
unchanged. Subsequent enrollment remains unrestricted. This operation is not a
no-op. Invalid IDs are checked first and still throw `IllegalArgumentException`.

An eligibility update does not change queue position or automatically drop an
already enrolled learner. A waitlisted learner who becomes ineligible remains
in place until promotion inspects their entry. Promotion rechecks the latest
eligibility, permanently removes ineligible entries it encounters, and continues
to the next learner. If eligibility becomes true again before inspection, that
learner retains their position. If the entry was already removed, the learner
must submit a new request and receives no restored priority.

Enrollment checks validity, existing enrollment, existing waitlisting, then
eligibility before modifying state. Duplicate results take precedence over
prerequisite evaluation and do not reorder the waitlist.

## Promotion and fairness

Dropping an enrolled learner removes them and invokes promotion before returning
`DROPPED`. If nobody is waiting, the seat remains free. Promotion traverses FIFO
order and permanently removes ineligible entries encountered before the first
eligible learner, then removes and enrolls that learner. Entries after the
promoted learner remain untouched. If all entries are ineligible, the waitlist
is emptied. Removed learners would need a new request to rejoin.

The restricted branch traverses an immutable ordered snapshot so removals from
the original set do not invalidate traversal. The selected learner is removed
before calling `enroll`, avoiding the duplicate-waitlist check.

Waitlist cancellation removes only the requested entry and does not trigger
promotion. A nonempty waitlist is created only when capacity is full; each
enrolled drop fills its vacancy before
releasing the lock when an eligible learner is waiting. This prevents a new
arrival from taking that seat. Eligibility updates must preserve this rule:
if seats are available, existing eligible waitlisted learners take priority over
new requests. Any vacancy filling triggered by an update must run under the
same lock and process the waitlist in FIFO order.

## Invariants and synchronization

- Enrollment size remains between zero and capacity.
- Enrollment and waitlist contain no duplicates and are disjoint.
- FIFO order is preserved among eligible waiting learners.
- Newly enrolled or promoted learners satisfy the prerequisite policy.
- Invalid, duplicate, and repeated requests preserve valid state.

All public state operations and snapshot creation synchronize on the service
instance. The private promotion helper runs under the calling `drop` lock.
Java monitors are reentrant, so the helper's calls to synchronized getters and
`enroll` do not release that lock. Drop and promotion form one atomic operation
relative to other callers on the same instance.

The eligibility-update operation synchronizes on this same monitor.
An update racing with promotion therefore takes effect either before or after
the atomic drop/promotion operation. If promotion happens first, a subsequent
loss of eligibility does not revoke that enrollment.

Enrolled snapshots use `Set.copyOf`; waitlist snapshots use `Stream.toList` to
provide immutable results while preserving FIFO order. Separate getter calls
are separate observations and may see different moments under concurrency.

## Limits and verification still needed

State is lost when the process ends. There is no persistence, distributed
coordination, interactive CLI, or cross-instance locking. A scripted `Main`
demonstrates three seats and eight learners using the service API without
duplicating business rules. The service has package visibility.
Promotion may scan the whole queue and the restricted branch allocates a
snapshot; this is acceptable for the bounded in-memory feature.

The unrestricted-update contract is implemented with IllegalStateException and
is covered by a permanent JUnit test. The suite contains 26 tests across six
focused classes, including concurrency checks. The AI-run suite passed with
zero failures, errors, or skips after a fresh compilation. The candidate also
reported a successful independent test run. Repeated race checks do not prove
all possible thread interleavings, and independent code review remains the
candidate's responsibility.

## Executed scenario checks

The following scenarios are covered by the permanent JUnit 5 suite. All 26 tests
passed in the latest AI-run execution on November 6, 2026. The initial temporary
harness passed 25 scenarios and failed the unrestricted-update case; that
historical result and its resolution are recorded in AI_USAGE.md. The permanent
suite uses the final explicit-rejection contract and supersedes the old no-op
assertion. Sources target Java 21; AI execution used JDK 25.0.2.

| # | Scenario and expected behavior | Result |
| --- | --- | --- |
| 1 | Zero and negative capacity throw IllegalArgumentException. | Pass |
| 2 | Initial enrollment/waitlist empty; all seats available. | Pass |
| 3 | Null, empty, space-only, and tab/newline IDs rejected by enrollment, drop, and update without changing state. | Pass |
| 4 | Null eligibility map allows an unknown valid ID to enroll. | Pass |
| 5 | Empty supplied map rejects enrollment and preserves state. | Pass |
| 6 | False, missing, and null eligibility values reject enrollment. | Pass |
| 7 | Mutating the original eligibility map does not change the service copy. | Pass |
| 8 | Full capacity waitlists once; duplicate enrollment/waitlisting preserves order and capacity. | Pass |
| 9 | Successive enrolled drops promote waiting learners in FIFO order. | Pass |
| 10 | Empty-waitlist drops succeed; repeated drops return NOT_FOUND, with and without prerequisites. | Pass |
| 11 | Waitlist cancellation succeeds once; rejoining goes to the tail. | Pass |
| 12 | Unknown learner drop returns NOT_FOUND without state changes. | Pass |
| 13 | Eligibility loss preserves queue position until inspection; duplicate request stays ALREADY_WAITLISTED; promotion removes the ineligible head and enrolls the next learner. | Pass |
| 14 | If all waiting learners lose eligibility, promotion removes them and leaves the seat free. | Pass |
| 15 | Eligibility restored before inspection preserves original FIFO priority. | Pass |
| 16 | Removed learner who regains eligibility must reapply and joins the tail. | Pass |
| 17 | Eligibility loss does not revoke enrollment; after dropping, a new enrollment request fails eligibility. | Pass |
| 18 | Updating an unknown learner creates eligibility; repeating the update is safe. | Pass |
| 19 | A valid-ID eligibility update on an unrestricted offering throws IllegalStateException; state remains unchanged and subsequent enrollment remains unrestricted. | Pass |
| 20 | Enrollment and waitlist snapshots reject mutation and remain unchanged after later service operations. | Pass |
| 21 | Case differences and surrounding spaces identify distinct nonblank IDs. | Pass |
| 22 | 100 simultaneous distinct requests for 5 seats yield 5 enrolled and 95 waitlisted, no overlap or overbooking. | Pass |
| 23 | 20 simultaneous requests for the same learner create one enrollment. | Pass |
| 24 | Drop racing with a new arrival preserves existing FIFO priority (100 iterations). | Pass |
| 25 | Eligibility loss racing with promotion produces a valid serialized outcome (100 iterations): either the learner was promoted before the update or skipped after it. | Pass |
| 26 | Two concurrent drops of the same enrolled learner promote only once. | Pass |
