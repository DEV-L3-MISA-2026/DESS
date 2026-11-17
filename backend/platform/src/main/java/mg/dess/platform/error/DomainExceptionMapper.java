package mg.dess.platform.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import mg.dess.shared.domain.DomainException;

/** Business-rule violations become HTTP 422 with a readable message. */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    @Override
    public Response toResponse(DomainException e) {
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(e.code(), e.getMessage()))
                .build();
    }
}
