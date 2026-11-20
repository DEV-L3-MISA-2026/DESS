package com.dess.assessment.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dess.assessment.application.impl.ExamSessionServiceImpl;
import com.dess.assessment.domain.ExamSession;

class ExamSessionServiceTest
{
    private ExamSessionServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ExamSessionServiceImpl();
    }

    @Test
    void startsEmpty()
    {
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void createsAndFindsSession()
    {
        ExamSession created = service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-23T09:00:00+03:00"), date("2026-11-23T11:00:00+03:00"));

        assertEquals(created, service.findById(created.id()).orElseThrow());
        assertEquals(1, service.findAll().size());
    }

    @Test
    void listsNewestFirst()
    {
        service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-23T09:00:00+03:00"), date("2026-11-23T11:00:00+03:00"));
        ExamSession later = service.create(UUID.randomUUID(), UUID.randomUUID(),
                date("2026-11-24T09:00:00+03:00"), date("2026-11-24T11:00:00+03:00"));

        List<ExamSession> sessions = service.findAll();

        assertEquals(2, sessions.size());
        assertEquals(later, sessions.get(0));
    }

    @Test
    void returnsEmptyForUnknownId()
    {
        assertTrue(service.findById(UUID.randomUUID()).isEmpty());
    }

    private static OffsetDateTime date(String text)
    {
        return OffsetDateTime.parse(text);
    }
}
