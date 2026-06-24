// Abstract base class shared by Passenger and Driver.
public abstract class Person {

    private String firstName;
    private String lastName;
    private String palestinianID;

    public Person(String firstName, String lastName, String palestinianID) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.palestinianID = palestinianID;
    }

    public String getFirstName()     { return firstName; }
    public String getLastName()      { return lastName; }
    public String getPalestinianID() { return palestinianID; }

    public void setFirstName(String firstName)         { this.firstName = firstName; }
    public void setLastName(String lastName)           { this.lastName = lastName; }
    public void setPalestinianID(String palestinianID) { this.palestinianID = palestinianID; }

    // Each subclass defines its own role label (Driver or Passenger).
    public abstract String getRole();

    @Override
    public String toString() {
        return firstName + " " + lastName + " (ID: " + palestinianID + ")";
    }
}
