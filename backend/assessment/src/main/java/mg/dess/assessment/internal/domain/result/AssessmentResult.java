package mg.dess.assessment.internal.domain.result;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregate root: the result of one student for one assessment.
 * SCORED carries points; ABSENT and EXCUSED carry none. Created through
 * Assessment.scoreFor / Assessment.markAs, which know the maximum score.
 * Add the JPA mapping (and audit fields) when implementing persistence.
 */
public class AssessmentResult {

    private UUID id;
    private UUID assessmentId;
    private UUID studentId;
    private ResultStatus status;
    private BigDecimal points;   // only when status == SCORED

    private AssessmentResult() {
        // reserved for the JPA mapping
    }

    public static AssessmentResult scored(UUID assessmentId, UUID studentId, BigDecimal points) {
        var result = new AssessmentResult();
        result.id = UUID.randomUUID();
        result.assessmentId = assessmentId;
        result.studentId = studentId;
        result.status = ResultStatus.SCORED;
        result.points = points;
        return result;
    }

    public static AssessmentResult withoutScore(UUID assessmentId, UUID studentId, ResultStatus status) {
        if (status == ResultStatus.SCORED) {
            throw new IllegalArgumentException("A SCORED result needs points");
        }
        var result = new AssessmentResult();
        result.id = UUID.randomUUID();
        result.assessmentId = assessmentId;
        result.studentId = studentId;
        result.status = status;
        return result;
    }

    public UUID id() { return id; }
    public UUID assessmentId() { return assessmentId; }
    public UUID studentId() { return studentId; }
    public ResultStatus status() { return status; }
    public BigDecimal points() { return points; }
}
