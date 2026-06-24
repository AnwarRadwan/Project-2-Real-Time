import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ReservationSystem {

    static final int SEATS_PER_TAXI = 7;
    static final String FILE_NAME = "reservations.txt";

    static Taxi[] taxis = new Taxi[3];
    static Scanner scanner = new Scanner(System.in);

    // Departure city for each taxi index, used when building a Passenger.
    static String[] departureCities = { "Hebron", "Jenin", "Jerusalem" };

    public static void main(String[] args) {

        initializeTaxis();
        loadFromFile(); // Load any reservations saved from a previous run.

        int choice;

        // Main menu loop, continues until the user selects Exit.
        do {
            printMenu();
            choice = readInt("Enter your choice: ");

            switch (choice) {
                case 1:  reserveSeat();          break;
                case 2:  cancelReservation();    break;
                case 3:  searchByNameMenu();      break;
                case 4:  searchByIDMenu();        break;
                case 5:  displaySeat();          break;
                case 6:  printAllReservations(); break;
                case 7:  displayStatistics();    break;
                case 8:  displayDriversInfo();   break;
                case 9:  saveToFile();           break;
                case 10: loadFromFile();         break;
                case 11: displayFareReport();    break;
                case 12:
                    saveToFile(); // Save reservations before exiting.
                    System.out.println("Thank you for using the system. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 1 to 12.");
            }

        } while (choice != 12);

        scanner.close();
    }

    static void printMenu() {
        System.out.println("\n==========================================");
        System.out.println("              MAIN MENU");
        System.out.println("==========================================");
        System.out.println("  1.  Reserve a Seat");
        System.out.println("  2.  Cancel Reservation");
        System.out.println("  3.  Search Passenger by Name");
        System.out.println("  4.  Search Passenger by ID");
        System.out.println("  5.  Display Passenger in a Seat");
        System.out.println("  6.  Print All Reservations");
        System.out.println("  7.  Display Taxi Statistics");
        System.out.println("  8.  Display Drivers Information");
        System.out.println("  9.  Save Data to File");
        System.out.println("  10. Load Data from File");
        System.out.println("  11. Display Fare Report");
        System.out.println("  12. Exit");
        System.out.println("==========================================");
    }

    // Creates the three fixed taxis, each with its own driver.
    static void initializeTaxis() {
        Driver driver1 = new Driver("Omar", "Saleh", "111111111", "DRV-1001");
        Driver driver2 = new Driver("Khaled", "Nasser", "222222222", "DRV-1002");
        Driver driver3 = new Driver("Yousef", "Amer", "333333333", "DRV-1003");

        Driver[] drivers = { driver1, driver2, driver3 };
        for (Driver d : drivers) {
            if (!validateDriverLicense(d.getDriverLicense())) {
                System.out.println("Warning: invalid driver license format for " + d.getFirstName());
            }
        }

        taxis[0] = new Taxi("Hebron -> Ramallah", driver1, SEATS_PER_TAXI);
        taxis[1] = new Taxi("Jenin -> Ramallah", driver2, SEATS_PER_TAXI);
        taxis[2] = new Taxi("Jerusalem -> Ramallah", driver3, SEATS_PER_TAXI);
    }

    // ---------------------------------------------------------------
    // Validation helpers (regular expressions)
    // ---------------------------------------------------------------

    static boolean validateName(String name) {
        return name != null && name.matches("[A-Za-z]{3,}");
    }

    static boolean validateID(String id) {
        return id != null && id.matches("\\d{9}");
    }

    static boolean validateDriverLicense(String license) {
        return license != null && license.matches("DRV-\\d{4}");
    }

    // Checks every seat in every taxi for a matching Palestinian ID.
    static boolean isDuplicateID(String id) {
        for (Taxi taxi : taxis) {
            for (Passenger p : taxi.getPassengers()) {
                if (p != null && p.getPalestinianID().equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Input helpers
    // ---------------------------------------------------------------

    static int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Please enter a valid number: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    static String readValidatedName(String prompt) {
        String name;
        do {
            System.out.print(prompt);
            name = scanner.nextLine().trim();
            if (!validateName(name)) {
                System.out.println("Invalid name. Letters only, minimum 3 characters.");
            }
        } while (!validateName(name));
        return name;
    }

    static void printSeatMap(Taxi taxi) {
        System.out.print("Seats: ");
        Passenger[] seats = taxi.getPassengers();
        for (int i = 0; i < seats.length; i++) {
            System.out.print(" [" + (i + 1) + ":" + (seats[i] == null ? "FREE" : "TAKEN") + "]");
        }
        System.out.println();
    }

    // ---------------------------------------------------------------
    // Menu option 1: Reserve a Seat
    // ---------------------------------------------------------------

    static void reserveSeat() {
        System.out.println("\n--- Reserve a Seat ---");

        try {
            int taxiNumber = readInt("Enter taxi number (1-3): ");
            if (taxiNumber < 1 || taxiNumber > 3) {
                throw new InvalidReservationException("Invalid taxi number: " + taxiNumber);
            }
            Taxi taxi = taxis[taxiNumber - 1];
            printSeatMap(taxi);

            int seatNumber = readInt("Enter seat number (1-7): ");
            int seatIndex = seatNumber - 1;

            String firstName = readValidatedName("Enter First Name: ");
            String lastName = readValidatedName("Enter Last Name: ");

            System.out.print("Enter Palestinian ID (9 digits): ");
            String id = scanner.nextLine().trim();
            if (!validateID(id)) {
                throw new InvalidReservationException("Invalid ID format: " + id);
            }
            if (isDuplicateID(id)) {
                throw new InvalidReservationException("This Palestinian ID is already registered: " + id);
            }

            String departureCity = departureCities[taxiNumber - 1];
            Passenger passenger = new Passenger(firstName, lastName, id, departureCity, "Ramallah");

            // Taxi.reserveSeat throws if the seat index is invalid or already taken.
            taxi.reserveSeat(seatIndex, passenger);

            System.out.println("\nReservation successful!");
            System.out.println(passenger);

        } catch (InvalidReservationException e) {
            System.out.println("Reservation failed: " + e.getMessage());
        } finally {
            System.out.println("Reservation attempt finished.\n");
        }
    }

    // ---------------------------------------------------------------
    // Menu option 2: Cancel Reservation
    // ---------------------------------------------------------------

    static void cancelReservation() {
        System.out.println("\n--- Cancel Reservation ---");

        try {
            int taxiNumber = readInt("Enter taxi number (1-3): ");
            if (taxiNumber < 1 || taxiNumber > 3) {
                throw new InvalidReservationException("Invalid taxi number: " + taxiNumber);
            }
            Taxi taxi = taxis[taxiNumber - 1];
            printSeatMap(taxi);

            int seatNumber = readInt("Enter seat number (1-7): ");
            int seatIndex = seatNumber - 1;

            Passenger removed = taxi.cancelSeat(seatIndex);
            System.out.println("Cancelled reservation for: " + removed);

        } catch (InvalidReservationException e) {
            System.out.println("Cancellation failed: " + e.getMessage());
        } finally {
            System.out.println("Cancellation attempt finished.\n");
        }
    }

    // ---------------------------------------------------------------
    // Menu options 3 & 4: Search by Name / ID
    // ---------------------------------------------------------------

    static void searchByNameMenu() {
        System.out.print("\nEnter name to search: ");
        String query = scanner.nextLine().trim();
        findPassengerByName(query);
    }

    static void findPassengerByName(String query) {
        String lowerQuery = query.toLowerCase();
        boolean found = false;

        for (Taxi taxi : taxis) {
            Passenger[] seats = taxi.getPassengers();
            for (int i = 0; i < seats.length; i++) {
                Passenger p = seats[i];
                if (p != null) {
                    String fullName = (p.getFirstName() + " " + p.getLastName()).toLowerCase();
                    if (fullName.contains(lowerQuery)) {
                        System.out.println("\nFound in " + taxi.getTaxiName() + ", Seat " + (i + 1) + ":");
                        System.out.println(p);
                        found = true;
                    }
                }
            }
        }

        if (!found) {
            System.out.println("No passenger found with name containing: " + query);
        }
    }

    static void searchByIDMenu() {
        System.out.print("\nEnter Palestinian ID to search: ");
        String query = scanner.nextLine().trim();
        findPassengerByID(query);
    }

    static void findPassengerByID(String query) {
        boolean found = false;

        for (Taxi taxi : taxis) {
            Passenger[] seats = taxi.getPassengers();
            for (int i = 0; i < seats.length; i++) {
                Passenger p = seats[i];
                if (p != null && p.getPalestinianID().equals(query)) {
                    System.out.println("\nFound in " + taxi.getTaxiName() + ", Seat " + (i + 1) + ":");
                    System.out.println(p);
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("No passenger found with ID: " + query);
        }
    }

    // ---------------------------------------------------------------
    // Menu option 5: Display Passenger in a Seat
    // ---------------------------------------------------------------

    static void displaySeat() {
        System.out.println("\n--- Display Passenger in a Seat ---");

        try {
            int taxiNumber = readInt("Enter taxi number (1-3): ");
            if (taxiNumber < 1 || taxiNumber > 3) {
                throw new InvalidReservationException("Invalid taxi number: " + taxiNumber);
            }
            Taxi taxi = taxis[taxiNumber - 1];
            printSeatMap(taxi);

            int seatNumber = readInt("Enter seat number (1-7): ");
            if (seatNumber < 1 || seatNumber > SEATS_PER_TAXI) {
                throw new InvalidReservationException("Invalid seat number: " + seatNumber);
            }

            Passenger p = taxi.getPassengers()[seatNumber - 1];
            if (p == null) {
                System.out.println("Seat " + seatNumber + " is EMPTY.");
            } else {
                System.out.println(p);
            }

        } catch (InvalidReservationException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            System.out.println("Display request finished.\n");
        }
    }

    // ---------------------------------------------------------------
    // Menu option 6: Print All Reservations
    // ---------------------------------------------------------------

    static void printAllReservations() {
        System.out.println("\n--- All Reservations ---");
        for (Taxi taxi : taxis) {
            taxi.printTaxiInfo();
            System.out.println();
        }
    }

    // ---------------------------------------------------------------
    // Menu option 7: Display Taxi Statistics
    // ---------------------------------------------------------------

    static void displayStatistics() {
        System.out.println("\n--- Taxi Statistics ---");
        int totalReserved = 0;
        int totalSeats = 0;

        for (Taxi taxi : taxis) {
            int reserved = taxi.countReservedSeats();
            totalReserved += reserved;
            totalSeats += SEATS_PER_TAXI;
            System.out.println(taxi.getTaxiName() + " -> Reserved: " + reserved + "/" + SEATS_PER_TAXI
                    + "  Available: " + (SEATS_PER_TAXI - reserved));
        }

        System.out.println("Total Reserved: " + totalReserved + "/" + totalSeats);
    }

    // ---------------------------------------------------------------
    // Menu option 8: Display Drivers Information (Polymorphism)
    // ---------------------------------------------------------------

    // Combines all drivers and passengers into one Person[] array.
    static Person[] buildPeopleArray() {
        int passengerCount = 0;
        for (Taxi taxi : taxis) {
            passengerCount += taxi.countReservedSeats();
        }

        Person[] people = new Person[taxis.length + passengerCount];
        int index = 0;

        for (Taxi taxi : taxis) {
            people[index++] = taxi.getDriver();
        }
        for (Taxi taxi : taxis) {
            for (Passenger p : taxi.getPassengers()) {
                if (p != null) {
                    people[index++] = p;
                }
            }
        }

        return people;
    }

    static void displayDriversInfo() {
        System.out.println("\n--- People in the System (Polymorphism Demo) ---");
        Person[] people = buildPeopleArray();

        // getRole() resolves to "Driver" or "Passenger" depending on the actual object type.
        for (Person person : people) {
            System.out.println(person.getRole() + ": " + person);
        }
    }

    // ---------------------------------------------------------------
    // Menu options 9 & 10: Save / Load File
    // ---------------------------------------------------------------

    static void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (int t = 0; t < taxis.length; t++) {
                Passenger[] seats = taxis[t].getPassengers();
                for (int s = 0; s < seats.length; s++) {
                    Passenger p = seats[s];
                    if (p != null) {
                        writer.println(t + "," + s + "," + p.getFirstName() + "," + p.getLastName() + ","
                                + p.getPalestinianID() + "," + p.getDepartureCity() + "," + p.getDestinationCity());
                    }
                }
            }
            System.out.println("Reservations saved to " + FILE_NAME);
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("No saved file found. Starting with empty taxis.");
            return;
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                int taxiIndex = Integer.parseInt(parts[0]);
                int seatIndex = Integer.parseInt(parts[1]);
                Passenger p = new Passenger(parts[2], parts[3], parts[4], parts[5], parts[6]);
                taxis[taxiIndex].placePassenger(seatIndex, p);
            }
            System.out.println("Reservations loaded from " + FILE_NAME);
        } catch (IOException e) {
            System.out.println("Error loading file: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Menu option 11: Fare Report (bonus feature)
    // ---------------------------------------------------------------

    static void displayFareReport() {
        System.out.println("\n--- Fare Report ---");
        double grandTotal = 0;

        for (Taxi taxi : taxis) {
            System.out.println("\n" + taxi.getTaxiName() + ":");
            double taxiTotal = 0;
            Passenger[] seats = taxi.getPassengers();

            for (int i = 0; i < seats.length; i++) {
                Passenger p = seats[i];
                if (p != null) {
                    double fare = p.calculateFare();
                    taxiTotal += fare;
                    System.out.println("  Seat " + (i + 1) + ": " + p.getFirstName() + " " + p.getLastName()
                            + " -> " + fare + " NIS");
                }
            }

            System.out.println("  Taxi Total: " + taxiTotal + " NIS");
            grandTotal += taxiTotal;
        }

        System.out.println("\nGrand Total Collected: " + grandTotal + " NIS");
    }
}
