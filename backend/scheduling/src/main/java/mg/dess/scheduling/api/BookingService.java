package mg.dess.scheduling.api;

import java.util.UUID;

/**
 * Published interface of the scheduling context. Called by academic (teaching sessions)
 * and assessment (exam sessions).
 *
 * <p>Contract (see docs/contracts/booking-contract.md):
 * <ul>
 *   <li>joins the caller's transaction: if it fails, the caller's work is rolled back;</li>
 *   <li>all resources are reserved, or none;</li>
 *   <li>a conflict raises {@link BookingConflictException}.</li>
 * </ul>
 */
public interface BookingService {

    BookingReceipt reserve(BookingRequest request);

    void cancel(UUID bookingId);
}
