package com.assessment.waitlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static com.assessment.waitlist.EnrollmentTestSupport.*;

@Timeout(30)
class ConcurrencyTest {

    @Test
    void concurrentDistinctEnrollmentsNeverOverbook() throws Exception {
        var service = new CourseEnrollmentService(5, null);
        var jobs = IntStream.range(0, 100).mapToObj(i -> (Runnable) () -> {
            var result = service.enroll("L" + i);
            assertTrue(result == EnrollmentResult.ENROLLED || result == EnrollmentResult.WAITLISTED);
        }).toList();
        race(jobs);
        assertEquals(5, service.getEnrolledLearners().size());
        assertEquals(95, service.getWaitlistedLearners().size());
        assertEquals(95, Set.copyOf(service.getWaitlistedLearners()).size());
        assertEquals(0, service.getAvailableSeats());
        assertTrue(Collections.disjoint(service.getEnrolledLearners(), service.getWaitlistedLearners()));
    }

    @Test
    void concurrentDuplicateEnrollmentsCreateOneRecord() throws Exception {
        var service = new CourseEnrollmentService(1, null);
        var results = new java.util.concurrent.ConcurrentLinkedQueue<EnrollmentResult>();
        race(Collections.nCopies(20, () -> results.add(service.enroll("A"))));
        assertEquals(1, results.stream().filter(r -> r == EnrollmentResult.ENROLLED).count());
        assertEquals(19, results.stream().filter(r -> r == EnrollmentResult.ALREADY_ENROLLED).count());
        assertState(service, Set.of("A"), List.of(), 0);
    }

    @Test
    void concurrentDropAndArrivalPreserveWaitingPriority() throws Exception {
        for (int i = 0; i < 100; i++) {
            var service = queued();
            race(List.of(
                    () -> assertEquals(DropResult.DROPPED, service.drop("A")),
                    () -> assertEquals(EnrollmentResult.WAITLISTED, service.enroll("D"))));
            assertState(service, Set.of("B"), List.of("C", "D"), 0);
        }
    }

    @Test
    void concurrentEligibilityLossAndPromotionHaveSerializedOutcome() throws Exception {
        for (int i = 0; i < 100; i++) {
            var service = queued();
            race(List.of(
                    () -> assertEquals(DropResult.DROPPED, service.drop("A")),
                    () -> service.updateEligibility("B", false)));
            var enrolled = service.getEnrolledLearners();
            assertTrue(enrolled.equals(Set.of("B")) || enrolled.equals(Set.of("C")));
            assertState(service, enrolled, enrolled.contains("B") ? List.of("C") : List.of(), 0);
        }
    }

    @Test
    void concurrentDuplicateDropsPromoteOnlyOnce() throws Exception {
        var service = queued();
        var results = new java.util.concurrent.ConcurrentLinkedQueue<DropResult>();
        race(Collections.nCopies(2, () -> results.add(service.drop("A"))));
        assertEquals(1, results.stream().filter(r -> r == DropResult.DROPPED).count());
        assertEquals(1, results.stream().filter(r -> r == DropResult.NOT_FOUND).count());
        assertState(service, Set.of("B"), List.of("C"), 0);
    }

    // Ready/start gates overlap requests without sleeps; futures propagate worker failures.
    private void race(List<Runnable> jobs) throws Exception {
        var executor = Executors.newFixedThreadPool(jobs.size());
        var ready = new CountDownLatch(jobs.size());
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<?>>();
        try {
            for (var job : jobs) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new AssertionError("Timed out waiting for start gate");
                        }
                        job.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("Worker interrupted", e);
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers did not become ready");
            start.countDown();
            for (var future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Workers did not terminate");
        }
    }
}
