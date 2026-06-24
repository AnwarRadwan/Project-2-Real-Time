// A passenger riding in one of the taxis. Extends Person and is Payable.
public class Passenger extends Person implements Payable {

    private String departureCity;
    private String destinationCity;

    public Passenger(String firstName, String lastName, String palestinianID,
                      String departureCity, String destinationCity) {
        super(firstName, lastName, palestinianID);
        this.departureCity = departureCity;
        this.destinationCity = destinationCity;
    }

    public String getDepartureCity()   { return departureCity; }
    public String getDestinationCity() { return destinationCity; }

    public void setDepartureCity(String departureCity)     { this.departureCity = departureCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    // Fare depends only on the departure city, since every route ends in Ramallah.
    @Override
    public double calculateFare() {
        if (departureCity.equalsIgnoreCase("Hebron"))    return 25;
        if (departureCity.equalsIgnoreCase("Jenin"))     return 30;
        if (departureCity.equalsIgnoreCase("Jerusalem")) return 15;
        return 0;
    }

    @Override
    public String getRole() {
        return "Passenger";
    }

    @Override
    public String toString() {
        return super.toString() + " | From: " + departureCity + " To: " + destinationCity
                + " | Fare: " + calculateFare() + " NIS";
    }
}
