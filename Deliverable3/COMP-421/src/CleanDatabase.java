import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * CleanDatabase is a stored procedure using Java with a small UI based on the
 * SQL equivalent for the stored procedure.
 * 
 * Function: Prunes the database for the seasons or teams respectively when the
 * minimum number of teams in a season, or a minimum number of players in a team
 * are not met.
 * 
 * @author Maxim Gorshkov, Andrew Borodovski, James McCorriston
 * 
 */
public class CleanDatabase {

	// Global variables to describe the database.
	// Username to connect to the database.
	private String username;
	// Password to connect to the database.
	private String password;
	// URL at which the database can be accessed.
	private String dburl;
	// The connection which establishes queries.
	private static Connection con;

	/**
	 * Constructor for the CleanDatabase
	 * 
	 * @param uname
	 *            - Username as provided by user input.
	 * @param pword
	 *            - Password as provided by user input.
	 * @param url
	 *            - Database URL as provided by user input.
	 */
	public CleanDatabase(String uname, String pword, String url) {
		username = uname;
		password = pword;
		dburl = url;

		// Given the input from the user attempt to establish the connection
		// otherwise catch the error.
		try {
			con = connectDatabase();
		} catch (SQLException sqle) {
			System.err.println("Could not establish connection to " + dburl
					+ ". Please check login credentials.");
			System.exit(0);
		}

	}

	/**
	 * Establishes the connection to the DB2 database using the user inputs.
	 * 
	 * @return Connection to the database which if successful will be used as
	 *         the global variable con
	 * @throws SQLException
	 */
	private Connection connectDatabase() throws SQLException {
		/*
		 * Register the driver. If you are on the Trottier labs, this should not
		 * be a problem, otherwise, must VPN to SOCS
		 */

		try {
			DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
		} catch (Exception cnfe) {
			System.out.println("Class not found");
		}

		return DriverManager.getConnection(dburl, username, password);

	}

	/**
	 * Start takes in the user input. The appropriate menu options are processed
	 * in the processMenu() method from when called from start().
	 * 
	 * @throws SQLException
	 */
	private void start() throws SQLException {
		// Scanner for user input
		Scanner input = new Scanner(System.in);
		int userInput = 0;
		boolean exit = false;

		// Keeps giving the user options until the exit command is issued
		while (exit == false) {
			System.out
					.println("\nWelcome to JAM Sports and Rec. \nDatabase cleaning system");
			System.out.println("===================================");
			System.out.println("1 - Create procedure");
			System.out.println("2 - Call procedure on TEAM");
			System.out
					.println("\tAll teams where there are fewer players than the minimum are delete. \n\tTo be run after the deadline.");
			System.out.println("3 - Call procedure on SEASON");
			System.out
					.println("\tCleans all seasons where the registration deadline \n\thas passed but there are still less than 4 teams registered.");
			System.out.println("4 - Drop Procedure");
			System.out.println("5 - Exit Application");
			System.out.println("===================================");

			/*
			 * We ask the user for the input and first we see if the input is
			 * correct or if it needs to be changed to an integer.
			 * 
			 * If we have the exit condition, we set the exit boolean to true.
			 * Otherwise we execute the appropriate function.
			 */
			System.out.print("Make your selection: ");
			try {
				userInput = input.nextInt();
				exit = processMenu(userInput);
			} catch (InputMismatchException inputE) {
				System.out.println("Please enter the menu item as an integer.");
			}
		}
		// Now that we know that the user would like to quit, we close the
		// connection and exit.
		try {
			con.close();
		} catch (SQLException e) {
			System.err
					.println("Something went wrong with closing the connection. Were you connected to begin with?");
			System.exit(-1);
		}
		System.out.println("Exiting Now. Thank you for using JAM cleaner.");
		System.exit(0);
	}

	/**
	 * Processes the menu options based on the input form the user, calling the
	 * appropriate methods to run.
	 * 
	 * @param userInput
	 *            - integer value representing the user's choice.
	 * @return True/False : Indicates whether the exit condition was chosen to
	 *         break from loop
	 */
	private boolean processMenu(int userInput) {
		switch (userInput) {
		case 1:
			createProcedure();
			break;
		case 2:
			callProcedure("team");
			break;
		case 3:
			callProcedure("season");
			break;
		case 4:
			dropProcedure();
			break;
		case 5:
			return true;
		default:
			System.out.print("That was an incorrect selection. Exiting.");
			break;

		}
		return false;
	}

