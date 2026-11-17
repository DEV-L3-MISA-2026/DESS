package mg.dess.platform.acceptance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** University acceptance scenario of the plan (section 10). */
class UniversityScenarioIT {

    @Test
    @Disabled("Enable once the API path is available (plan: Day 8)")
    void importProgramEnrollmentSessionsConflictsAndResults() {
        // 1. import config-samples/university.xml (campus, faculty, department)
        // 2. create a program and a version; enroll a student in that version
        // 3. group, offering, assignment, session, conflict checks
        // 4. assessment session and results
    }
}
