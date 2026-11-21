package com.dess.assessment.application.impl;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.dess.academic.application.CourseService;
import com.dess.academic.exception.CourseNotFoundException;
import com.dess.assessment.application.AssessmentService;
import com.dess.assessment.application.CreateAssessmentCommand;
import com.dess.assessment.application.spi.AssessmentRepository;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.assessment.exception.InvalidAssessmentException;

/**
 * @brief Application rules of the assessment definitions.
 *
 * <p>The course of an assessment is checked through the academic
 * application interface. The module never reads the academic tables
 * directly.</p>
 */
@ApplicationScoped
@Transactional
public class AssessmentServiceImpl implements AssessmentService
{
    private final AssessmentRepository repository;
    private final CourseService courseService;

    @Inject
    public AssessmentServiceImpl(AssessmentRepository repository, CourseService courseService)
    {
        this.repository = repository;
        this.courseService = courseService;
    }

    @Override
    public Assessment create(CreateAssessmentCommand command)
    {
        if (command == null)
        {
            throw new IllegalArgumentException("command is required");
        }
        try
        {
            courseService.get(command.courseId());
        }
        catch (CourseNotFoundException e)
        {
            throw new InvalidAssessmentException("course not found: " + command.courseId(), e);
        }
        return repository.save(Assessment.create(command.courseId(), command.title(),
                command.type(), command.maximumScore()));
    }

    @Override
    public Assessment get(UUID id)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("id is required");
        }
        return repository.findById(id)
                .orElseThrow(() -> new AssessmentNotFoundException("assessment not found: " + id));
    }

    @Override
    public List<Assessment> list()
    {
        return repository.findAll();
    }

    @Override
    public List<Assessment> listByCourse(UUID courseId)
    {
        if (courseId == null)
        {
            throw new IllegalArgumentException("courseId is required");
        }
        return repository.findByCourseId(courseId);
    }

    @Override
    public Assessment deactivate(UUID id)
    {
        Assessment assessment = get(id);
        return repository.save(assessment.deactivated());
    }
}
