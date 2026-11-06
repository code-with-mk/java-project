package com.assessment.waitlist;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static com.assessment.waitlist.EnrollmentTestSupport.*;

class SnapshotTest {

    @Test
    void snapshotsAreImmutableAndDetached() {
        var service = queued();
        var enrolled = service.getEnrolledLearners();
        var waiting = service.getWaitlistedLearners();
        assertThrows(UnsupportedOperationException.class, enrolled::clear);
        assertThrows(UnsupportedOperationException.class, waiting::clear);
        service.drop("A");
        assertEquals(Set.of("A"), enrolled);
        assertEquals(List.of("B", "C"), waiting);
        assertState(service, Set.of("B"), List.of("C"), 0);
    }
}
