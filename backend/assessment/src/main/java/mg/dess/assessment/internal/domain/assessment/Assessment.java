package mg.dess.assessment.internal.domain.assessment;

import java.math.BigDecimal;
import java.util.UUID;
import mg.dess.assessment.internal.domain.result.AssessmentResult;
import mg.dess.assessment.internal.domain.result.InvalidScoreException;
import mg.dess.assessment.internal.domain.result.ResultStatus;

/**
 * Aggregate root: what is evaluated (exam or test) and its maximum score.
 * Results are separate aggregates (many per assessment), so Assessment does not hold them;
 * it acts as their factory because it knows the maximum score.
 */
public class Assessment {

    private final UUID id;
    private final BigDecimal maxScore;

    public Assessment(UUID id, BigDecimal maxScore) {
        if (maxScore == null || maxScore.signum() <= 0) {
            throw new IllegalArgumentException("maxScore must be greater than 0");
        }
        this.id = id;
        this.maxScore = maxScore;
    }

    public UUID id() {
        return id;
    }

    public BigDecimal maxScore() {
        return maxScore;
    }

    public AssessmentResult scoreFor(UUID studentId, BigDecimal points) {
        if (points == null || points.signum() < 0 || points.compareTo(maxScore) > 0) {
            throw new InvalidScoreException(points, maxScore);
        }
        return AssessmentResult.scored(id, studentId, points);
    }

    /** Records ABSENT or EXCUSED. Use scoreFor for a scored result. */
    public AssessmentResult markAs(UUID studentId, ResultStatus status) {
        return AssessmentResult.withoutScore(id, studentId, status);
    }
}
