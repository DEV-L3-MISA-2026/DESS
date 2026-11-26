package com.dess.assessment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * @brief One entry of the correction history of a result.
 *
 * <p>A correction never rewrites history: the result row keeps only
 * the latest score and status, while every correction is kept here
 * with the previous value, the new value, who corrected it and when.
 * The original entered value stays readable as the previous value
 * of the first entry of the trail.</p>
 *
 * <p>Rules of the model:</p>
 * <ul>
 *   <li>The previous and the new value each follow the result
 *       score/status rule: a SCORED value carries a score, an ABSENT
 *       or EXCUSED value carries no score.</li>
 *   <li>A score, when present, is at least zero and has at most two
 *       decimal places. The upper bound is the assessment maximum
 *       score, checked by the result service before the correction
 *       is recorded.</li>
 *   <li>The corrector is required. It is who corrected the result,
 *       never who entered it.</li>
 *   <li>Instants are stored in UTC. Any offset given by a caller is
 *       converted to UTC on construction.</li>
 * </ul>
 */
public record ResultCorrection(UUID id, UUID resultId, BigDecimal previousScore,
                               ResultStatus previousStatus, BigDecimal newScore,
                               ResultStatus newStatus, String correctedBy,
                               OffsetDateTime correctedAt)
{
    public ResultCorrection
    {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(resultId, "resultId is required");
        Objects.requireNonNull(previousStatus, "previousStatus is required");
        Objects.requireNonNull(newStatus, "newStatus is required");
        checkValue(previousScore, previousStatus, "previous");
        checkValue(newScore, newStatus, "new");
        if (correctedBy == null || correctedBy.isBlank())
        {
            throw new IllegalArgumentException("correctedBy is required");
        }
        if (correctedBy.length() > 200)
        {
            throw new IllegalArgumentException("correctedBy must contain at most 200 characters");
        }
        Objects.requireNonNull(correctedAt, "correctedAt is required");
        correctedBy = correctedBy.trim();
        correctedAt = correctedAt.withOffsetSameInstant(ZoneOffset.UTC);
    }

    private static void checkValue(BigDecimal score, ResultStatus status, String which)
    {
        if (status == ResultStatus.SCORED)
        {
            if (score == null)
            {
                throw new IllegalArgumentException(
                        "score is required for a SCORED " + which + " value");
            }
            if (score.compareTo(BigDecimal.ZERO) < 0)
            {
                throw new IllegalArgumentException(
                        "score must be at least zero for a " + which + " value");
            }
            if (score.scale() > 2)
            {
                throw new IllegalArgumentException(
                        "score must have at most two decimal places for a " + which + " value");
            }
        }
        else if (score != null)
        {
            throw new IllegalArgumentException(
                    "score must be empty for an ABSENT or EXCUSED " + which + " value");
        }
    }

    /**
     * @brief Creates a correction entry recorded now.
     *
     * @param resultId the corrected result
     * @param previousScore the score before the correction, or null
     * @param previousStatus the status before the correction
     * @param newScore the score after the correction, or null
     * @param newStatus the status after the correction
     * @param correctedBy who corrected the result
     * @param correctedAt when the correction happened, normally the
     *                    update time of the corrected result
     * @return the new correction entry
     */
    public static ResultCorrection create(UUID resultId, BigDecimal previousScore,
                                          ResultStatus previousStatus, BigDecimal newScore,
                                          ResultStatus newStatus, String correctedBy,
                                          OffsetDateTime correctedAt)
    {
        return new ResultCorrection(UUID.randomUUID(), resultId, previousScore, previousStatus,
                newScore, newStatus, correctedBy, correctedAt);
    }
}
