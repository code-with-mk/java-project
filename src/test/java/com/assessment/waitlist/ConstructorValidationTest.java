package com.assessment.waitlist;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static com.assessment.waitlist.EnrollmentTestSupport.*;

class ConstructorValidationTest {

    @Test
    void rejectsNonpositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CourseEnrollmentService(0, null));
        assertThrows(IllegalArgumentException.class, () -> new CourseEnrollmentService(-1, null));
    }

    @Test
    void startsEmptyWithAllSeatsAvailable() {
        assertState(service(), Set.of(), List.of(), 1);
    }

    @Test
    void rejectsInvalidIdsWithoutChangingState() {
        for (var service : List.of(queued(), new CourseEnrollmentService(1, null))) {
            var enrolled = service.getEnrolledLearners();
            var waiting = service.getWaitlistedLearners();
            int seats = service.getAvailableSeats();
            for (String id : new String[]{null, "", " ", "\t\n"}) {
                assertEquals(EnrollmentResult.INVALID_REQUEST, service.enroll(id));
                assertEquals(DropResult.INVALID_REQUEST, service.drop(id));
                assertThrows(IllegalArgumentException.class,
                        () -> service.updateEligibility(id, true));
                assertState(service, enrolled, waiting, seats);
            }
        }
    }

    @Test
    void defensivelyCopiesEligibility() {
        var eligibility = new HashMap<>(Map.of("A", true, "B", false));
        var service = new CourseEnrollmentService(1, eligibility);
        eligibility.put("A", false);
        eligibility.put("B", true);
        assertEquals(EnrollmentResult.PREREQUISITE_NOT_MET, service.enroll("B"));
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("A"));
        assertState(service, Set.of("A"), List.of(), 0);
    }
}