	/**
	 * Drops the procedure from the database. If the procedure is not intilized,
	 * exits with message.
	 */
	private void dropProcedure() {
		Statement stmt = null;
		// Try to drop procedure
		try {
			stmt = con.createStatement();
			// Drop procedure and success message
			stmt.execute("DROP PROCEDURE clean");
			System.out.println("Stored procedure was deleted.");
		}
		// Cannot drop the procedure so return a message and exit
		catch (SQLException ex) {
			System.err
					.println("The procedure was not initialized. Nothing to delete.");
		}

	}

	/**
	 * Based on whether the user selected the 'team' or 'season' option, calls
	 * the procedure to delete the appropriate records that are do not meet the
	 * minimum requirements.
	 * 
	 * Since we control that the only two options can be those specified in the
	 * procedure, we can return error of not being able to call the procedure
	 * based on the fact that it wasn't instantiated first.
	 */
	private void callProcedure(String selection) {
		Statement stmt = null;
		// Try to call the clean on either team or season
		try {
			stmt = con.createStatement();
			// If successful, perform pruning and write success message.
			stmt.execute("call clean('" + selection + "')");
			System.out.println("Stored procedure called on " + selection
					+ " successfully.");
			// Otherwise we return a message and quit to menu
		} catch (SQLException ex) {
			System.err
					.println("You need to create the procedure before calling it.");
		}

	}

	/**
	 * Creates the procedure in the database based on the SQL Stored Procedure.
	 * If the action cannot be performed, the procedure may already be defined
	 * with the same name in this database.
	 */
	private void createProcedure() {
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			// Try to execute the initialization of the procedure
			stmt.execute("create procedure clean (in coption varchar(20)) "
					+ "language sql "
					+ "begin  "
					+ "declare syear int;  "
					+ "declare sport varchar(30); "
					+ "declare llevel varchar(3); "
					+ "declare regdeadline date; "
					+ "declare counter int; "
					+ "declare maximum int; "
					+ "declare name varchar(30); "
					+ "declare thisSeason cursor for "
					+ "select syear, sport, llevel, regdeadline from season; "
					+ "declare thisTeam cursor for "
					+ "select name, syear, sport, llevel from team; "
					+ "if coption = 'season' then "
					+ "select count(*) into maximum from season; "
					+ "set counter = 0; "
					+ "open thisSeason; "
					+ "fetch_loop1: "
					+ "loop "
					+ "if counter = maximum then "
					+ "leave fetch_loop1; "
					+ "end if; "
					+ "fetch thisSeason into syear, sport, llevel, regdeadline; "
					+ "if regdeadline < current_date then "
					+ "if ((syear, sport, llevel) not in (select t.syear, t.sport, t.llevel "
					+ "from team t group by t.syear, t.sport, t.llevel "
					+ "having count(*) >= 4)) then  "
					+ "delete from season where current of thisSeason; "
					+ "end if; "
					+ "end if; "
					+ "set counter = counter + 1; "
					+ "end loop; "
					+ "close thisSeason; "
					+ "else if coption = 'team' then "
					+ "select count(*) into maximum from team; "
					+ "set counter = 0; "
					+ "open thisTeam; "
					+ "fetch_loop2: "
					+ "loop "
					+ "if counter = maximum then "
					+ "leave fetch_loop2; "
					+ "end if; "
					+ "fetch thisTeam into name, syear, sport, llevel; "
					+ "if ((name, syear, sport, llevel) not in (select p.name, p.syear, p.sport, p.llevel "
					+ "from playsfor p group by p.name, p.syear, p.sport, p.llevel "
					+ "having count (*) >= (select min(minplayers) from league "
					+ "where league.sport = p.sport and league.llevel = p.llevel))) then "
					+ "delete from team where current of thisTeam; "
					+ "end if; " + "set counter = counter + 1; " + "end loop; "
					+ "close thisTeam; " + "end if; " + "end if; " + "end");
			// Success message
			System.out.println("Stored procedure created successfully.");
		} catch (SQLException ex) {
			// If we're caught by an error then the procedure exists
			System.err
					.println("The procedure already exists. You can run the commands.");
		}
	}

	/**
	 * Main method for the client side interaction with the user.
	 */
	public static void main(String[] args) throws SQLException {

		/*
		 * Enter your credentials here
		 */

		String uname = "jmccor6";
		String pword = "aHG9dh6K";
		String url = "jdbc:db2://db2.cs.mcgill.ca:50000/cs421";
		
		CleanDatabase cleanJAM = new CleanDatabase(uname, pword, url);
		cleanJAM.start();

	}
}
