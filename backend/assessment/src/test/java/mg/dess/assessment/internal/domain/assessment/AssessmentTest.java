package mg.dess.assessment.internal.domain.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import mg.dess.assessment.internal.domain.result.AssessmentResult;
import mg.dess.assessment.internal.domain.result.InvalidScoreException;
import mg.dess.assessment.internal.domain.result.ResultStatus;
import org.junit.jupiter.api.Test;

class AssessmentTest {

    private final Assessment exam = new Assessment(UUID.randomUUID(), new BigDecimal("20"));
    private final UUID student = UUID.randomUUID();

    @Test
    void scoreWithinRangeIsRecorded() {
        AssessmentResult result = exam.scoreFor(student, new BigDecimal("14.5"));
        assertEquals(ResultStatus.SCORED, result.status());
        assertEquals(new BigDecimal("14.5"), result.points());
    }

    @Test
    void scoreAboveMaximumIsRejected() {
        assertThrows(InvalidScoreException.class, () -> exam.scoreFor(student, new BigDecimal("21")));
    }

    @Test
    void negativeScoreIsRejected() {
        assertThrows(InvalidScoreException.class, () -> exam.scoreFor(student, new BigDecimal("-1")));
    }

    @Test
    void absenceHasNoScore() {
        AssessmentResult result = exam.markAs(student, ResultStatus.ABSENT);
        assertEquals(ResultStatus.ABSENT, result.status());
        assertNull(result.points());
    }

    @Test
    void scoredStatusCannotBeRecordedWithoutPoints() {
        assertThrows(IllegalArgumentException.class, () -> exam.markAs(student, ResultStatus.SCORED));
    }

    @Test
    void maximumScoreMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Assessment(UUID.randomUUID(), BigDecimal.ZERO));
    }
}
