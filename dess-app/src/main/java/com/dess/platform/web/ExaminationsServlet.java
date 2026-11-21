package com.dess.platform.web;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private static final String VIEW = "/WEB-INF/views/examinations/index.jsp";
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

        List<Map<String, Object>> sessions = examSessionService.findAll().stream()
                .map(session -> Map.<String, Object>of(
                        "id", session.id().toString(),
                        "startsAt", DATE_TIME.format(
                                session.startsAt().withOffsetSameInstant(ZoneOffset.UTC)),
                        "endsAt", DATE_TIME.format(
                                session.endsAt().withOffsetSameInstant(ZoneOffset.UTC)),
                        "room", roomLabels.getOrDefault(session.roomId(), "Unknown room"),
                        "candidateCount", session.candidatePersonIds().size(),
                        "invigilatorCount", session.invigilatorPersonIds().size()))
                .toList();

        request.setAttribute("sessions", sessions);
        request.setAttribute("activeNav", "examinations");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
