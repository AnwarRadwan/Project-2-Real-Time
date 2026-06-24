// Custom checked exception for all reservation-related failures.
public class InvalidReservationException extends Exception {
    public InvalidReservationException(String message) {
        super(message);
    }
}
