package com.dess.platform.web;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.dess.assessment.application.ExamSessionService;
import com.dess.assessment.application.ResultService;
import com.dess.assessment.domain.ExamSession;
import com.dess.platform.web.view.Badge;
import com.dess.platform.web.view.ResultRow;
import com.dess.platform.web.view.ResultsView;
import com.dess.platform.web.view.Views;
import com.dess.scheduling.application.RoomService;

/**
 * Results by assessment.
 *
 * <p>The page lists each exam session with the number of results recorded for
 * its assessment. Result entry and correction exist in the application layer,
 * but this page has no entry form yet, so it never invents a mark or claims that
 * a session has been validated.</p>
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        Web.exposeFlash(request);
        showPage(request, response);
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response)
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
                            DATE_TIME.format(session.startsAt()
                                    .withOffsetSameInstant(ZoneOffset.UTC)),
                            DATE_TIME.format(session.endsAt()
                                    .withOffsetSameInstant(ZoneOffset.UTC)),
                            roomLabels.getOrDefault(session.roomId(), "Unknown room"),
                            session.candidatePersonIds().size(),
                            Badge.of(progress.tone(), progress.label()));
                })
                .toList();

        ResultsView view = new ResultsView(sessions);

        Views.render(request, response, view);
    }
}
