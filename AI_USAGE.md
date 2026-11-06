# AI Assistance and Verification Record

## Summary

AI was used as an engineering mentor, code reviewer, testing assistant, and documentation tool during the Course Enrollment and Waitlist Management assessment.

The candidate was responsible for evaluating the requirements, selecting the application architecture, defining the enrollment and waitlist policies, and implementing the core service workflow.

The candidate developed and revised the enrollment, dropping, waitlist promotion, and eligibility-update logic. AI assisted by reviewing the implementation, identifying defects, explaining edge cases, and executing verification checks. The candidate evaluated the feedback and corrected the service logic.

The final automated suite contains 26 JUnit 5 tests across six test classes. The recorded AI-run Maven execution completed with 26 tests passed, zero failures, zero errors, and zero skipped tests.

The candidate subsequently reported independently running the tests and receiving a successful build. The candidate's exact command and console output were not provided, so this is recorded separately from AI-run verification.

## Design discussions and decisions

The candidate used AI to discuss alternative approaches and evaluate trade-offs. The final architecture, data structures, business rules, and concurrency policy were selected based on the candidate's assessment of the requirements.

**Technology stack and architecture**

The candidate selected Java 21, Maven, JUnit 5, and an in-memory service implementation.

Plain Java was chosen instead of Spring Boot because the assignment involves a single course offering and does not require REST endpoints, database persistence, or a frontend.

The candidate preferred a minimal service that could be compiled, executed, and tested independently.


### Project setup

The candidate requested:

> "add maven wrapper and verify java"

AI created the Java 21 Maven configuration with JUnit 5, compiler and test plugins, and the Maven Wrapper pinned to Maven 3.9.9.

The candidate selected this approach so reviewers could compile and run the project without installing Maven separately.

AI verified that Java and `javac` 25.0.2 were installed and that the project could target Java 21.

The initial wrapper build succeeded but had no permanent tests at that stage, so it was not treated as behavioral verification.

The candidate also requested:

> "created folder structure use same for test as well"

AI organized the test package to mirror the main source package.

These changes affected `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper`, `.gitignore`, the test package directory, and the two result enums.

### Prerequisite policy and targeted implementation

The candidate stated:

> "I would say eligibility can be null which would mean no criteria required"

AI adapted its advice to distinguish a null eligibility map, which means unrestricted enrollment, from an empty supplied map, which qualifies nobody.

The candidate's policy was retained instead of the earlier suggestion to reject a null constructor map.

### Review of candidate workflow changes

The candidate implemented the drop and promotion workflow and repeatedly requested code review using prompts such as:

> "check now"

> "test now"

AI identified several defects in intermediate versions of the implementation.

Promotion initially called `enroll()` while the learner was still waitlisted, causing the duplicate check to interfere with promotion.

An empty `Optional` could be accessed during a drop with an empty waitlist.

Another promotion branch removed the selected learner without successfully enrolling them.

AI also identified mutable snapshot results and incomplete drop outcomes.

The candidate revised these methods following the review.

The corrections included removing the selected learner from the waitlist before completing promotion, handling an empty waitlist safely, ensuring successful enrollment during promotion, and returning immutable snapshots.

Subsequent focused checks confirmed successful FIFO promotion, safe empty-waitlist and repeated-drop behavior, and immutable snapshots.

AI provided review findings and verification assistance. The candidate revised the workflow rather than replacing the complete service implementation.

### Changing eligibility and edge-case verification

During the design discussion, the candidate raised the question:

> "Decide whether eligibility can change while waiting; currently it’s fixed at construction"

The candidate selected mutable eligibility so that prerequisite status could change before promotion.

The candidate later requested:

> "now eligibilty might chnage when learner in waitlist, make changes in design doc"

The candidate added `updateEligibility` to the service.

AI helped document queue-position preservation, eligibility rechecking during promotion, permanent removal of ineligible entries, and the requirement to use the same synchronization boundary as enrollment and dropping.

The candidate then requested:

> "I have added the relevant code please test with all edge cases"

AI created a temporary 26-scenario verification harness in the ignored project cache.

The first execution passed 25 scenarios and failed one scenario involving an eligibility update on an unrestricted course.

AI initially tested a no-op expectation.

The candidate rejected that interpretation and selected explicit rejection using `IllegalStateException`.

The final JUnit test was aligned with that decision and verifies unchanged state and continued unrestricted enrollment.

Intermediate `NullPointerException` failures were genuine historical results, not final passing outcomes.

### Permanent JUnit tests and organization

The candidate requested:

> "write unit test cases using junit"

AI wrote 26 JUnit 5 tests covering the documented scenarios.

The candidate then requested:

> "too many tests in single file can we divide them"

AI reorganized the tests into six focused files without changing their assertions or the service logic.

The resulting files were:

- `ConstructorValidationTest`
- `EnrollmentTest`
- `DropPromotionTest`
- `EligibilityUpdateTest`
- `SnapshotTest`
- `ConcurrencyTest`

`EnrollmentTestSupport` contains shared fixtures and state assertions.

The candidate requested this organization to improve readability and maintainability.

The concurrency tests use ready and start latches, bounded waits, and futures that propagate worker failures.

They check concurrent distinct and duplicate enrollment, drop versus a new arrival, eligibility loss versus promotion, and duplicate drops.

Two race scenarios each run 100 iterations.

