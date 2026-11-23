package com.dess.academic.application;

import com.dess.academic.domain.GroupEnrollment;

import java.util.List;
import java.util.UUID;

/** API applicative des inscriptions dans les groupes. */
public interface GroupEnrollmentService
{
    GroupEnrollment enroll(UUID studentId, UUID groupId, UUID periodId);
    void remove(UUID enrollmentId);
    GroupEnrollment get(UUID enrollmentId);
    List<GroupEnrollment> listByGroupAndPeriod(UUID groupId, UUID periodId);
}
