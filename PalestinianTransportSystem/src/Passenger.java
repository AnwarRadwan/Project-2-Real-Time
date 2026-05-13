/**
 * ============================================================
 *  Passenger.java
 *  Represents a single passenger in the transport system.
 *  Encapsulates all personal and travel details.
 * ============================================================
 */
public class Passenger {

    // -------------------------
    //  Private Attributes
    // -------------------------
    private String firstName;
    private String lastName;
    private String palestinianID;
    private String departureCity;
    private String destinationCity;

    // -------------------------
    //  Constructor
    // -------------------------

    /**
     * Creates a new Passenger with all required travel details.
     *
     * @param firstName       Passenger's first name (letters only, min 3 chars)
     * @param lastName        Passenger's last name  (letters only, min 3 chars)
     * @param palestinianID   9-digit Palestinian national ID
     * @param departureCity   City where the passenger boards the taxi
     * @param destinationCity City where the passenger exits the taxi
     */
    public Passenger(String firstName, String lastName, String palestinianID,
                     String departureCity, String destinationCity) {
        this.firstName       = firstName;
        this.lastName        = lastName;
        this.palestinianID   = palestinianID;
        this.departureCity   = departureCity;
        this.destinationCity = destinationCity;
    }

    // -------------------------
    //  Getters
    // -------------------------
    public String getFirstName()       { return firstName; }
    public String getLastName()        { return lastName; }
    public String getPalestinianID()   { return palestinianID; }
    public String getDepartureCity()   { return departureCity; }
    public String getDestinationCity() { return destinationCity; }

    // -------------------------
    //  Setters
    // -------------------------
    public void setFirstName(String firstName)             { this.firstName       = firstName; }
    public void setLastName(String lastName)               { this.lastName        = lastName; }
    public void setPalestinianID(String palestinianID)     { this.palestinianID   = palestinianID; }
    public void setDepartureCity(String departureCity)     { this.departureCity   = departureCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    // -------------------------
    //  Display Method
    // -------------------------

    /**
     * Prints all passenger information in a clean, formatted layout.
     * Called whenever passenger details need to be shown to the user.
     */
    public void printPassengerInfo() {
        System.out.println("    Full Name      : " + firstName + " " + lastName);
        System.out.println("    Palestinian ID : " + palestinianID);
        System.out.println("    Departure      : " + departureCity);
        System.out.println("    Destination    : " + destinationCity);
    }
}
