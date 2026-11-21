package com.dess.assessment.application;

import java.util.List;
import java.util.UUID;

import com.dess.assessment.domain.Assessment;

/**
 * @brief Application API of the assessment definitions.
 *
 * <p>An assessment is a definition: what is evaluated, for which
 * course, with which maximum score. The exam sessions that hold the
 * assessment are owned by Christian, and the results are recorded
 * through {@link ResultService}.</p>
 */
public interface AssessmentService
{
    Assessment create(CreateAssessmentCommand command);

    Assessment get(UUID id);

    List<Assessment> list();

    List<Assessment> listByCourse(UUID courseId);

    Assessment deactivate(UUID id);
}
