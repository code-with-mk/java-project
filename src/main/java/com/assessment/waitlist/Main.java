package com.assessment.waitlist;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** A scripted demonstration; all enrollment rules remain in the service. */
public class Main {
    public static void main(String[] args) {
        var service = new CourseEnrollmentService(3, Map.of(
                "A", true, "B", true, "C", true, "D", true,
                "E", true, "F", true, "G", true, "H", false));

        System.out.println("Course waitlist demo: 3 seats, 8 learners");
        for (String learner : List.of("A", "B", "C", "D", "E", "F", "G", "H")) {
            System.out.println("Enroll " + learner + ": " + service.enroll(learner));
        }
        printState(service);

        System.out.println("\nDuplicate requests preserve state and queue position:");
        System.out.println("Enroll A: " + service.enroll("A"));
        System.out.println("Enroll E: " + service.enroll("E"));

        System.out.println("\nD loses eligibility while waiting; F cancels:");
        service.updateEligibility("D", false);
        System.out.println("Drop F: " + service.drop("F"));
        printState(service);

        System.out.println("\nA drops: D is removed as ineligible and E is promoted:");
        System.out.println("Drop A: " + service.drop("A"));
        printState(service);

        System.out.println("\nD regains eligibility and reapplies behind G:");
        service.updateEligibility("D", true);
        System.out.println("Enroll D: " + service.enroll("D"));
        printState(service);

        System.out.println("\nB and C drop in turn: G, then D, are promoted:");
        for (String learner : List.of("B", "C")) {
            System.out.println("Drop " + learner + ": " + service.drop(learner));
            printState(service);
        }

        System.out.println("\nRepeated and invalid requests leave state unchanged:");
        System.out.println("Drop A again: " + service.drop("A"));
        System.out.println("Enroll blank ID: " + service.enroll(" "));
        printState(service);
    }

    private static void printState(CourseEnrollmentService service) {
        // Sorting enrollment is for readable output; waitlist order stays FIFO.
        System.out.println("  Enrolled: " + new TreeSet<>(service.getEnrolledLearners()));
        System.out.println("  Waitlist (first to last): " + service.getWaitlistedLearners());
        System.out.println("  Available seats: " + service.getAvailableSeats());
    }
}