Successful finite runs provide evidence for the tested scenarios but do not prove all possible thread interleavings.

### Documentation and final record

The candidate requested documentation changes using prompts including:

> "update design doc"

AI created and revised `README.md`, `DESIGN.md`, and this record to describe setup, API behavior, invariants, assumptions, limitations, and actual verification results.

The candidate requested updates when documentation needed to reflect the selected implementation behavior, particularly eligibility handling and waitlist promotion.

The candidate also requested complete sentences and a professional assignment summary.

The documentation was revised to distinguish candidate implementation work from AI-authored configuration, tests, documentation, demonstration code, and targeted implementation fixes.

The candidate subsequently reported a successful independent test run.

The original conversations remain the source for additional prompts and responses. This file provides available exact prompts and relevant response summaries.

## Acceptance, edits, and rejected alternatives

The following decisions summarize actions and choices visible in the development discussions.

Retention of an AI-authored file indicates incorporation into the project; it does not imply a separate independent review that the candidate did not report.

| Prompt or discussion | AI suggestion or response summary | Decision and rationale | Files or decisions informed | Verification evidence |
|---|---|---|---|---|
| "add maven wrapper and verify java" | AI proposed Maven configuration and a project-local wrapper for repeatable commands. | The candidate accepted this setup by requesting its creation and wrapper installation. A global Maven installation was unnecessary. | `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper`, `.gitignore` | Wrapper compilation succeeded; later Maven runs passed all 26 tests. |
| "created folder structure use same for test as well" | AI proposed mirroring the main source package in the test directory. | The candidate selected the standard Maven package structure for readability. | Source and test directories | Test classes compiled in the expected package structure. |
| "lets create other classes" | AI supplied enum outcomes for enrollment and dropping. | The candidate retained the result enums and removed an unused promotion outcome. | `EnrollmentResult.java`, `DropResult.java` | Enums compiled for Java 21; JUnit checks service return values. |
| Enrollment and waitlist data structure discussions | AI explained collection alternatives and trade-offs. | The candidate selected `HashSet` for enrollment and `LinkedHashSet` for FIFO waitlist uniqueness and ordering. | `CourseEnrollmentService.java`, `DESIGN.md` | Enrollment, duplicate, FIFO, and promotion tests passed. |
| "I would say eligibility can be null which would mean no criteria required" | Earlier AI advice suggested rejecting null constructor eligibility data. | The candidate changed that policy. Null means no prerequisites; an empty supplied map qualifies nobody. | Constructor, enrollment logic, `README.md`, `DESIGN.md` | JUnit verifies unrestricted, empty, false, missing, and null-valued eligibility cases. |
 `CourseEnrollmentService.java` | Temporary checks and permanent constructor and enrollment tests passed. |
| "check now" and "test now" | AI identified broken promotion, unsafe empty-queue handling, and mutable snapshots. | The candidate edited the workflow after feedback. | Drop, promotion, and snapshot methods | Regression checks exposed intermediate failures; final tests passed. |
| "Decide whether eligibility can change while waiting; currently it’s fixed at construction" | AI discussed mutable eligibility and promotion revalidation. | The candidate selected eligibility updates while retaining FIFO order and promotion-time validation. | `updateEligibility`, `DESIGN.md` | Eligibility-update and promotion tests passed. |
| "I have added the relevant code please test with all edge cases" | The temporary harness initially expected unrestricted eligibility updates to do nothing. | The candidate selected explicit rejection using `IllegalStateException` instead of a no-op. | `updateEligibility`, `DESIGN.md`, `EligibilityUpdateTest.java` | Final JUnit test verifies the exception, unchanged state, and continued unrestricted behavior. |
| "write unit test cases using junit" | AI generated permanent JUnit tests. | The candidate incorporated the tests to verify the service implementation. | Test source files | All 26 tests passed. |
| "too many tests in single file can we divide them" | AI proposed grouping tests by behavior. | The candidate requested and accepted six focused test classes to improve maintainability. | Six test classes and `EnrollmentTestSupport.java` | Fresh compilation and execution passed all 26 tests after the split. |
| "update the code with this use bigger case than just 2" | AI supplied a scripted demonstration using three seats and eight learners. | The candidate requested a larger demonstration to exercise more service behavior. | `Main.java`, `README.md`, `DESIGN.md` | AI executed the demo and observed D, E, and G enrolled, no waiting learners, and zero available seats. |
| Documentation requests, including "update both the docs" | AI summarized implementation, test history, and known limitations. | The candidate requested complete sentences and assignment-ready documentation reflecting the final contract. | `README.md`, `DESIGN.md`, `AI_USAGE.md` | Documentation was checked against source, tests, and demo; the candidate separately reported a successful test build. |

AI assistance was accepted for Maven configuration, the Maven Wrapper, JUnit test generation, test organization, documentation, and verification.

The candidate modified the service workflow after AI review identified issues in dropping, promotion, empty-waitlist handling, and snapshot behavior.

The candidate rejected or did not select several alternatives, including Spring Boot, database persistence, a frontend, a separate mutable seat counter, a queue plus a second lookup collection, separate locking boundaries for dropping and promotion, and an unnecessary public promotion-result enum.

The candidate also rejected AI's initial no-op expectation for unrestricted eligibility updates.

These decisions reflect the candidate's preference for a minimal implementation with explicit behavior and testable state transitions.
