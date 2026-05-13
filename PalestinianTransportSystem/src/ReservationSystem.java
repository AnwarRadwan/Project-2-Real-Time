/**
 * ============================================================
 *  ReservationSystem.java
 *  Palestinian Public Transport Reservation System
 *
 *  Routes managed:
 *    Taxi 1 : Hebron    → Ramallah  (fare: 30 NIS)
 *    Taxi 2 : Jenin     → Ramallah  (fare: 35 NIS)
 *    Taxi 3 : Jerusalem → Ramallah  (fare: 25 NIS)
 *
 *  Each taxi has 7 seats stored in a 2D Passenger array.
 *  No ArrayList is used anywhere in this project.
 * ============================================================
 */

import java.util.Scanner;

public class ReservationSystem {

    // ----------------------------------------------------------------
    //  Global Data Structures
    //  taxis[taxiIndex][seatIndex]
    //    taxiIndex : 0 = Hebron, 1 = Jenin, 2 = Jerusalem
    //    seatIndex : 0-6  (seats 1-7 shown to user)
    // ----------------------------------------------------------------
    static Passenger[][] taxis   = new Passenger[3][7];
    static Scanner       scanner = new Scanner(System.in);

    // Departure cities parallel to taxis array index
    static String[] departureCities = { "Hebron", "Jenin", "Jerusalem" };

    // Fare per seat for each taxi route (in NIS)
    static int[] fares = { 30, 35, 25 };

    // =================================================================
    //                            MAIN
    // =================================================================

    public static void main(String[] args) {

        printBanner();

        int choice;

        // Main program loop — runs until user selects Exit (option 8)
        do {
            printMenu();
            System.out.print("  Enter your choice: ");
            choice = safeReadInt();

            switch (choice) {
                case 1: reserveSeat();           break;
                case 2: cancelReservation();     break;
                case 3: searchByName();          break;
                case 4: searchByID();            break;
                case 5: displaySeat();           break;
                case 6: printAllReservations();  break;
                case 7: displayOccupancyReport();break;
                case 8:
                    System.out.println("\n  Thank you for using Palestinian Transport System.");
                    System.out.println("  Safe travels!\n");
                    break;
                default:
                    System.out.println("  [!] Invalid choice. Please enter a number from 1 to 8.");
            }

        } while (choice != 8);

        scanner.close();
    }

    // =================================================================
    //                    DISPLAY HELPERS
    // =================================================================

