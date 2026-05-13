import java.util.Scanner;

public class ReservationSystem {

    static Passenger[][] taxis = new Passenger[3][7];
    static Scanner scanner = new Scanner(System.in);
    static String[] departureCities = { "Hebron", "Jenin", "Jerusalem" };
    static int[] fares = { 30, 35, 25 };

    public static void main(String[] args) {

        System.out.println("==========================================");
        System.out.println("  Palestinian Transport Reservation System");
        System.out.println("  Hebron | Jenin | Jerusalem -> Ramallah");
        System.out.println("==========================================");

        int choice;

        do {
            printMenu();
            System.out.print("Enter your choice: ");
            choice = safeReadInt();

            switch (choice) {
                case 1: reserveSeat();            break;
                case 2: cancelReservation();      break;
                case 3: searchByName();           break;
                case 4: searchByID();             break;
                case 5: displaySeat();            break;
                case 6: printAllReservations();   break;
                case 7: displayOccupancyReport(); break;
                case 8:
                    System.out.println("Thank you for using the system. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 1 to 8.");
            }

        } while (choice != 8);

        scanner.close();
    }

    static void printMenu() {
        System.out.println("\n==========================================");
        System.out.println("              MAIN MENU");
        System.out.println("==========================================");
        System.out.println("  1. Reserve a Seat");
        System.out.println("  2. Cancel Reservation");
        System.out.println("  3. Search Passenger by Name");
        System.out.println("  4. Search Passenger by ID");
        System.out.println("  5. Display Passenger in a Seat");
        System.out.println("  6. Print All Reservations");
        System.out.println("  7. Occupancy and Fare Report");
        System.out.println("  8. Exit");
        System.out.println("==========================================");
    }

    static String getTaxiName(int index) {
        if (index == 0) return "Hebron -> Ramallah";
        if (index == 1) return "Jenin  -> Ramallah";
        if (index == 2) return "Jerusalem -> Ramallah";
        return "Unknown Route";
    }

    static int countReservedSeatsInTaxi(int taxiIndex) {
        int count = 0;
        for (int i = 0; i < 7; i++) {
            if (taxis[taxiIndex][i] != null) {
                count++;
            }
        }
        return count;
    }

