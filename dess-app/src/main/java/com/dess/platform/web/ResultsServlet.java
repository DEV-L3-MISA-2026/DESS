package com.dess.platform.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.dess.academic.application.CourseService;
import com.dess.academic.exception.CourseNotFoundException;
import com.dess.assessment.application.AssessmentService;
import com.dess.assessment.application.CorrectResultCommand;
import com.dess.assessment.application.ExamSessionService;
import com.dess.assessment.application.RecordResultCommand;
import com.dess.assessment.application.ResultService;
import com.dess.assessment.domain.Assessment;
import com.dess.assessment.domain.ExamSession;
import com.dess.assessment.domain.Result;
import com.dess.assessment.domain.ResultStatus;
import com.dess.assessment.exception.AssessmentException;
import com.dess.assessment.exception.AssessmentNotFoundException;
import com.dess.people.application.PeopleService;
import com.dess.platform.web.view.Badge;
import com.dess.platform.web.view.CorrectionRow;
import com.dess.platform.web.view.ResultEntry;
import com.dess.platform.web.view.ResultEntryRow;
import com.dess.platform.web.view.ResultRow;
import com.dess.platform.web.view.ResultsView;
import com.dess.platform.web.view.Views;
import com.dess.scheduling.application.RoomService;
import com.dess.security.application.AuthorizationService;
import com.dess.security.domain.RoleCatalog;
import com.dess.security.web.SessionUser;

/**
 * Results by assessment.
 *
 * <p>The page lists each exam session with the number of results recorded for
 * its assessment. Opening a session shows its candidates with the stored
 * result only, and the entry form for a mark or a correction. A mark is
 * never shown unless the server stored it, and an invalid mark returns 400
 * with the entered value kept.</p>
 *
 * <p>There is no average, weighting, ranking or retake here: those are
 * post-MVP and stay out of this page.</p>
 */
@WebServlet("/app/results")
public class ResultsServlet extends HttpServlet
{
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    private ExamSessionService examSessionService;

    @Inject
    private RoomService roomService;

    @Inject
    private ResultService resultService;

    @Inject
    private AssessmentService assessmentService;

    @Inject
    private CourseService courseService;

    @Inject
    private PeopleService peopleService;

    @Inject
    private AuthorizationService authorizationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        showPage(request, response, Web.form(), Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");

        if (!Web.requirePermission(request, response, authorizationService,
                RoleCatalog.ACADEMIC_WRITE))
        {
            return;
        }

        String action = Web.param(request, "action");

        switch (action)
        {
            case "recordResult" -> recordResult(request, response);
            case "correctResult" -> correctResult(request, response);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
        }
    }

    private void recordResult(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = readEntryForm(request);
        Assessment assessment = assessmentForForm(form, request, response);

        if (assessment == null)
        {
            return;
        }

        Map<String, String> errors = new LinkedHashMap<>();
        ParsedEntryForm parsed = parseEntryForm(form, errors, assessment.maximumScore());

        if (!errors.isEmpty() || parsed == null)
        {
            renderEntryError(request, response, form, errors);
            return;
        }

        try
        {
            resultService.recordResult(new RecordResultCommand(parsed.assessmentId(),
                    parsed.sessionId(), parsed.studentPersonId(), parsed.score(),
                    parsed.status(), currentUserName(request)));
            Web.flash(request, "success", "The result was recorded.");
            response.sendRedirect(request.getContextPath() + "/app/results?session="
                    + parsed.sessionId());
        }
        catch (AssessmentException | IllegalArgumentException exception)
        {
            putFormError(errors, exception.getMessage());
            renderEntryError(request, response, form, errors);
        }
    }

    private void correctResult(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Map<String, String> form = readEntryForm(request);
        Assessment assessment = assessmentForForm(form, request, response);

        if (assessment == null)
        {
            return;
        }

        Map<String, String> errors = new LinkedHashMap<>();
        ParsedEntryForm parsed = parseEntryForm(form, errors, assessment.maximumScore());
        String resultValue = form.getOrDefault("resultId", "");
        UUID resultId = null;

        if (resultValue.isBlank())
        {
            putFormError(errors, "Correct an existing result: no stored result was given.");
        }
        else
        {
            try
            {
                resultId = UUID.fromString(resultValue);
            }
            catch (IllegalArgumentException exception)
            {
                putFormError(errors, "Correct an existing result: the stored result id is not valid.");
            }
        }

        if (!errors.isEmpty() || parsed == null)
        {
            renderEntryError(request, response, form, errors);
            return;
        }

        try
        {
            resultService.correctResult(resultId, new CorrectResultCommand(parsed.score(),
                    parsed.status(), currentUserName(request)));
            Web.flash(request, "success", "The result was corrected.");
            response.sendRedirect(request.getContextPath() + "/app/results?session="
                    + parsed.sessionId());
        }
        catch (AssessmentException | IllegalArgumentException exception)
        {
            putFormError(errors, exception.getMessage());
            renderEntryError(request, response, form, errors);
        }
    }

