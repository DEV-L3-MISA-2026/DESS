package mg.dess.assessment.internal.domain.result;

import java.math.BigDecimal;
import mg.dess.shared.domain.DomainException;

public class InvalidScoreException extends DomainException {

    public InvalidScoreException(BigDecimal points, BigDecimal maxScore) {
        super("INVALID_SCORE", "Score " + points + " must be between 0 and " + maxScore + ".");
    }
}