    static boolean isValidName(String name) {
        if (name == null || name.trim().length() < 3) return false;
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isLetter(name.charAt(i))) return false;
        }
        return true;
    }

    static boolean isValidID(String id) {
        if (id == null || id.length() != 9) return false;
        for (int i = 0; i < id.length(); i++) {
            if (!Character.isDigit(id.charAt(i))) return false;
        }
        return true;
    }

    static boolean isDuplicateID(String id) {
        for (int t = 0; t < 3; t++) {
            for (int s = 0; s < 7; s++) {
                if (taxis[t][s] != null && taxis[t][s].getPalestinianID().equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    static int calculateFare(int taxiIndex) {
        return fares[taxiIndex];
    }

    static int safeReadInt() {
        while (!scanner.hasNextInt()) {
            System.out.print("Please enter a valid number: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    static int selectTaxi() {
        System.out.println("\nAvailable Taxis:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  " + (i + 1) + ". " + getTaxiName(i)
                    + "  [" + countReservedSeatsInTaxi(i) + "/7 reserved]");
        }
        int taxi;
        do {
            System.out.print("Enter taxi number (1-3): ");
            taxi = safeReadInt();
            if (taxi < 1 || taxi > 3) {
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        } while (taxi < 1 || taxi > 3);
        return taxi - 1;
    }

    static int selectSeat(int taxiIndex) {
        System.out.print("Seats for " + getTaxiName(taxiIndex) + ": ");
        for (int s = 0; s < 7; s++) {
            if (taxis[taxiIndex][s] == null) {
                System.out.print(" [" + (s + 1) + ":FREE]");
            } else {
                System.out.print(" [" + (s + 1) + ":TAKEN]");
            }
        }
        System.out.println();
        int seat;
        do {
            System.out.print("Enter seat number (1-7): ");
            seat = safeReadInt();
            if (seat < 1 || seat > 7) {
                System.out.println("Invalid choice. Please enter 1 to 7.");
            }
        } while (seat < 1 || seat > 7);
        return seat - 1;
    }

    static void reserveSeat() {
        System.out.println("\n--- Reserve a Seat ---");

        int taxiIndex = selectTaxi();
        int seatIndex = selectSeat(taxiIndex);

        if (taxis[taxiIndex][seatIndex] != null) {
            System.out.println("Seat " + (seatIndex + 1) + " in "
                    + getTaxiName(taxiIndex) + " is already reserved.");
            return;
        }

        String firstName;
        do {
            System.out.print("Enter First Name: ");
            firstName = scanner.nextLine().trim();
            if (!isValidName(firstName)) {
                System.out.println("Invalid first name. Letters only, minimum 3 characters.");
            }
        } while (!isValidName(firstName));

        String lastName;
        do {
            System.out.print("Enter Last Name: ");
            lastName = scanner.nextLine().trim();
            if (!isValidName(lastName)) {
                System.out.println("Invalid last name. Letters only, minimum 3 characters.");
            }
        } while (!isValidName(lastName));

        String palestinianID;
        boolean idAccepted = false;
        do {
            System.out.print("Enter Palestinian ID (9 digits): ");
            palestinianID = scanner.nextLine().trim();
            if (!isValidID(palestinianID)) {
                System.out.println("Invalid ID. Must be exactly 9 digits.");
            } else if (isDuplicateID(palestinianID)) {
                System.out.println("This ID is already registered. Duplicate IDs are not allowed.");
            } else {
                idAccepted = true;
            }
        } while (!idAccepted);

        String departureCity   = departureCities[taxiIndex];
        String destinationCity = "Ramallah";

        taxis[taxiIndex][seatIndex] = new Passenger(
                firstName, lastName, palestinianID,
                departureCity, destinationCity);

        System.out.println("\nReservation Successful!");
        System.out.println("------------------------------------------");
        System.out.println("  Taxi : " + getTaxiName(taxiIndex));
        System.out.println("  Seat : " + (seatIndex + 1));
        System.out.println("  Fare : " + calculateFare(taxiIndex) + " NIS");
        System.out.println("------------------------------------------");
        taxis[taxiIndex][seatIndex].printPassengerInfo();
        System.out.println("------------------------------------------");
    }

    static void cancelReservation() {
        System.out.println("\n--- Cancel Reservation ---");

        int taxiIndex = selectTaxi();
        int seatIndex = selectSeat(taxiIndex);

        if (taxis[taxiIndex][seatIndex] == null) {
            System.out.println("Seat " + (seatIndex + 1) + " is already empty. Nothing to cancel.");
            return;
        }

        System.out.println("\nPassenger to be removed:");
        System.out.println("------------------------------------------");
        taxis[taxiIndex][seatIndex].printPassengerInfo();
        System.out.println("------------------------------------------");

        taxis[taxiIndex][seatIndex] = null;

        System.out.println("Reservation cancelled. Seat " + (seatIndex + 1) + " is now available.");
    }

    static void searchByName() {
        System.out.println("\n--- Search by Name ---");
        System.out.print("Enter name to search: ");
        String query = scanner.nextLine().trim().toLowerCase();

        boolean found = false;

        for (int t = 0; t < 3; t++) {
            for (int s = 0; s < 7; s++) {
                if (taxis[t][s] != null) {
                    String fullName = (taxis[t][s].getFirstName() + " "
                            + taxis[t][s].getLastName()).toLowerCase();
                    if (fullName.contains(query)) {
                        System.out.println("\nPassenger found:");
                        System.out.println("------------------------------------------");
                        System.out.println("  Taxi : " + getTaxiName(t));
                        System.out.println("  Seat : " + (s + 1));
                        System.out.println("------------------------------------------");
                        taxis[t][s].printPassengerInfo();
                        System.out.println("------------------------------------------");
                        found = true;
                    }
                }
            }
        }

        if (!found) {
            System.out.println("No passenger found with name: " + query);
        }
    }

    static void searchByID() {
        System.out.println("\n--- Search by Palestinian ID ---");
        System.out.print("Enter Palestinian ID: ");
        String query = scanner.nextLine().trim();

        boolean found = false;

        for (int t = 0; t < 3; t++) {
            for (int s = 0; s < 7; s++) {
                if (taxis[t][s] != null && taxis[t][s].getPalestinianID().equals(query)) {
                    System.out.println("\nPassenger found:");
                    System.out.println("------------------------------------------");
                    System.out.println("  Taxi : " + getTaxiName(t));
                    System.out.println("  Seat : " + (s + 1));
                    System.out.println("------------------------------------------");
                    taxis[t][s].printPassengerInfo();
                    System.out.println("------------------------------------------");
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("No passenger found with ID: " + query);
        }
    }

    static void displaySeat() {
        System.out.println("\n--- Display Seat ---");

        int taxiIndex = selectTaxi();
        int seatIndex = selectSeat(taxiIndex);

        if (taxis[taxiIndex][seatIndex] == null) {
            System.out.println("Seat " + (seatIndex + 1) + " in "
                    + getTaxiName(taxiIndex) + " is EMPTY.");
        } else {
            System.out.println("\nSeat " + (seatIndex + 1) + " | " + getTaxiName(taxiIndex));
            System.out.println("------------------------------------------");
            taxis[taxiIndex][seatIndex].printPassengerInfo();
            System.out.println("------------------------------------------");
        }
    }

    static void printAllReservations() {
        System.out.println("\n==========================================");
        System.out.println("       ALL CURRENT RESERVATIONS");
        System.out.println("==========================================");

        for (int t = 0; t < 3; t++) {
            int reserved = countReservedSeatsInTaxi(t);

            System.out.println("\nTaxi " + (t + 1) + ": " + getTaxiName(t));
            System.out.println("Reserved: " + reserved + " / 7   Available: " + (7 - reserved));
            System.out.println("------------------------------------------");

            for (int s = 0; s < 7; s++) {
                if (taxis[t][s] == null) {
                    System.out.println("  Seat " + (s + 1) + " : EMPTY");
                } else {
                    System.out.println("  Seat " + (s + 1) + " : "
                            + taxis[t][s].getFirstName() + " " + taxis[t][s].getLastName()
                            + "  (ID: " + taxis[t][s].getPalestinianID() + ")");
                }
            }
        }
    }

    static void displayOccupancyReport() {
        System.out.println("\n==========================================");
        System.out.println("        OCCUPANCY AND FARE REPORT");
        System.out.println("==========================================");

        int busiestTaxiIndex   = 0;
        int maxPassengers      = 0;
        int totalSystemRevenue = 0;

        for (int t = 0; t < 3; t++) {
            int reserved  = countReservedSeatsInTaxi(t);
            int available = 7 - reserved;
            int percent   = (reserved * 100) / 7;
            int revenue   = reserved * fares[t];

            totalSystemRevenue += revenue;

            if (reserved > maxPassengers) {
                maxPassengers    = reserved;
                busiestTaxiIndex = t;
            }

            System.out.println("\nTaxi " + (t + 1) + ": " + getTaxiName(t));
            System.out.println("------------------------------------------");
            System.out.println("  Reserved  : " + reserved + " / 7 seats");
            System.out.println("  Available : " + available + " seats");
            System.out.println("  Occupancy : " + percent + "%");
            System.out.println("  Fare/Seat : " + fares[t] + " NIS");
            System.out.println("  Revenue   : " + revenue + " NIS");
        }

        System.out.println("\n==========================================");
        System.out.println("SUMMARY");
        System.out.println("==========================================");

        if (maxPassengers == 0) {
            System.out.println("  No reservations yet. All taxis are empty.");
        } else {
            System.out.println("  Busiest Taxi  : " + getTaxiName(busiestTaxiIndex)
                    + " (" + maxPassengers + " passengers)");
        }

        System.out.println("  Total Revenue : " + totalSystemRevenue + " NIS");
        System.out.println("==========================================");
    }
}
