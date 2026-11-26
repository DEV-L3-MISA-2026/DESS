package com.dess.assessment.application.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.dess.assessment.application.CorrectResultCommand;
import com.dess.assessment.application.EligibilityService;
import com.dess.assessment.application.RecordResultCommand;
import com.dess.assessment.application.ResultService;
import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.application.spi.ResultCorrectionRepository;
import com.dess.assessment.application.spi.ResultRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.Result;
import com.dess.assessment.domain.ResultCorrection;
import com.dess.assessment.domain.ResultStatus;
import com.dess.assessment.presentation.EligibilityPresentation;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.assessment.exception.DuplicateResultException;
import com.dess.assessment.exception.IneligibleCandidateException;
import com.dess.assessment.exception.InvalidResultException;
import com.dess.assessment.exception.ResultNotFoundException;

/**
 * @brief Application rules of the result entry and retrieval.
 *
 * <p>A result is recorded only for an existing assessment and an
 * eligible student, and only once per assessment and student. The
 * score must fit the assessment maximum score. A correction keeps
 * the entry audit fields and only moves the update time, and it
 * appends one entry to the correction trail with the previous value,
 * the new value and who corrected it.</p>
 */
@ApplicationScoped
@Transactional
public class ResultServiceImpl implements ResultService
{
    private final ResultRepository results;
    private final AssessmentRepository assessments;
    private final EligibilityService eligibilityService;
    private final ResultCorrectionRepository corrections;

    @Inject
    public ResultServiceImpl(ResultRepository results, AssessmentRepository assessments,
                             EligibilityService eligibilityService,
                             ResultCorrectionRepository corrections)
    {
        this.results = results;
        this.assessments = assessments;
        this.eligibilityService = eligibilityService;
        this.corrections = corrections;
    }

    @Override
    public Result recordResult(RecordResultCommand command)
    {
        if (command == null)
        {
            throw new IllegalArgumentException("command is required");
        }
        Assessment assessment = assessments.findById(command.assessmentId())
                .orElseThrow(() -> new AssessmentNotFoundException(
                        "assessment not found: " + command.assessmentId()));
        EligibilityService.Eligibility eligibility =
                eligibilityService.check(command.assessmentId(), command.studentPersonId());
        if (!eligibility.eligible())
        {
            throw new IneligibleCandidateException("student " + command.studentPersonId()
                    + " is not eligible for assessment " + command.assessmentId() + ": "
                    + EligibilityPresentation.reasonMessages(eligibility,
                            command.studentPersonId()));
        }
        if (results.existsByAssessmentAndStudent(command.assessmentId(), command.studentPersonId()))
        {
            throw new DuplicateResultException("a result already exists for assessment "
                    + command.assessmentId() + " and student " + command.studentPersonId());
        }
        checkScoreAgainstMaximum(command.score(), command.status(), assessment);
        return results.save(Result.create(command.assessmentId(), command.examSessionId(),
                command.studentPersonId(), command.score(), command.status(), command.enteredBy()));
    }

    @Override
    public Result correctResult(UUID resultId, CorrectResultCommand command)
    {
        if (resultId == null)
        {
            throw new IllegalArgumentException("resultId is required");
        }
        if (command == null)
        {
            throw new IllegalArgumentException("command is required");
        }
        Result result = results.findById(resultId)
                .orElseThrow(() -> new ResultNotFoundException("result not found: " + resultId));
        Assessment assessment = assessments.findById(result.assessmentId())
                .orElseThrow(() -> new AssessmentNotFoundException(
                        "assessment not found: " + result.assessmentId()));
        checkScoreAgainstMaximum(command.score(), command.status(), assessment);
        Result corrected = result.corrected(command.score(), command.status());
        Result saved = results.save(corrected);
        corrections.save(ResultCorrection.create(result.id(), result.score(), result.status(),
                corrected.score(), corrected.status(), command.correctedBy(),
                corrected.updatedAt()));
        return saved;
    }

    @Override
    public Optional<Result> findById(UUID id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("id is required");
        }
        return results.findById(id);
    }

    @Override
    public Optional<Result> findByAssessmentAndStudent(UUID assessmentId, long studentPersonId)
    {
        if (assessmentId == null)
        {
            throw new IllegalArgumentException("assessmentId is required");
        }
        return results.findByAssessmentAndStudent(assessmentId, studentPersonId);
    }

    @Override
    public List<Result> listByAssessment(UUID assessmentId)
    {
        if (assessmentId == null)
        {
            throw new IllegalArgumentException("assessmentId is required");
        }
        return results.findByAssessment(assessmentId);
    }

    @Override
    public List<Result> listByStudent(long studentPersonId)
    {
        return results.findByStudent(studentPersonId);
    }

    @Override
    public List<ResultCorrection> correctionsFor(UUID resultId)
    {
        if (resultId == null)
        {
            throw new IllegalArgumentException("resultId is required");
        }
        return corrections.findByResultId(resultId);
    }

    @Override
    public ResultDetail detail(UUID id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("id is required");
        }
        Result result = results.findById(id)
                .orElseThrow(() -> new ResultNotFoundException("result not found: " + id));
        return new ResultDetail(result, corrections.findByResultId(id));
    }

    private void checkScoreAgainstMaximum(java.math.BigDecimal score, ResultStatus status,
                                          Assessment assessment)
    {
        if (status == ResultStatus.SCORED && score != null
                && score.compareTo(assessment.maximumScore()) > 0)
        {
            throw new InvalidResultException("score " + score
                    + " exceeds the assessment maximum score " + assessment.maximumScore());
        }
    }
}
