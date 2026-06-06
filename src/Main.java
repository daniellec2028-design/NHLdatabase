import java.sql.*;
import java.util.Scanner;

public class Main {
    private static void runSQLandPrint(String query, Connection conn) throws SQLException {
        System.out.println("Running SQL: " + query + "\n");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        ResultSetMetaData metaData = rs.getMetaData();
        int cols = metaData.getColumnCount();   //get the number of columns

        int[] colWidths = new int[cols];
        //initialize column widths with header lengths
        for (int i=0; i < cols; i++) {
            colWidths[i] = metaData.getColumnLabel(i+1).length();
        }
        //loop through results to get maximum lengths in each column
        while (rs.next()) {
            for (int i=0; i < colWidths.length; i++) {
                try {
                    int len = rs.getString(i+1).length();
                    if (len > colWidths[i])
                        colWidths[i] = len;
                } catch (NullPointerException e) {
                    //getString() can return NULL, so ignore them
                }
            }
        }
        //re-run query to return to the first row
        rs = stmt.executeQuery(query);
        metaData = rs.getMetaData();

        //print out column headers
        for (int i=1; i <= cols; i++) {
            System.out.printf("%-" + (colWidths[i-1]+1) + "s", metaData.getColumnLabel(i));
        }
        System.out.println();
        //print each row
        while (rs.next()) {
            for (int i=1; i <= cols; i++) {
                System.out.printf("%-" + (colWidths[i-1]+1) + "s", rs.getString(i));
            }
            System.out.println();
        }
    }

    public static void printMenu() {
        System.out.print("\nThis program demonstrates how to connect to a SQLite database ");
        System.out.print("(the sakila database in this case), ");
        System.out.println("run a query, and display the results.");
        System.out.println("\nWhat would you like to do?");
        System.out.println("1. List all players.");
        System.out.println("2. Search for a player.");
        System.out.println("3. List out all NHL teams.");
        System.out.println("4. Search for a team.");
        System.out.println("5. Find a goalie.");
        System.out.println("9. Quit");
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:NHL.db");
            while (true) {
                printMenu();
                String s = sc.nextLine();

                if (s.startsWith("1")) {
                    String sql = "SELECT firstName,lastName,primaryPosition FROM player";
                    runSQLandPrint(sql, conn);

                } else if (s.startsWith("2")) {
                    System.out.print("Enter a player's name: ");
                    String name = sc.nextLine();
                    String sql = "SELECT * "
                            + "FROM player WHERE concat(firstName, ' ', lastName)='"
                            + name + "'";
                    runSQLandPrint(sql, conn);

                    sql = "SELECT p.firstName, p.lastName, p.primaryPosition, " +
                            "s.total_goals, s.total_assists\n" +
                            "FROM player AS p\n" +
                            "INNER JOIN (SELECT player_id, SUM(goals) AS total_goals, " +
                            "SUM(assists) AS total_assists\n" +
                            "FROM game_skater_stats AS s\n" +
                            "GROUP BY player_id) AS s\n" +
                            "USING (player_id) WHERE concat(firstName, ' ', lastName)='"
                            + name + "'";
                    runSQLandPrint(sql, conn);
                }
                if (s.startsWith("3")) {
                    String sql = "SELECT city,teamName,abbreviation FROM team";
                    runSQLandPrint(sql, conn);
                } else if (s.startsWith("4")) {
                    System.out.print("Enter a team's name: ");
                    String name = sc.nextLine();
                    String sql = "SELECT * "
                            + "FROM team WHERE teamName='"
                            + name + "'";
                    runSQLandPrint(sql, conn);
                } else if (s.startsWith("5")) {
                    System.out.print("Enter a goalie's name: ");
                    String name = sc.nextLine();

                    String sql = "SELECT p.firstName, p.lastName, p.primaryPosition, s.saves, " +
                            "s.savePercentage\n" +
                            "FROM player AS p\n" +
                            "INNER JOIN (\n" +
                            "    SELECT player_id, SUM(saves) AS saves, AVG(savePercentage) AS savePercentage\n" +
                            "    FROM goalie_stats\n" +
                            "    GROUP BY player_id\n" +
                            ") AS s\n" +
                            "USING (player_id) WHERE concat(firstName, ' ', lastName)='"
                            + name + "'";

                    // --- CHANGED LOGIC START ---
                    // Create a statement to check if the query returns any records before printing
                    Statement checkStmt = conn.createStatement();
                    ResultSet checkRs = checkStmt.executeQuery(sql);

                    if (!checkRs.next()) {
                        // If there is no first row, the goalie does not exist or has no goalie stats
                        System.out.println("Goalie not found.");
                    } else {
                        // If data exists, pass the query to your existing printing engine
                        runSQLandPrint(sql, conn);
                    }
                    checkRs.close();
                    checkStmt.close();
                    // --- CHANGED LOGIC END ---

                } else if (s.startsWith("9")) {
                    break;
                } else {
                    System.out.println("Choice not recognized.");
                }
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}