/**
 * Assessment domain: definitions, eligibility and results.
 *
 * <p>Fabien owns this module. Christian owns the exam sessions.</p>
 *
 * <p>An {@code Assessment} is a definition: what is evaluated, for
 * which course, with which maximum score. An {@code ExamSession} is
 * the event that holds the assessment at one time and in one room.
 * A {@code Result} is one student's outcome for one assessment.</p>
 *
 * <p>One student has at most one result per assessment. A retake is
 * a new assessment, not a second result.</p>
 */
package com.dess.assessment.domain;
