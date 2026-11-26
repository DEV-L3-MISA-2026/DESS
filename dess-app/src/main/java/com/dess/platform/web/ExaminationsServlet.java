package com.dess.platform.web;

import java.io.IOException;
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
import com.dess.platform.web.view.ExaminationsView;
import com.dess.platform.web.view.SessionRow;
import com.dess.platform.web.view.Views;
import com.dess.scheduling.application.RoomService;

/**
 * Exam sessions list.
 *
 * <p>Read-only for now. Sessions are created by the exam scheduling
 * flow; this page lists what the assessment module holds and shows
 * how many candidates and invigilators each session has.</p>
 */
@WebServlet("/app/examinations")
public class ExaminationsServlet extends HttpServlet
{
    @Inject
    private ExamSessionService examSessionService;

    @Inject
    private RoomService roomService;

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
        Map<UUID, String> roomLabels = roomService.findAll().stream()
                .collect(Collectors.toMap(room -> room.id(),
                        room -> room.code() + " - " + room.name()));

        List<SessionRow> sessions = examSessionService.findAll().stream()
                .map(session -> new SessionRow(
                        Web.moment(session.startsAt()),
                        Web.moment(session.endsAt()),
                        roomLabels.getOrDefault(session.roomId(), "Unknown room"),
                        session.candidatePersonIds().size(),
                        session.invigilatorPersonIds().size()))
                .toList();

        ExaminationsView view = new ExaminationsView(sessions);

        Views.render(request, response, view);
    }
}
