package com.assessment.waitlist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class CourseEnrollmentService {

    private final Set<String> enrolledLearners;
    private final Set<String> waitlist;
    private final Map<String, Boolean> eligibility;
    private final int capacity;

    public CourseEnrollmentService(int capacity, Map<String, Boolean> eligibility) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be greater than zero");
        }

        this.capacity = capacity;
        // A null map means this offering has no prerequisite restriction.
        this.eligibility = eligibility == null ? null : new HashMap<>(eligibility);
        this.enrolledLearners = new HashSet<>();
        this.waitlist = new LinkedHashSet<>();
    }

    public synchronized EnrollmentResult enroll(String learnerId) {
        if (learnerId == null || learnerId.isBlank()) {
            return EnrollmentResult.INVALID_REQUEST;
        }

        if (enrolledLearners.contains(learnerId)) {
            return EnrollmentResult.ALREADY_ENROLLED;
        }

        if (waitlist.contains(learnerId)) {
            return EnrollmentResult.ALREADY_WAITLISTED;
        }

        if (eligibility != null && !Boolean.TRUE.equals(eligibility.get(learnerId))) {
            return EnrollmentResult.PREREQUISITE_NOT_MET;
        }

        if (enrolledLearners.size() < capacity) {
            enrolledLearners.add(learnerId);
            return EnrollmentResult.ENROLLED;
        } else {
            waitlist.add(learnerId);
            return EnrollmentResult.WAITLISTED;
        }
    }

    public synchronized DropResult drop(String learnerId) {
        if (learnerId == null || learnerId.isBlank()) {
            return DropResult.INVALID_REQUEST;
        }
        if (enrolledLearners.remove(learnerId)) {
            promoteNextEligible();
            return DropResult.DROPPED;
        } else if (waitlist.remove(learnerId)) {
            return DropResult.WAITLIST_CANCELLED;
        } else {
            return DropResult.NOT_FOUND;
        }
    }

    private void promoteNextEligible() {
        if (waitlist.isEmpty()) {
            return;
        }
        if (eligibility== null) {
            Optional<String> nextEligible = waitlist.stream().findFirst();
            waitlist.remove(nextEligible.get());
            enroll(nextEligible.get());
        } else {
           List<String> learners = getWaitlistedLearners();
           for (String learner : learners) {
               if (Boolean.TRUE.equals(eligibility.get(learner))) {
                  waitlist.remove(learner);
                  enroll(learner);
                  break;
               }else{
                   waitlist.remove(learner);
               }
           }
        }
    }

    public synchronized void updateEligibility(
        String learnerId,
        boolean eligible) {

    if (learnerId == null || learnerId.isBlank()) {
        throw new IllegalArgumentException("Invalid learner ID");
    }

    if(eligibility == null) {
        throw new IllegalStateException("No eligibility restrictions for this course offering");
    }

    if (eligible) {
        eligibility.put(learnerId, true);
    } else {
        eligibility.put(learnerId, false);
    }
}

    public synchronized Set<String> getEnrolledLearners() {
         return Set.copyOf(enrolledLearners);
    }

    public synchronized List<String> getWaitlistedLearners() {
        return waitlist.stream().toList();
    }

    public synchronized int getAvailableSeats() {
        return this.capacity - this.enrolledLearners.size();
    }

}
