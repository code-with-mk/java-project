package com.assessment.waitlist;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static com.assessment.waitlist.EnrollmentTestSupport.*;

class DropPromotionTest {

    @Test
    void successiveDropsPromoteInFifoOrderInBothModes() {
        for (var service : List.of(service(), new CourseEnrollmentService(1, null))) {
            service.enroll("A");
            service.enroll("B");
            service.enroll("C");
            assertEquals(DropResult.DROPPED, service.drop("A"));
            assertState(service, Set.of("B"), List.of("C"), 0);
            assertEquals(DropResult.DROPPED, service.drop("B"));
            assertState(service, Set.of("C"), List.of(), 0);
        }
    }

    @Test
    void emptyWaitlistDropAndRepeatedDropAreSafeInBothModes() {
        for (var service : List.of(service(), new CourseEnrollmentService(1, null))) {
            service.enroll("A");
            assertEquals(DropResult.DROPPED, service.drop("A"));
            assertState(service, Set.of(), List.of(), 1);
            assertEquals(DropResult.NOT_FOUND, service.drop("A"));
            assertState(service, Set.of(), List.of(), 1);
        }
    }

    @Test
    void cancelledLearnerRejoinsAtTail() {
        var service = queued();
        assertEquals(DropResult.WAITLIST_CANCELLED, service.drop("B"));
        assertEquals(DropResult.NOT_FOUND, service.drop("B"));
        assertState(service, Set.of("A"), List.of("C"), 0);
        assertEquals(EnrollmentResult.WAITLISTED, service.enroll("B"));
        assertState(service, Set.of("A"), List.of("C", "B"), 0);
    }

    @Test
    void unknownDropPreservesState() {
        var service = queued();
        assertEquals(DropResult.NOT_FOUND, service.drop("unknown"));
        assertState(service, Set.of("A"), List.of("B", "C"), 0);
    }
}
