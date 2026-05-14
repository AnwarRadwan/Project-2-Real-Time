public class Passenger {

    private String firstName;
    private String lastName;
    private String palestinianID;
    private String departureCity;
    private String destinationCity;

    public Passenger(String firstName, String lastName, String palestinianID,
                     String departureCity, String destinationCity) {
        this.firstName       = firstName;
        this.lastName        = lastName;
        this.palestinianID   = palestinianID;
        this.departureCity   = departureCity;
        this.destinationCity = destinationCity;
    }

    public String getFirstName()       { return firstName; }
    public String getLastName()        { return lastName; }
    public String getPalestinianID()   { return palestinianID; }
    public String getDepartureCity()   { return departureCity; }
    public String getDestinationCity() { return destinationCity; }

    public void setFirstName(String firstName)             { this.firstName       = firstName; }
    public void setLastName(String lastName)               { this.lastName        = lastName; }
    public void setPalestinianID(String palestinianID)     { this.palestinianID   = palestinianID; }
    public void setDepartureCity(String departureCity)     { this.departureCity   = departureCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    public void printPassengerInfo() {
        System.out.println("  Full Name      : " + firstName + " " + lastName);
        System.out.println("  Palestinian ID : " + palestinianID);
        System.out.println("  Departure      : " + departureCity);
        System.out.println("  Destination    : " + destinationCity);
    }
}
