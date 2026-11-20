package com.dess.scheduling.domain;

/**
 * Frozen set of resource types that a booking can reserve.
 *
 * <p>ROOM is a schedulable room. PERSON is one individual, a teacher
 * or a student, identified by a Person id. STUDENT_GROUP is a teaching
 * cohort examined together. No other resource type is part of the
 * frozen booking contract. A new type needs an agreement between the
 * scheduling owners and the assessment owners before it is added.</p>
 */
public enum ResourceType
{
    ROOM,
    PERSON,
    STUDENT_GROUP
}
