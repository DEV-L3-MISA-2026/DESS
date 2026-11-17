package mg.dess.scheduling.internal.application;

import java.util.UUID;
import mg.dess.scheduling.api.BookingReceipt;
import mg.dess.scheduling.api.BookingRequest;
import mg.dess.scheduling.api.BookingService;

/**
 * Implementation of the published BookingService.
 * When implementing: annotate with @ApplicationScoped, join the caller's transaction,
 * check capacity and availability, persist the Booking, call EntityManager.flush(), and
 * translate PostgreSQL SQLState 23P01 into BookingConflictException.
 */
public class BookingServiceImpl implements BookingService {

    @Override
    public BookingReceipt reserve(BookingRequest request) {
        throw new UnsupportedOperationException("Not implemented yet - see docs/contracts/booking-contract.md");
    }

    @Override
    public void cancel(UUID bookingId) {
        throw new UnsupportedOperationException("Not implemented yet - see docs/contracts/booking-contract.md");
    }
}
