// A taxi driver. Extends Person and adds a license number.
public class Driver extends Person {

    private String driverLicense;

    public Driver(String firstName, String lastName, String palestinianID, String driverLicense) {
        super(firstName, lastName, palestinianID);
        this.driverLicense = driverLicense;
    }

    public String getDriverLicense() { return driverLicense; }
    public void setDriverLicense(String driverLicense) { this.driverLicense = driverLicense; }

    @Override
    public String getRole() {
        return "Driver";
    }

    @Override
    public String toString() {
        return super.toString() + " | License: " + driverLicense;
    }
}
