// A single taxi: aggregates one Driver and an array of Passenger seats.
public class Taxi {

    private String taxiName;
    private Driver driver;
    private Passenger[] passengers;

    public Taxi(String taxiName, Driver driver, int seatCount) {
        this.taxiName = taxiName;
        this.driver = driver;
        this.passengers = new Passenger[seatCount];
    }

    public String getTaxiName()        { return taxiName; }
    public Driver getDriver()          { return driver; }
    public Passenger[] getPassengers() { return passengers; }

    // Reserves a seat. Throws if the seat index is out of range or already taken.
    public void reserveSeat(int seatIndex, Passenger passenger) throws InvalidReservationException {
        if (seatIndex < 0 || seatIndex >= passengers.length) {
            throw new InvalidReservationException("Invalid seat number for taxi " + taxiName);
        }
        if (passengers[seatIndex] != null) {
            throw new InvalidReservationException(
                    "Seat " + (seatIndex + 1) + " in " + taxiName + " is already reserved");
        }
        passengers[seatIndex] = passenger;
    }

    // Cancels a seat and returns the removed passenger. Throws if invalid or already empty.
    public Passenger cancelSeat(int seatIndex) throws InvalidReservationException {
        if (seatIndex < 0 || seatIndex >= passengers.length) {
            throw new InvalidReservationException("Invalid seat number for taxi " + taxiName);
        }
        if (passengers[seatIndex] == null) {
            throw new InvalidReservationException(
                    "Seat " + (seatIndex + 1) + " in " + taxiName + " is already empty");
        }
        Passenger removed = passengers[seatIndex];
        passengers[seatIndex] = null;
        return removed;
    }

    // Used only when restoring reservations from file; skips validation.
    public void placePassenger(int seatIndex, Passenger passenger) {
        if (seatIndex >= 0 && seatIndex < passengers.length) {
            passengers[seatIndex] = passenger;
        }
    }

    public int countReservedSeats() {
        int count = 0;
        for (int i = 0; i < passengers.length; i++) {
            if (passengers[i] != null) {
                count++;
            }
        }
        return count;
    }

    public void printTaxiInfo() {
        System.out.println("Taxi: " + taxiName);
        System.out.println("Driver: " + driver);
        System.out.println("Reserved: " + countReservedSeats() + "/" + passengers.length);
        for (int i = 0; i < passengers.length; i++) {
            if (passengers[i] == null) {
                System.out.println("  Seat " + (i + 1) + ": EMPTY");
            } else {
                System.out.println("  Seat " + (i + 1) + ": " + passengers[i]);
            }
        }
    }
}
