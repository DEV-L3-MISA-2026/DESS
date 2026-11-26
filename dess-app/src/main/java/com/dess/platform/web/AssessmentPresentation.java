package com.dess.platform.web;

import com.dess.assessment.domain.Assessment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only projections of assessment definitions for the server-rendered pages.
 *
 * <p>Exam sessions store an assessment id. The page resolves it to the
 * assessment title when the definition still exists, and says so explicitly
 * when it does not.</p>
 */
public final class AssessmentPresentation
{
    /** Label shown when the stored assessment id has no matching definition. */
    public static final String UNKNOWN_LABEL = "Unknown assessment";

    private AssessmentPresentation()
    {
    }

    /**
     * Builds an id-to-title map in list order.
     *
     * @param assessments assessment definitions
     * @return a map from assessment id to title
     * @throws NullPointerException when the list or an entry is null
     */
    public static Map<UUID, String> labels(List<Assessment> assessments)
    {
        Objects.requireNonNull(assessments, "assessments is required");

        Map<UUID, String> labels = new LinkedHashMap<>();

        for (Assessment assessment : assessments)
        {
            Objects.requireNonNull(assessment, "an assessment is required");
            labels.put(assessment.id(), assessment.title());
        }

        return labels;
    }

    /**
     * @param assessmentId id stored on an exam session
     * @param labels map from assessment id to title
     * @return the title, or {@link #UNKNOWN_LABEL} when the id is not known
     * @throws NullPointerException when an argument is null
     */
    public static String label(UUID assessmentId, Map<UUID, String> labels)
    {
        Objects.requireNonNull(assessmentId, "assessmentId is required");
        Objects.requireNonNull(labels, "labels is required");

        return labels.getOrDefault(assessmentId, UNKNOWN_LABEL);
    }

    /**
     * @param type the kind of evaluation
     * @return the label shown in lists and forms, for example {@code Exam}
     * @throws NullPointerException when the type is null
     */
    public static String typeLabel(com.dess.assessment.domain.AssessmentType type)
    {
        Objects.requireNonNull(type, "type is required");
        return switch (type)
        {
            case EXAM -> "Exam";
            case TEST -> "Test";
            case PRACTICAL -> "Practical";
        };
    }
}
