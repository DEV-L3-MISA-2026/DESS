package mg.dess.academic.api;

import java.util.List;
import java.util.UUID;

/** Published read interface about student groups. Used by assessment (candidates) and for room capacity. */
public interface GroupQueries {

    /** Number of students enrolled in the group. */
    int sizeOf(UUID groupId);

    /** Ids of the students enrolled in the group. */
    List<UUID> studentIdsOf(UUID groupId);
}
