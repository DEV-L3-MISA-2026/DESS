package com.dess.platform.web;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.dess.assessment.application.ExamSessionService;
import com.dess.scheduling.application.RoomService;

/**
 * Results by candidate.
 *
 * <p>Read-only for now, and the results themselves are not entered yet:
 * the Result model does not exist, it is the next step of the assessment
 * module, after the assessment definitions and the eligibility of
 * candidates. This page therefore shows each session with the number
 * of candidates and the state of its results, without inventing any
 * mark.</p>
 */
@WebServlet("/app/results")
public class ResultsServlet extends HttpServlet
{
    private static final String VIEW = "/WEB-INF/views/results/index.jsp";
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
        List<Map<String, Object>> sessions = examSessionService.findAll().stream()
                .map(session -> Map.<String, Object>of(
                        "id", session.id().toString(),
                        "startsAt", DATE_TIME.format(session.startsAt()),
                        "endsAt", DATE_TIME.format(session.endsAt()),
                        "room", roomLabel(session.roomId()),
                        "candidateCount", session.candidatePersonIds().size(),
                        "results", "Not entered yet"))
                .toList();

        request.setAttribute("sessions", sessions);
        request.setAttribute("activeNav", "results");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private String roomLabel(UUID roomId)
    {
        return roomService.findById(roomId)
                .map(room -> room.code() + " - " + room.name())
                .orElse("Unknown room");
    }
}
