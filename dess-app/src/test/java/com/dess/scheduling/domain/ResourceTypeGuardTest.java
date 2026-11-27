package com.dess.scheduling.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

/**
 * Guard for the frozen resource type set.
 *
 * ResourceType is part of the frozen booking contract shared with the
 * assessment module. Adding or renaming a value is a contract change:
 * it needs an agreement between the scheduling owners and the
 * assessment owners first. If this test fails, do not just update the
 * expected list — get the contract change agreed, then update it.
 */
class ResourceTypeGuardTest
{
    @Test
    void resourceTypeSetIsExactlyTheFrozenContract()
    {
        assertArrayEquals(
                new ResourceType[]{ResourceType.ROOM, ResourceType.PERSON, ResourceType.STUDENT_GROUP},
                ResourceType.values(),
                "ResourceType is a frozen contract (ROOM, PERSON, STUDENT_GROUP). "
                        + "A new value is a contract change: it needs an agreement between "
                        + "the scheduling owners and the assessment owners before it is added.");
    }
}