    /**
     * @brief Resolves the assessment of the submitted entry form.
     *
     * <p>A missing or unknown assessment cannot be rendered with its maximum
     * score, so the error is sent directly.</p>
     *
     * @param form submitted values
     * @param request current request
     * @param response current response
     * @return the assessment, or {@code null} when the error was already sent
     * @throws IOException when the error response cannot be written
     */
    private Assessment assessmentForForm(Map<String, String> form, HttpServletRequest request,
                                         HttpServletResponse response) throws IOException
    {
        String assessmentValue = form.getOrDefault("assessmentId", "");

        try
        {
            UUID assessmentId = UUID.fromString(assessmentValue);
            return assessmentService.get(assessmentId);
        }
        catch (IllegalArgumentException exception)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Enter a valid assessment id.");
            return null;
        }
        catch (AssessmentNotFoundException exception)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
            return null;
        }
    }

    private Map<String, String> readEntryForm(HttpServletRequest request)
    {
        Map<String, String> form = Web.form();
        form.put("assessmentId", Web.param(request, "assessmentId"));
        form.put("sessionId", Web.param(request, "sessionId"));
        form.put("studentPersonId", Web.param(request, "studentPersonId"));
        form.put("resultId", Web.param(request, "resultId"));
        form.put("score", Web.param(request, "score"));
        form.put("status", Web.param(request, "status"));
        return form;
    }

    private String currentUserName(HttpServletRequest request)
    {
        SessionUser user = Web.currentUser(request);

        if (user == null)
        {
            return "";
        }

        return user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName();
    }

    /**
     * @brief Validates the submitted result entry values.
     *
     * <p>The mark is checked against the assessment maximum score here, so an
     * invalid mark returns 400 with the entered value kept. The service
     * checks it again before saving; the page never trusts its own check
     * alone. Score and status errors are keyed by the DOM id of the row
     * they belong to, for example {@code score-12}; errors on the hidden
     * fields go under {@code form} and are shown in the page alert.</p>
     *
     * @param form submitted values
     * @param errors map that receives one message per invalid field, keyed by
     *               the field DOM id
     * @param maximumScore the highest mark the assessment accepts
     * @return the parsed values, or {@code null} when any field is invalid
     */
    static ParsedEntryForm parseEntryForm(Map<String, String> form, Map<String, String> errors,
                                          BigDecimal maximumScore)
    {
        String studentValue = form.getOrDefault("studentPersonId", "");
        long studentPersonId = 0;

        if (studentValue.isBlank())
        {
            putFormError(errors, "Choose a candidate.");
        }
        else
        {
            try
            {
                studentPersonId = Long.parseLong(studentValue);
                if (studentPersonId <= 0)
                {
                    throw new NumberFormatException("person id must be positive");
                }
            }
            catch (NumberFormatException exception)
            {
                putFormError(errors, "Enter a valid candidate id.");
            }
        }

        UUID assessmentId = parseUuidField(form, errors, "assessmentId");
        UUID sessionId = parseUuidField(form, errors, "sessionId");

        ResultStatus status = null;
        String statusValue = form.getOrDefault("status", "");

        // The DOM id of the row this submission belongs to, when known.
        String rowSuffix = studentPersonId > 0 ? "-" + studentPersonId : "";
        String scoreKey = studentPersonId > 0 ? "score" + rowSuffix : "form";
        String statusKey = studentPersonId > 0 ? "status" + rowSuffix : "form";

        if (statusValue.isBlank())
        {
            errors.put(statusKey, "Choose a status.");
        }
        else
        {
            try
            {
                status = ResultStatus.valueOf(statusValue);
            }
            catch (IllegalArgumentException exception)
            {
                errors.put(statusKey, "Choose a valid status.");
            }
        }

        BigDecimal score = null;
        String scoreValue = form.getOrDefault("score", "");

        if (status == ResultStatus.SCORED)
        {
            if (scoreValue.isBlank())
            {
                errors.put(scoreKey, "Enter a mark.");
            }
            else
            {
                score = parseScore(scoreValue, maximumScore, scoreKey, errors);
            }
        }
        else if (!scoreValue.isBlank())
        {
            errors.put(scoreKey,
                    "Remove the mark: an absent or excused result carries no mark.");
        }

        if (!errors.isEmpty())
        {
            return null;
        }

        return new ParsedEntryForm(assessmentId, sessionId, studentPersonId, score, status);
    }

    private static UUID parseUuidField(Map<String, String> form, Map<String, String> errors,
                                       String field)
    {
        String value = form.getOrDefault(field, "");

        if (value.isBlank())
        {
            putFormError(errors, "Enter an id.");
            return null;
        }
        try
        {
            return UUID.fromString(value);
        }
        catch (IllegalArgumentException exception)
        {
            putFormError(errors, "Enter a valid id.");
            return null;
        }
    }

    private static BigDecimal parseScore(String scoreValue, BigDecimal maximumScore,
                                         String scoreKey, Map<String, String> errors)
    {
        BigDecimal score;

        try
        {
            score = new BigDecimal(scoreValue);
        }
        catch (NumberFormatException exception)
        {
            errors.put(scoreKey, "Enter a number for the mark.");
            return null;
        }

        if (score.compareTo(BigDecimal.ZERO) < 0)
        {
            errors.put(scoreKey, "The mark must be at least zero.");
            return null;
        }
        if (score.scale() > 2)
        {
            errors.put(scoreKey, "The mark must have at most two decimal places.");
            return null;
        }
        if (maximumScore != null && score.compareTo(maximumScore) > 0)
        {
            errors.put(scoreKey, "The mark must be at most " + maximumScore.toPlainString() + ".");
            return null;
        }

        return score;
    }

    /**
     * @brief Adds a message under the {@code form} key without dropping an
     *        earlier one.
     */
    private static void putFormError(Map<String, String> errors, String message)
    {
        errors.merge("form", message, (first, second) -> first + " " + second);
    }

    /** @brief The validated values of the result entry form. */
    record ParsedEntryForm(UUID assessmentId, UUID sessionId, long studentPersonId,
                           BigDecimal score, ResultStatus status)
    {
    }

    /**
     * @brief Builds one entry row per candidate from the stored results.
     *
     * <p>The row shows the stored score and status only. The correction
     * trail comes from {@code ResultService.detail}, so the original value
     * stays readable after a correction.</p>
     *
     * @param session the exam session whose candidates are listed
     * @param assessmentId the assessment the results belong to
     * @param resultService the service that holds the stored results
     * @param peopleService the service that names the candidates
     * @return one row per candidate, in session order
     */
    static List<ResultEntryRow> buildEntryRows(ExamSession session, UUID assessmentId,
                                               ResultService resultService,
                                               PeopleService peopleService)
    {
        Map<Long, String> registrations = peopleService.listStudents().stream()
                .collect(Collectors.toMap(
                        profile -> profile.personId(),
                        profile -> profile.registrationNumber(),
                        (first, second) -> first));

        List<ResultEntryRow> rows = new ArrayList<>();

        for (long personId : session.candidatePersonIds())
        {
            Optional<Result> stored =
                    resultService.findByAssessmentAndStudent(assessmentId, personId);
            String name = peopleService.findById(personId)
                    .map(person -> person.firstName() + " " + person.lastName())
                    .orElse("Unknown person");

            if (stored.isEmpty())
            {
                rows.add(new ResultEntryRow(personId, name,
                        registrations.getOrDefault(personId, ""), "—",
                        Badge.of("off", "Not entered"), "", List.of()));
                continue;
            }

            Result result = stored.get();
            List<CorrectionRow> corrections = resultService.detail(result.id()).corrections()
                    .stream()
                    .map(correction -> new CorrectionRow(
                            scoreText(correction.previousScore()),
                            statusLabel(correction.previousStatus()),
                            scoreText(correction.newScore()),
                            statusLabel(correction.newStatus()),
                            correction.correctedBy(),
                            DATE_TIME.format(correction.correctedAt()
                                    .withOffsetSameInstant(ZoneOffset.UTC))))
                    .toList();

            rows.add(new ResultEntryRow(personId, name,
                    registrations.getOrDefault(personId, ""),
                    scoreText(result.score()), Badge.resultStatus(result.status()),
                    result.id().toString(), corrections));
        }

        return rows;
    }

    static String scoreText(BigDecimal score)
    {
        return score == null ? "—" : score.toPlainString();
    }

    static String statusLabel(ResultStatus status)
    {
        return switch (status)
        {
            case SCORED -> "Scored";
            case ABSENT -> "Absent";
            case EXCUSED -> "Excused";
        };
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response,
                          Map<String, String> entryForm, Map<String, String> entryErrors)
            throws ServletException, IOException
    {
        List<ExamSession> allSessions = examSessionService.findAll();

        Map<UUID, String> roomLabels = roomService.findAll().stream()
                .collect(Collectors.toMap(room -> room.id(),
                        room -> room.code() + " - " + room.name()));

        // Results belong to an assessment, not to a session. Count once per
        // assessment so two sessions of the same assessment share the number.
        Map<UUID, Integer> recordedByAssessment = new HashMap<>();
        for (ExamSession session : allSessions)
        {
            recordedByAssessment.computeIfAbsent(session.assessmentId(),
                    assessmentId -> resultService.listByAssessment(assessmentId).size());
        }

        List<ResultRow> sessions = allSessions.stream()
                .map(session -> {
                    ResultProgress progress = ResultProgress.of(
                            session.candidatePersonIds().size(),
                            recordedByAssessment.get(session.assessmentId()));
                    return new ResultRow(
                            session.id().toString(),
                            DATE_TIME.format(session.startsAt()
                                    .withOffsetSameInstant(ZoneOffset.UTC)),
                            DATE_TIME.format(session.endsAt()
                                    .withOffsetSameInstant(ZoneOffset.UTC)),
                            roomLabels.getOrDefault(session.roomId(), "Unknown room"),
                            session.candidatePersonIds().size(),
                            Badge.of(progress.tone(), progress.label()));
                })
                .toList();

        EntryResolution resolution = entrySection(request, response, allSessions, roomLabels,
                entryForm, entryErrors);

        if (resolution.errorSent())
        {
            return;
        }

        ResultsView view = new ResultsView(sessions, resolution.entry(), canWrite(request));

        Views.render(request, response, view);
    }

    /** @brief What {@link #entrySection} resolved: an entry section, none, or an error sent. */
    private record EntryResolution(ResultEntry entry, boolean errorSent)
    {
    }

    /**
     * @brief Resolves the entry section for the selected session, if any.
     *
     * @param request current request
     * @param allSessions every stored exam session
     * @param roomLabels room id to room label
     * @param entryForm submitted entry values, kept after a validation error
     * @param entryErrors entry field errors
     * @return the entry section, or no entry when no session is selected, with
     *         {@code errorSent} true when the error was already sent
     * @throws IOException when the error response cannot be written
     */
    private EntryResolution entrySection(HttpServletRequest request, HttpServletResponse response,
                                         List<ExamSession> allSessions,
                                         Map<UUID, String> roomLabels,
                                         Map<String, String> entryForm,
                                         Map<String, String> entryErrors) throws IOException
    {
        String sessionValue = Web.param(request, "session");

        if (sessionValue.isBlank() && entryForm.getOrDefault("sessionId", "").isBlank())
        {
            return new EntryResolution(null, false);
        }

        String sessionIdText = sessionValue.isBlank()
                ? entryForm.getOrDefault("sessionId", "") : sessionValue;
        UUID sessionId;

        try
        {
            sessionId = UUID.fromString(sessionIdText);
        }
        catch (IllegalArgumentException exception)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Enter a valid session id.");
            return new EntryResolution(null, true);
        }

        Optional<ExamSession> found = allSessions.stream()
                .filter(session -> session.id().equals(sessionId)).findFirst();

        if (found.isEmpty())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "No exam session exists with id " + sessionId + ".");
            return new EntryResolution(null, true);
        }

        ExamSession session = found.get();
        Assessment assessment = assessmentService.get(session.assessmentId());
        String courseLabel;

        try
        {
            var course = courseService.get(assessment.courseId());
            courseLabel = course.code() + " - " + course.title();
        }
        catch (CourseNotFoundException exception)
        {
            courseLabel = "Unknown course";
        }

        return new EntryResolution(new ResultEntry(session.id().toString(),
                assessment.id().toString(), assessment.title(),
                courseLabel, AssessmentPresentation.typeLabel(assessment.type()),
                assessment.maximumScore().toPlainString(),
                DATE_TIME.format(session.startsAt().withOffsetSameInstant(ZoneOffset.UTC)),
                DATE_TIME.format(session.endsAt().withOffsetSameInstant(ZoneOffset.UTC)),
                roomLabels.getOrDefault(session.roomId(), "Unknown room"),
                buildEntryRows(session, assessment.id(), resultService, peopleService),
                entryForm, entryErrors), false);
    }

    private void renderEntryError(HttpServletRequest request, HttpServletResponse response,
                                  Map<String, String> form, Map<String, String> errors)
            throws ServletException, IOException
    {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showPage(request, response, form, errors);
    }

    private boolean canWrite(HttpServletRequest request)
    {
        var user = Web.currentUser(request);

        return user != null && authorizationService.isPermitted(user.toAccount(),
                RoleCatalog.ACADEMIC_WRITE);
    }
}
