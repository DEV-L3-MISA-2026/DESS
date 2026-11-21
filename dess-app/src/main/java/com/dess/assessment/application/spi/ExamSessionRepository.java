package com.dess.assessment.application.spi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dess.assessment.domain.ExamSession;

/**
 * @brief Persistence port of the assessment module.
 *
 * <p>The application layer uses this interface; the JPA adapter lives in the
 * infrastructure package. No other module accesses the exam session tables.</p>
 */
public interface ExamSessionRepository
{
    /**
     * @param session session to insert or update
     * @return the stored session
     */
    ExamSession save(ExamSession session);

    /**
     * @param id session id
     * @return the stored session, or an empty result
     */
    Optional<ExamSession> findById(UUID id);

    /**
     * @return all stored sessions
     */
    List<ExamSession> findAll();

    /**
     * @param id id of the session to remove
     */
    void delete(UUID id);
}
