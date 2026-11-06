package com.assessment.waitlist;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class EnrollmentTestSupport {

    private EnrollmentTestSupport() {}

    static CourseEnrollmentService service() {
        return new CourseEnrollmentService(1,
                Map.of("A", true, "B", true, "C", true, "D", true));
    }

    static CourseEnrollmentService queued() {
        var service = service();
        assertEquals(EnrollmentResult.ENROLLED, service.enroll("A"));
        assertEquals(EnrollmentResult.WAITLISTED, service.enroll("B"));
        assertEquals(EnrollmentResult.WAITLISTED, service.enroll("C"));
        return service;
    }

    static void assertState(CourseEnrollmentService service, Set<String> enrolled,
                             List<String> waiting, int availableSeats) {
        assertAll(
                () -> assertEquals(enrolled, service.getEnrolledLearners()),
                () -> assertEquals(waiting, service.getWaitlistedLearners()),
                () -> assertEquals(availableSeats, service.getAvailableSeats()),
                () -> assertTrue(Collections.disjoint(
                        service.getEnrolledLearners(), service.getWaitlistedLearners())));
    }

}