    /** Prints the welcome banner shown once at startup. */
    static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   Palestinian Public Transport Reservation System  ║");
        System.out.println("║      Serving: Hebron | Jenin | Jerusalem          ║");
        System.out.println("║              All routes → Ramallah                ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    /** Prints the numbered main menu. */
    static void printMenu() {
        System.out.println("\n  ┌─────────────────────────────────────┐");
        System.out.println("  │             MAIN MENU               │");
        System.out.println("  ├─────────────────────────────────────┤");
        System.out.println("  │  1.  Reserve a Seat                 │");
        System.out.println("  │  2.  Cancel Reservation             │");
        System.out.println("  │  3.  Search Passenger by Name       │");
        System.out.println("  │  4.  Search Passenger by ID         │");
        System.out.println("  │  5.  Display Passenger in a Seat    │");
        System.out.println("  │  6.  Print All Reservations         │");
        System.out.println("  │  7.  Occupancy & Fare Report        │");
        System.out.println("  │  8.  Exit                           │");
        System.out.println("  └─────────────────────────────────────┘");
    }

    // =================================================================
    //                REQUIRED HELPER METHOD 1
    //        getTaxiName — returns the full route name for a taxi
    // =================================================================

    /**
     * Returns the route label for the given taxi index.
     *
     * @param index  0 = Hebron, 1 = Jenin, 2 = Jerusalem
     * @return       Human-readable route string, e.g. "Hebron → Ramallah"
     */
    static String getTaxiName(int index) {
        if (index == 0) return "Hebron → Ramallah";
        if (index == 1) return "Jenin  → Ramallah";
        if (index == 2) return "Jerusalem → Ramallah";
        return "Unknown Route";
    }

    // =================================================================
    //                REQUIRED HELPER METHOD 2
    //     countReservedSeatsInTaxi — counts occupied seats in a taxi
    // =================================================================

    /**
     * Counts how many seats in a given taxi currently have a passenger.
     *
     * @param taxiIndex  0-based taxi index
     * @return           Number of non-null seats (0–7)
     */
    static int countReservedSeatsInTaxi(int taxiIndex) {
        int count = 0;
        // Iterate every seat slot; increment counter when a Passenger object exists
        for (int i = 0; i < 7; i++) {
            if (taxis[taxiIndex][i] != null) {
                count++;
            }
        }
        return count;
    }

    // =================================================================
    //                REQUIRED HELPER METHOD 3
    //          isValidName — validates a first or last name
    // =================================================================

    /**
     * Validates that a name:
     *   - Is not null or blank
     *   - Contains letters ONLY (no digits, spaces, or symbols)
     *   - Is at least 3 characters long
     *
     * @param name  The string to validate
     * @return      true if the name passes all checks, false otherwise
     */
    static boolean isValidName(String name) {
        if (name == null || name.trim().length() < 3) return false;
        for (int i = 0; i < name.length(); i++) {
            // Reject any character that is not an English or Arabic letter
            if (!Character.isLetter(name.charAt(i))) return false;
        }
        return true;
    }

    // =================================================================
    //                REQUIRED HELPER METHOD 4
    //         isValidID — validates a Palestinian national ID
    // =================================================================

    /**
     * Validates that a Palestinian ID:
     *   - Is exactly 9 characters long
     *   - Contains digits ONLY
     *
     * @param id  The ID string entered by the user
     * @return    true if valid, false otherwise
     */
    static boolean isValidID(String id) {
        if (id == null || id.length() != 9) return false;
        for (int i = 0; i < id.length(); i++) {
            if (!Character.isDigit(id.charAt(i))) return false;
        }
        return true;
    }

    // =================================================================
    //                REQUIRED HELPER METHOD 5
    //          isDuplicateID — checks for duplicate IDs system-wide
    // =================================================================

    /**
     * Searches every seat in every taxi to detect whether the given ID
     * is already registered.  Prevents the same person from booking twice.
     *
     * @param id  The Palestinian ID to check
     * @return    true if a matching ID is found, false if the ID is unique
     */
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

    // =================================================================
    //               SAFE INPUT UTILITIES
    // =================================================================

    /**
     * Reads an integer from stdin.  If the user types a non-integer,
     * the method re-prompts them until a valid number is entered.
     *
     * @return  A valid integer entered by the user
     */
    static int safeReadInt() {
        while (!scanner.hasNextInt()) {
            System.out.print("  [!] Please enter a valid number: ");
            scanner.next(); // discard the bad token
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // consume the trailing newline
        return value;
    }

    /**
     * Prompts the user to pick one of the three taxis.
     * Loops until a valid taxi number (1–3) is entered.
     *
     * @return  0-based taxi index (0, 1, or 2)
     */
    static int selectTaxi() {
        System.out.println("\n  Available Taxis:");
        for (int i = 0; i < 3; i++) {
            System.out.println("    " + (i + 1) + ". " + getTaxiName(i)
                    + "  [" + countReservedSeatsInTaxi(i) + "/7 reserved]");
        }

        int taxi;
        do {
            System.out.print("  Enter taxi number (1–3): ");
            taxi = safeReadInt();
            if (taxi < 1 || taxi > 3) {
                System.out.println("  [!] Invalid choice. Enter 1, 2, or 3.");
            }
        } while (taxi < 1 || taxi > 3);

        return taxi - 1; // convert to 0-based index
    }

    /**
     * Prompts the user to pick a seat from 1 to 7.
     * Loops until a valid seat number is entered.
     *
     * @return  0-based seat index (0–6)
     */
    static int selectSeat(int taxiIndex) {
        // Show seat availability for the chosen taxi
        System.out.print("  Seat map [" + getTaxiName(taxiIndex) + "]:  ");
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
            System.out.print("  Enter seat number (1–7): ");
            seat = safeReadInt();
            if (seat < 1 || seat > 7) {
                System.out.println("  [!] Invalid choice. Enter 1 to 7.");
            }
        } while (seat < 1 || seat > 7);

        return seat - 1; // convert to 0-based index
    }

    // =================================================================
    //             MENU OPTION 1 — RESERVE A SEAT
    // =================================================================

    /**
     * Guides the user through booking a seat:
     *   1. Choose taxi and seat
     *   2. Verify seat is empty
     *   3. Collect and validate passenger details
     *   4. Create Passenger object and store it in the array
     */
    static void reserveSeat() {
        System.out.println("\n  ── Reserve a Seat ──────────────────────────");

        int taxiIndex = selectTaxi();
        int seatIndex = selectSeat(taxiIndex);

        // Reject if seat is already occupied
        if (taxis[taxiIndex][seatIndex] != null) {
            System.out.println("  [!] Seat " + (seatIndex + 1) + " in "
                    + getTaxiName(taxiIndex) + " is already reserved.");
            return;
        }

        // --- Collect first name with validation loop ---
        String firstName;
        do {
            System.out.print("  Enter First Name: ");
            firstName = scanner.nextLine().trim();
            if (!isValidName(firstName)) {
                System.out.println("  [!] Invalid first name. Letters only, minimum 3 characters.");
            }
        } while (!isValidName(firstName));

        // --- Collect last name with validation loop ---
        String lastName;
        do {
            System.out.print("  Enter Last Name : ");
            lastName = scanner.nextLine().trim();
            if (!isValidName(lastName)) {
                System.out.println("  [!] Invalid last name. Letters only, minimum 3 characters.");
            }
        } while (!isValidName(lastName));

        // --- Collect Palestinian ID with validation and duplicate check ---
        String palestinianID;
        boolean idAccepted = false;
        do {
            System.out.print("  Enter Palestinian ID (9 digits): ");
            palestinianID = scanner.nextLine().trim();

            if (!isValidID(palestinianID)) {
                System.out.println("  [!] Invalid ID. Must be exactly 9 digits (numbers only).");
            } else if (isDuplicateID(palestinianID)) {
                // Prevent the same person from reserving more than one seat
                System.out.println("  [!] This ID is already registered in the system.");
                System.out.println("      Duplicate reservations are not allowed.");
            } else {
                idAccepted = true;
            }
        } while (!idAccepted);

        // Departure and destination are determined by the taxi route
        String departureCity   = departureCities[taxiIndex];
        String destinationCity = "Ramallah";

        // Create the Passenger object and place it into the 2D array
        taxis[taxiIndex][seatIndex] = new Passenger(
                firstName, lastName, palestinianID,
                departureCity, destinationCity);

        // Confirmation output
        System.out.println("\n  ✔ Reservation Successful!");
        System.out.println("  ┌────────────────────────────────────────┐");
        System.out.printf ("  │  Taxi  : %-30s│%n", getTaxiName(taxiIndex));
        System.out.printf ("  │  Seat  : %-30d│%n", (seatIndex + 1));
        System.out.printf ("  │  Fare  : %-28s│%n", fares[taxiIndex] + " NIS");
        System.out.println("  ├────────────────────────────────────────┤");
        taxis[taxiIndex][seatIndex].printPassengerInfo();
        System.out.println("  └────────────────────────────────────────┘");
    }

    // =================================================================
    //             MENU OPTION 2 — CANCEL RESERVATION
    // =================================================================

    /**
     * Cancels a reservation by setting the chosen seat to null.
     * Shows the passenger's details before removing them.
     */
    static void cancelReservation() {
        System.out.println("\n  ── Cancel Reservation ──────────────────────");

        int taxiIndex = selectTaxi();
        int seatIndex = selectSeat(taxiIndex);

        // Nothing to cancel if the seat is already empty
        if (taxis[taxiIndex][seatIndex] == null) {
            System.out.println("  [!] Seat " + (seatIndex + 1) + " is already empty. Nothing to cancel.");
            return;
        }

        // Show who is being removed before deleting
        System.out.println("\n  Passenger to be removed:");
        System.out.println("  ┌────────────────────────────────────────┐");
        taxis[taxiIndex][seatIndex].printPassengerInfo();
        System.out.println("  └────────────────────────────────────────┘");

        // Setting the seat to null frees it for future reservations
        taxis[taxiIndex][seatIndex] = null;

        System.out.println("  ✔ Reservation cancelled. Seat " + (seatIndex + 1)
                + " is now available.");
    }

    // =================================================================
    //             MENU OPTION 3 — SEARCH BY NAME
    // =================================================================

    /**
     * Searches every seat in every taxi for a passenger whose
     * full name contains the user's search string (case-insensitive).
     */
    static void searchByName() {
        System.out.println("\n  ── Search by Name ──────────────────────────");
        System.out.print("  Enter name to search: ");
        String query = scanner.nextLine().trim().toLowerCase();

        boolean found = false;

        // Scan all taxis and all seats for a matching name
        for (int t = 0; t < 3; t++) {
            for (int s = 0; s < 7; s++) {
                if (taxis[t][s] != null) {
                    String fullName = (taxis[t][s].getFirstName() + " "
                            + taxis[t][s].getLastName()).toLowerCase();

                    // Match if the query appears anywhere in the full name
                    if (fullName.contains(query)) {
                        System.out.println("\n  ✔ Match found:");
                        System.out.println("  ┌────────────────────────────────────────┐");
                        System.out.printf ("  │  Taxi : %-30s│%n", getTaxiName(t));
                        System.out.printf ("  │  Seat : %-30d│%n", (s + 1));
                        System.out.println("  ├────────────────────────────────────────┤");
                        taxis[t][s].printPassengerInfo();
                        System.out.println("  └────────────────────────────────────────┘");
                        found = true;
                    }
                }
            }
        }

        if (!found) {
            System.out.println("  No passenger found matching \"" + query + "\".");
        }
    }

    // =================================================================
    //             MENU OPTION 4 — SEARCH BY ID
    // =================================================================

    /**
     * Finds a passenger by their exact Palestinian ID number.
     * Searches across all taxis and all seats.
     */
    static void searchByID() {
        System.out.println("\n  ── Search by Palestinian ID ────────────────");
        System.out.print("  Enter Palestinian ID: ");
        String query = scanner.nextLine().trim();

        boolean found = false;

        for (int t = 0; t < 3; t++) {
            for (int s = 0; s < 7; s++) {
                // Use .equals() for exact string comparison (not ==)
                if (taxis[t][s] != null && taxis[t][s].getPalestinianID().equals(query)) {
                    System.out.println("\n  ✔ Passenger found:");
                    System.out.println("  ┌────────────────────────────────────────┐");
                    System.out.printf ("  │  Taxi : %-30s│%n", getTaxiName(t));
                    System.out.printf ("  │  Seat : %-30d│%n", (s + 1));
                    System.out.println("  ├────────────────────────────────────────┤");
                    taxis[t][s].printPassengerInfo();
                    System.out.println("  └────────────────────────────────────────┘");
                    found = true;
                }
            }
        }

        if (!found) {
            System.out.println("  No passenger found with ID: " + query);
        }
    }

    // =================================================================
    //             MENU OPTION 5 — DISPLAY PASSENGER IN A SEAT
    // =================================================================

    /**
     * Displays the details of the passenger in a specific seat,
     * or reports that the seat is empty.
     */
    static void displaySeat() {
        System.out.println("\n  ── Display Seat Information ────────────────");

        int taxiIndex = selectTaxi();
        int seatIndex = selectSeat(taxiIndex);

        if (taxis[taxiIndex][seatIndex] == null) {
            System.out.println("  Seat " + (seatIndex + 1) + " in "
                    + getTaxiName(taxiIndex) + " is currently EMPTY.");
        } else {
            System.out.println("\n  Seat " + (seatIndex + 1) + " | " + getTaxiName(taxiIndex));
            System.out.println("  ┌────────────────────────────────────────┐");
            taxis[taxiIndex][seatIndex].printPassengerInfo();
            System.out.println("  └────────────────────────────────────────┘");
        }
    }

    // =================================================================
    //             MENU OPTION 6 — PRINT ALL RESERVATIONS
    // =================================================================

    /**
     * Displays a full listing of all taxis, showing each seat's
     * status (occupied or empty) with passenger names and IDs.
     */
    static void printAllReservations() {
        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║         ALL CURRENT RESERVATIONS         ║");
        System.out.println("  ╚══════════════════════════════════════════╝");

        // Iterate through each taxi
        for (int t = 0; t < 3; t++) {
            int reserved = countReservedSeatsInTaxi(t);

            System.out.println("\n  Taxi " + (t + 1) + ": " + getTaxiName(t));
            System.out.println("  Reserved: " + reserved + " / 7 seats  |  "
                    + "Available: " + (7 - reserved) + " seats");
            System.out.println("  ─────────────────────────────────────────");

            for (int s = 0; s < 7; s++) {
                if (taxis[t][s] == null) {
                    System.out.printf("    Seat %d : [  EMPTY  ]%n", (s + 1));
                } else {
                    // Show seat number, full name, and ID on one line
                    System.out.printf("    Seat %d : %-20s  ID: %s%n",
                            (s + 1),
                            taxis[t][s].getFirstName() + " " + taxis[t][s].getLastName(),
                            taxis[t][s].getPalestinianID());
                }
            }
        }
    }

    // =================================================================
    //       MENU OPTION 7 (CUSTOM FEATURE) — OCCUPANCY & FARE REPORT
    //
    //  Shows per-taxi occupancy percentage, fare per seat, estimated
    //  total revenue, and highlights the busiest taxi in the system.
    // =================================================================

    /**
     * Custom Feature: Occupancy & Fare Report
     *
     * For each taxi this method displays:
     *   - Number of reserved vs. available seats
     *   - Occupancy percentage
     *   - Fare per seat (NIS)
     *   - Estimated revenue from current passengers
     *
     * It also identifies the busiest taxi (most passengers) and
     * reports the total system-wide revenue estimate.
     */
    static void displayOccupancyReport() {
        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║         OCCUPANCY & FARE REPORT          ║");
        System.out.println("  ╚══════════════════════════════════════════╝");

        int busiestTaxiIndex   = 0;
        int maxPassengers      = 0;
        int totalSystemRevenue = 0;

        // Process each taxi
        for (int t = 0; t < 3; t++) {
            int reserved   = countReservedSeatsInTaxi(t);
            int available  = 7 - reserved;
            double percent = (reserved / 7.0) * 100.0;
            int revenue    = reserved * fares[t];

            totalSystemRevenue += revenue;

            // Track the taxi with the most passengers for the "busiest" label
            if (reserved > maxPassengers) {
                maxPassengers    = reserved;
                busiestTaxiIndex = t;
            }

            System.out.println("\n  Taxi " + (t + 1) + ": " + getTaxiName(t));
            System.out.println("  ─────────────────────────────────────────");
            System.out.printf ("    Reserved     : %d / 7 seats%n",    reserved);
            System.out.printf ("    Available    : %d seats%n",        available);
            System.out.printf ("    Occupancy    : %.1f%%%n",          percent);
            System.out.printf ("    Fare/Seat    : %d NIS%n",          fares[t]);
            System.out.printf ("    Est. Revenue : %d NIS%n",          revenue);

            // Visual progress bar (each '█' represents one reserved seat)
            System.out.print ("    Seat Bar     : [");
            for (int s = 0; s < 7; s++) {
                System.out.print(taxis[t][s] != null ? "█" : "░");
            }
            System.out.println("]");
        }

        // Summary section
        System.out.println("\n  ═══════════════════════════════════════════");
        System.out.println("  SUMMARY");
        System.out.println("  ═══════════════════════════════════════════");

        if (maxPassengers == 0) {
            System.out.println("    No reservations yet. All taxis are empty.");
        } else {
            System.out.println("    Busiest Taxi   : " + getTaxiName(busiestTaxiIndex)
                    + " (" + maxPassengers + " passengers)");
        }

        System.out.printf ("    Total Revenue  : %d NIS%n", totalSystemRevenue);
        System.out.println("  ═══════════════════════════════════════════");
    }
}
