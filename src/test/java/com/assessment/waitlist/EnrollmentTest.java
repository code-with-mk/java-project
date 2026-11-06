package com.assessment.waitlist;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static com.assessment.waitlist.EnrollmentTestSupport.*;

class EnrollmentTest {

    @Test
    void nullEligibilityAllowsUnknownLearner() {
        var service = new CourseEnrollmentService(1, null);
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("unknown"));
        assertState(service, Set.of("unknown"), List.of(), 0);
    }

    @Test
    void emptyEligibilityRejectsEnrollment() {
        var service = new CourseEnrollmentService(1, Map.of());
        assertEquals(EnrollmentResult.PREREQUISITE_NOT_MET, service.enroll("A"));
        assertState(service, Set.of(), List.of(), 1);
    }

    @Test
    void falseMissingAndNullEligibilityRejectWithoutWaitlisting() {
        var eligibility = new HashMap<String, Boolean>();
        eligibility.put("A", false);
        eligibility.put("B", null);
        var service = new CourseEnrollmentService(1, eligibility);
        for (String id : List.of("A", "B", "unknown")) {
            assertEquals(EnrollmentResult.PREREQUISITE_NOT_MET, service.enroll(id));
        }
        assertState(service, Set.of(), List.of(), 1);
    }

    @Test
    void fullCapacityAndDuplicatesPreserveOrder() {
        var service = queued();
        assertEquals(EnrollmentResult.ALREADY_ENROLLED, service.enroll("A"));
        assertEquals(EnrollmentResult.ALREADY_WAITLISTED, service.enroll("B"));
        assertState(service, Set.of("A"), List.of("B", "C"), 0);
    }

    @Test
    void idsAreCaseSensitiveAndNotTrimmed() {
        var service = new CourseEnrollmentService(3, null);
        for (String id : List.of("A", "a", " A ")) {
            assertEquals(EnrollmentResult.ENROLLED, service.enroll(id));
        }
        assertState(service, Set.of("A", "a", " A "), List.of(), 0);
    }
}
