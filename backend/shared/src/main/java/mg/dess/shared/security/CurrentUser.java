package mg.dess.shared.security;

import java.util.UUID;

/**
 * The authenticated user, as seen by the business modules. Implemented in the security
 * module. Roles live in security; ownership rules (for example "a professor only enters
 * results for their own course offerings") are checked by the module that owns the data.
 */
public interface CurrentUser {

    UUID personId();

    boolean isAdmin();

    boolean isProfessor();
}
