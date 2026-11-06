package com.assessment.waitlist;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static com.assessment.waitlist.EnrollmentTestSupport.*;

class EligibilityUpdateTest {

    @Test
    void eligibilityLossRemovesHeadAtPromotionAndPromotesNext() {
        var service = queued();
        service.updateEligibility("B", false);
        assertState(service, Set.of("A"), List.of("B", "C"), 0);
        assertEquals(EnrollmentResult.ALREADY_WAITLISTED, service.enroll("B"));
        assertEquals(DropResult.DROPPED, service.drop("A"));
        assertState(service, Set.of("C"), List.of(), 0);
        assertEquals(EnrollmentResult.PREREQUISITE_NOT_MET, service.enroll("B"));
    }

    @Test
    void allIneligibleWaitingLearnersAreRemoved() {
        var service = queued();
        service.updateEligibility("B", false);
        service.updateEligibility("C", false);
        assertEquals(DropResult.DROPPED, service.drop("A"));
        assertState(service, Set.of(), List.of(), 1);
    }

    @Test
    void restoredEligibilityBeforeInspectionKeepsPriority() {
        var service = queued();
        service.updateEligibility("B", false);
        service.updateEligibility("B", true);
        assertState(service, Set.of("A"), List.of("B", "C"), 0);
        service.drop("A");
        assertState(service, Set.of("B"), List.of("C"), 0);
    }

    @Test
    void removedLearnerMustReapplyAtTail() {
        var service = queued();
        service.enroll("D");
        service.updateEligibility("B", false);
        service.drop("A");
        service.updateEligibility("B", true);
        assertState(service, Set.of("C"), List.of("D"), 0);
        assertEquals(EnrollmentResult.WAITLISTED, service.enroll("B"));
        assertState(service, Set.of("C"), List.of("D", "B"), 0);
    }

    @Test
    void eligibilityLossDoesNotRevokeExistingEnrollment() {
        var service = service();
        service.enroll("A");
        service.updateEligibility("A", false);
        assertEquals(EnrollmentResult.ALREADY_ENROLLED, service.enroll("A"));
        assertState(service, Set.of("A"), List.of(), 0);
        service.drop("A");
        assertEquals(EnrollmentResult.PREREQUISITE_NOT_MET, service.enroll("A"));
        assertState(service, Set.of(), List.of(), 1);
    }

    @Test
    void unknownLearnerEligibilityCanBeAddedAndUpdatedRepeatedly() {
        var service = new CourseEnrollmentService(1, Map.of());
        service.updateEligibility("X", true);
        service.updateEligibility("X", true);
        assertState(service, Set.of(), List.of(), 1);
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("X"));
    }

    @Test
    void unrestrictedOfferingRejectsUpdatesWithoutChangingState() {
        var service = new CourseEnrollmentService(1, null);
        service.enroll("A");
        service.enroll("B");
        for (boolean eligible : new boolean[]{false, true}) {
            assertThrows(IllegalStateException.class,
                    () -> service.updateEligibility("B", eligible));
            assertState(service, Set.of("A"), List.of("B"), 0);
        }
        service.drop("A");
        assertState(service, Set.of("B"), List.of(), 0);
        assertEquals(EnrollmentResult.WAITLISTED, service.enroll("unknown"));
    }
}
