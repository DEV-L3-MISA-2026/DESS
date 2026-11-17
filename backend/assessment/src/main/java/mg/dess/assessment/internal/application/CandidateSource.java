package mg.dess.assessment.internal.application;

import java.util.List;
import java.util.UUID;

/**
 * Port owned by assessment: where exam candidates come from. Implemented by an
 * anti-corruption adapter (infrastructure.acl) that translates academic.api data,
 * so changes in academic never leak into the exam model.
 */
public interface CandidateSource {

    List<UUID> studentIdsOfGroup(UUID groupId);
}
