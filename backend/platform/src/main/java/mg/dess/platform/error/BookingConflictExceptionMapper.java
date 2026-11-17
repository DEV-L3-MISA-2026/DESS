package mg.dess.platform.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import mg.dess.scheduling.api.BookingConflictException;

/** A booking conflict becomes HTTP 409 (more specific than the generic DomainException mapper). */
@Provider
public class BookingConflictExceptionMapper implements ExceptionMapper<BookingConflictException> {

    @Override
    public Response toResponse(BookingConflictException e) {
        return Response.status(409)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(e.code(), e.getMessage()))
                .build();
    }
}
