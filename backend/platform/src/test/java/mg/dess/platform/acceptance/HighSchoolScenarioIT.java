package mg.dess.platform.acceptance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** High-school acceptance scenario of the plan (section 10). */
class HighSchoolScenarioIT {

    @Test
    @Disabled("Enable once the API path is available (plan: Day 7)")
    void importSchedulesSessionsRejectsConflictsAndRecordsResults() {
        // 1. import config-samples/high-school.xml
        // 2. create period and group "Terminale A"; register students and a professor
        // 3. create Mathematics, an offering, a dated session; reserve room, teacher, group
        // 4. an overlapping session is rejected with a readable error
        // 5. create an assessment and an exam session; record a score and an absence; read results
    }
}
