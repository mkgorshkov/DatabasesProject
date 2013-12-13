import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.*;

/**
 * User-friendly application program for the sports/recreation center database.
 * 
 * @author Maxim Gorshkov, Andrew Borodovski, James McCorriston
 */
public class DatabaseUI {
	
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
	 * Constructor for the DatabaseUI.
	 * @param uname - Username as provided by user input.
	 * @param pword - Password as provided by user input.
	 * @param url - Database URL as provided by user input.
	 */
	public DatabaseUI(String uname, String pword, String url){
		username = uname;
		password = pword;
		dburl = url;
		
		// Given the input from the user attempt to establish the connection otherwise catch the error.
		try {
			con = connectDatabase();
		} catch (SQLException sqle) {
			System.err.println("Could not establish connection to "+ dburl+ ". Please check login credentials.");
			System.exit(0);
		}
		
	}
	
	/**
	 * Establishes the connection to the DB2 database using the user inputs.
	 * @return Connection to the database which if successful will be used as the global variable con
	 * @throws SQLException
	 */
	private Connection connectDatabase() throws SQLException {
		/* Register the driver.
		 * If you are on the Trottier labs, this should not be a problem, otherwise, must VPN to SOCS
		 */
		
		try {
			DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());
		} catch (Exception cnfe) {
			System.out.println("Class not found");
		}

		return DriverManager.getConnection(dburl, username, password);
		
	}

	/**
	 * Gives the user the possible options which can be chosen, continuing to work through the
	 * various queries until the user issues the quitting command.
	 * @throws SQLException
	 */
	private void start() throws SQLException{
		
		// Set variables for interactivity
		// Variable to check if the user is at the exit condition
		boolean exit = false;
		// Gets response from the user.
		Scanner input;
		// Dictates which method to call based on response.
		int userInput = 0;

		// Work through the menu until a user selects to quit.
		while (exit == false) {
			input = new Scanner(System.in);

			System.out
					.println("\nWelcome to JAM Sports and Rec. \nPlease select one of the following:");
			System.out.println("===================================");
			System.out
					.println("1 - Look up registration information of a player");
			System.out.println("2 - Add a new player, coordinator, or official");
			System.out.println("3 - Promote player to captain");
			System.out.println("4 - Cancel/Reschedule an upcoming game");
			System.out.println("5 - Alter employee salary");
			System.out.println("6 - Exit Application");
			System.out.println("===================================");

			/*
			 * We ask the user for the input and first we see if the input is correct or if 
			 * it needs to be changed to an integer.
			 * 
			 * If we have the exit condition, we set the exit boolean to true. Otherwise we execute
			 * the appropriate function.
			 */
			System.out.print("Make your selection: ");
			try {
				userInput = input.nextInt();
				exit = processMenu(userInput);
			} catch (InputMismatchException inputE) {
				System.out.println("Please enter the menu item as an integer.");
			}

		}
		
		// Now that we know that the user would like to quit, we close the connection and exit.
		try {
			con.close();
		} catch (SQLException e) {
			System.err.println("Something went wrong with closing the connection. Were you connected to begin with?");
			System.exit(-1);
		}
		System.out.println("Exiting Now. Thank you for using JAM.");
		System.exit(0);
	}
	
	/**
	 * Processes the user input requests. Executes the appropriate query or modification
	 * and returns a boolean as to whether it was an exit request.
	 * @param pUserInput - the response that the user game at the menu.
	 * @return Boolean - True if exit condition, False otherwise.
	 * @throws SQLException
	 */
	private boolean processMenu(int pUserInput) throws SQLException {
		switch (pUserInput) {
		case 1: playerLookup();
			break;
		case 2: addNewRecord();
			break;
		case 3: promoteCaptain();
			break;
		case 4: deleteMoveGame();
			break;
		case 5: alterSalary();
			break;
		case 6:
			return true;
		default:
			System.out.print("That was an incorrect selection. Please try again");
			break;

		}
		return false;
	}
	
	/**
	 * Given a statement to update a table in the form of a String, the query is either performed with the
	 * appropriate response or tables returned or an exception is thrown.
	 * @param querySQL
	 * @throws SQLException
	 */
	private boolean executeUpdate(String querySQL) throws SQLException{
		Statement statement = con.createStatement();
		int sqlCode;
		String sqlState = "00000";
		
		// Querying a table
		try {
			statement.executeUpdate(querySQL);
			return true;
			
		} catch (SQLException e) {
			sqlCode = e.getErrorCode(); // Get SQLCODE
			sqlState = e.getSQLState(); // Get SQLSTATE

			if(sqlState.equals("23505")){
				System.out.println("A record already exists with this ID...");
			}
		}
		
		statement.close();
		return false;
	}
	
	/**
	 * Given a statement to execute in the form of a String, the query is either performed with the
	 * appropriate response or tables returned or an exception is thrown.
	 * @param querySQL
	 * @throws SQLException
	 */
	private ResultSet executeQuery(String querySQL) throws SQLException{
		Statement statement = con.createStatement();
		ResultSet sqlResponse = null;
		int sqlCode = 0;
		String sqlState = "00000";
		
		// Querying a table
		try {
			sqlResponse = statement.executeQuery(querySQL);
			return sqlResponse;
			
		} catch (SQLException e) {
			sqlCode = e.getErrorCode(); // Get SQLCODE
			sqlState = e.getSQLState(); // Get SQLSTATE

			// Your code to handle errors comes here;
			// something more meaningful than a print would be good
			System.out.println("Code: " + sqlCode + "  sqlState: " + sqlState);
		}
		
		statement.close();
		return null;
	}

	/**
	 * Player lookup has the function of querying the database for a specific player or players
	 * using the fields that the Player table has. Only one field or a combination of the fields
	 * can be used for lookup.
	 */
	private void playerLookup(){
		// Takes the input from the user
		Scanner input = new Scanner(System.in);
		int userInput = 0;
		// Each query will have a spot in the array in case there are multiples
		String[] qSearch = new String[9];
		// The SQL query which will be executed.
		String sqlQuery = "";
		// The response to be received.
		ResultSet sqlResponse = null;
		// The response makes sense
		boolean validResponse = true;
		
		System.out.println("\n===================================");
		System.out.println("Player Lookup");
		System.out.println("===================================");
		System.out.println("By what field would you like to look up a player?");
		System.out.println("1 - PID");
		System.out.println("2 - Gender");
		System.out.println("3 - Last Name");
		System.out.println("4 - First Name");
		System.out.println("5 - Address");
		System.out.println("6 - Phone Number");
		System.out.println("7 - Email");
		System.out.println("8 - Birthday");
		System.out.println("9 - Date Created");
		System.out.println("**For a combination write a sequence of the above (ex. 34 for Last Name and First Name)");
		
		// We make sure that the user puts the correct input ex. 1 number
		System.out.print("Make your selection: ");
		try {
			userInput = input.nextInt();
		} catch (InputMismatchException inputE) {
			System.out.println("Please enter the menu item as an integer.");
		}
		
		// To make it easier to execute in order parse in a string, so we know the length
		String executionOrder = ""+userInput;
		
		// Go through and execute one by one
		for(int i = 0; i<executionOrder.length(); i++){
			// Offset from character reprsentation of int to actual int
			int execute = executionOrder.charAt(i) - 48;
			
			// We take input form user one by one for the different queries. If there is nothing for the
			// attribute to be queried we take the value in, otherwise we return that the user cannot
			// use two values.
			input = new Scanner(System.in);
			switch (execute) {
			case 1: System.out.println("Enter a PID to search: ");
					if(qSearch[0] == null || qSearch[0].isEmpty()){
					qSearch[0] = "PID = " + input.nextLine();
					}else{
						System.out.println("You can't search for two values for the same attribute, using "+qSearch[0]);
					}
				break;
			case 2: System.out.println("Enter a Gender to search: ");
			if(qSearch[1] == null || qSearch[1].isEmpty()){
				qSearch[1] = "Gender = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[1]);
				}
				break;
			case 3: System.out.println("Enter a Last Name to search: ");
			if(qSearch[2] == null || qSearch[2].isEmpty()){
				qSearch[2] = "Lname = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[2]);
				}
				break;
			case 4: System.out.println("Enter a First Name to search: ");
			if(qSearch[3] == null || qSearch[3].isEmpty()){
				qSearch[3] = "Fname = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[3]);
				}
				break;
			case 5: System.out.println("Enter an Address to search: ");
			if(qSearch[4] == null || qSearch[4].isEmpty()){
				qSearch[4] = "Address = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[4]);
				}
				break;
			case 6: System.out.println("Enter a Phone Number to search: ");
			if(qSearch[5] == null || qSearch[5].isEmpty()){
				qSearch[5] = "Phonenumber = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[5]);
				}
				break;
			case 7: System.out.println("Enter a Email to search: ");
			if(qSearch[6] == null || qSearch[6].isEmpty()){
				qSearch[6] = "Email = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[6]);
				}
				break;
			case 8: System.out.println("Enter a birthday to search (YYYY-MM-DD): ");
			if(qSearch[7] == null || qSearch[7].isEmpty()){
				qSearch[7] = "Birthday = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[7]);
				}
				break;
			case 9: System.out.println("Enter a date created to search (YYYY-MM-DD): ");
			if(qSearch[8] == null || qSearch[8].isEmpty()){
				qSearch[8] = "Datecreated = '" + input.nextLine() +"'";
				}else{
					System.out.println("You can't search for two values for the same attribute, using "+qSearch[8]);
				}
				break;
			default:
				System.out.print("That was an incorrect selection. Not executing the function "+execute+".");
				validResponse = false;
				break;

			}
			
		}
		
		// Now that we have our executions, we can now perform the queries.
		// We concatenate all that needs to be sent to the database.
		if(validResponse){
		sqlQuery = ""+"SELECT * FROM Player WHERE ";
		int sqlCount = 0;
		for(int i = 0; i<qSearch.length; i++){
			if(qSearch[i] != null && sqlCount == 0){
				sqlQuery +="(" + qSearch[i];
				sqlCount++;
			}
			else if(qSearch[i] != null && sqlCount > 0){
				sqlQuery +=" AND " + qSearch[i];
			}
		}
		sqlQuery += ")";
		
		// Gather the response from the SQL Query and being to output to user.
		try {
			sqlResponse = executeQuery(sqlQuery);
			
			// If we didn't return any records, it means non exist.
			if(!sqlResponse.next() || sqlResponse == null){
				System.out.println("No records exist for your specifications. Try again with a coarser search.");
				System.out.println("Perhaps your input was incorrect in some way?");
			}	
			// Otherwise we show the user what the records return.
			else{
				do{
				int pid = sqlResponse.getInt(1);
				String gender = sqlResponse.getString(2);
				String lname = sqlResponse.getString(3);
				String fname = sqlResponse.getString(4);
				String addr = sqlResponse.getString(5);
				String phone = sqlResponse.getString(6);
				String email = sqlResponse.getString(7);
				String birthday = sqlResponse.getString(8);
				String created = sqlResponse.getString(9);
				
				System.out.println("PID :" + pid);
				System.out.println("Gender :" + gender);
				System.out.println("Last Name :" + lname);
				System.out.println("First Name :" + fname);
				System.out.println("Address :" + addr);
				System.out.println("Phone Number :" + phone);
				System.out.println("Email :" + email);
				System.out.println("Birthday :" + birthday);
				System.out.println("Created :" + created);
				System.out.println("-----");
			}while(sqlResponse.next());
			}
		// If something didn't work along the way, we catch it.
		} catch (SQLException sqlE) {
			System.err.println("Cannot execute the query please try again.");
		}
		}
	}
	
	/**
	 * Adds a new record to the database based on the user's choice of either the Player, Coordinator
	 * or Official category.
	 * 
	 * Recursively, we go through each field and add the record as necessary.
	 * @throws SQLException 
	 */
	private void addNewRecord() throws SQLException{
		// Takes the input from the user
				Scanner input = new Scanner(System.in);
				int userInput = 0;
				// Each entry into a attribute is an element in the array
				String[] newPlayer = new String[9];
				String[] newOfficial = new String[10];
				String[] newCoordinator = new String[10];
				// The SQL query which will be executed.
				String sqlQuery = "";
				// The response to be received.
				
				System.out.println("\n===================================");
				System.out.println("Add a Record");
				System.out.println("===================================");
				System.out.println("What type of record would you like to add?");
				System.out.println("1 - Player");
				System.out.println("2 - Official");
				System.out.println("3 - Coordinator");
				
				// We make sure that the user puts the correct input - an integer
				System.out.print("Make your selection: ");
				try {
					userInput = input.nextInt();
				} catch (InputMismatchException inputE) {
					System.out.println("Please enter the menu item as an integer.");
				}
				
				if(userInput == 1){
					System.out.println("We'll now begin to create a new Player.");
					// Take in the PID of a user, make sure that it is an integer between the 
					// constraing values that we have in the database.
					System.out.println("Enter a PID [Between 260400000 and 260500000]: ");
					try {
						int tempInput = input.nextInt();
						if(tempInput > 260400000 && tempInput < 260500000){
							newPlayer[0] = ""+tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					} catch (InputMismatchException inputE) {
						System.out.println("Not valid input. PID's are digits only. Back to main menu.");
					}
					
					// Take in the gender of the user, make sure that it's only one character in length
					// and specifies either a male or a female.
					if(newPlayer[0] != null){
						System.out.println("Enter a gender [m or f]: ");
						String tempInput = input.next();
						if(tempInput.length() == 1 && (tempInput.charAt(0) == 'm' || tempInput.charAt(0) == 'f')){
							newPlayer[1] = tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					}
					
					// Take in the last name of the user and check to make sure that it's the proper length.
					if(newPlayer[1] != null){
						System.out.println("Enter a last name [up to 25 characters with first letter capital]: ");
						String tempInput = input.next();
						if(tempInput.length() <= 25 && (tempInput.charAt(0) >= 'A' && tempInput.charAt(0) <= 'Z')){
							newPlayer[2] = tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					
					}
					
					// Take in the first name of the user and check to make sure that it's the proper length
					if(newPlayer[2] != null){
					System.out.println("Enter a first name [up to 25 characters with first letter capital]: ");
					String tempInput = input.next();
					if(tempInput.length() <= 25 && (tempInput.charAt(0) >= 'A' && tempInput.charAt(0) <= 'Z')){
						newPlayer[3] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
				
					}
					
					//Take in the address of the user and make sure that it's the proper length
					if(newPlayer[3] != null){
					input = new Scanner(System.in);
					System.out.println("Enter an address [up to 100 characters]: ");
					String tempInput = input.nextLine();
					if(tempInput.length() <= 100){
						newPlayer[4] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					}
					
					// Take in the phone number of the user and make sure it's a proper length
					if(newPlayer[4] != null){
					System.out.println("Enter a phone number [10 digits]: ");
					
					try {
						String tempInput = input.next();
						// Get a reguar expression to check for digits since it's too large for int
						if(tempInput.length() == 10 && (tempInput.matches("^[0-9]{10,10}$"))){
							newPlayer[5] = ""+tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					} catch (InputMismatchException inputE) {
						System.out.println("Not valid input. Phone nubers are digits only. Back to main menu.");
					}
					
					}
					
					// Take in the email of a user and make sure it's a proper length and has proper symbols
					if(newPlayer[5] != null){
					System.out.println("Enter an email [up to 50 characters]: ");
					
					String tempInput = input.next();
					if(tempInput.length() <= 50 && tempInput.contains("@") && tempInput.contains(".")){
						newPlayer[6] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					
					}
					
					// Take in the birthday of the user and make sure it's the proper format
					if(newPlayer[6] != null){
					
					System.out.println("Enter a birthday [YYYY-MM-DD]: ");
					
					// Pull out some regular expressions on this...
					final String DatePattern = "^\\d{4}-\\d{2}-\\d{2}$";
					final Pattern pattern = Pattern.compile(DatePattern);
					
					String tempInput = input.next();
					
					final Matcher matcher = pattern.matcher(tempInput);
					
				
					if(matcher.matches()){
						newPlayer[7] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					}
					
					// Generated the date created based on today's date.
					if(newPlayer[7] != null){
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						Date date = new Date();
						newPlayer[8] = "" + dateFormat.format(date);
						
						
						sqlQuery = "INSERT INTO Player VALUES ("+ Integer.parseInt(newPlayer[0]) +
								",'"+newPlayer[1]+"','"+newPlayer[2]+"','"+newPlayer[3]+"','"+newPlayer[4]+
								"','"+newPlayer[5]+"','"+newPlayer[6]+"','"+newPlayer[7]+"','"+newPlayer[8]+
								"')";
						
						if(executeUpdate(sqlQuery)){
							System.out.println("Entered a new Player successfully.");
						}
						else{
							System.out.println("Could not enter new Player.");
						}
									
					}
					
					
					
				}
				
				// If we're not creating a player, and the user selects that they want to create
				// an official, we continue onwards
				else if(userInput == 2){
					System.out.println("We'll now begin to create a new Official.");
					// Take in the PID of a user, make sure that it is an integer between the 
					// constraing values that we have in the database.
					System.out.println("Enter a OID [Between 300000 and 400000]: ");
					try {
						int tempInput = input.nextInt();
						if(tempInput > 300000 && tempInput < 400000){
							newOfficial[0] = ""+tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					} catch (InputMismatchException inputE) {
						System.out.println("Not valid input. PID's are digits only. Back to main menu.");
					}
					
					// Take in the gender of the user, make sure that it's only one character in length
					// and specifies either a male or a female.
					if(newOfficial[0] != null){
						System.out.println("Enter a gender [m or f]: ");
						String tempInput = input.next();
						if(tempInput.length() == 1 && (tempInput.charAt(0) == 'm' || tempInput.charAt(0) == 'f')){
							newOfficial[1] = tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					}
					
					// Take in the last name of the user and check to make sure that it's the proper length.
					if(newOfficial[1] != null){
						System.out.println("Enter a last name [up to 25 characters with first letter capital]: ");
						String tempInput = input.next();
						if(tempInput.length() <= 25 && (tempInput.charAt(0) >= 'A' && tempInput.charAt(0) <= 'Z')){
							newOfficial[2] = tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					
					}
					
					// Take in the first name of the user and check to make sure that it's the proper length
					if(newOfficial[2] != null){
					System.out.println("Enter a first name [up to 25 characters with first letter capital]: ");
					String tempInput = input.next();
					if(tempInput.length() <= 25 && (tempInput.charAt(0) >= 'A' && tempInput.charAt(0) <= 'Z')){
						newOfficial[3] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
				
					}
					
					//Take in the address of the user and make sure that it's the proper length
					if(newOfficial[3] != null){
					input = new Scanner(System.in);
					System.out.println("Enter an address [up to 100 characters]: ");
					String tempInput = input.nextLine();
					if(tempInput.length() <= 100){
						newOfficial[4] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					}
					
					// Take in the phone number of the user and make sure it's a proper length
					if(newOfficial[4] != null){
					System.out.println("Enter a phone number [10 digits]: ");
					
					try {
						String tempInput = input.next();
						// Get a reguar expression to check for digits since it's too large for int
						if(tempInput.length() == 10 && (tempInput.matches("^[0-9]{10,10}$"))){
							newOfficial[5] = ""+tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					} catch (InputMismatchException inputE) {
						System.out.println("Not valid input. Phone nubers are digits only. Back to main menu.");
					}
					
					}
					
					// Take in the email of a user and make sure it's a proper length and has proper symbols
					if(newOfficial[5] != null){
					System.out.println("Enter an email [up to 50 characters]: ");
					
					String tempInput = input.next();
					if(tempInput.length() <= 50 && tempInput.contains("@") && tempInput.contains(".")){
						newOfficial[6] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					
					}
					
					// Take in the birthday of the user and make sure it's the proper format
					if(newOfficial[6] != null){
					
					System.out.println("Enter a birthday [YYYY-MM-DD]: ");
					
					// Pull out some regular expressions on this...
					final String DatePattern = "^\\d{4}-\\d{2}-\\d{2}$";
					final Pattern pattern = Pattern.compile(DatePattern);
					
					String tempInput = input.next();
					
					final Matcher matcher = pattern.matcher(tempInput);
					
				
					if(matcher.matches()){
						newOfficial[7] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					}
					
					if(newOfficial[7] != null){
						System.out.println("Enter a salary: ");
						try {
							int tempInput = input.nextInt();
							newOfficial[9] = ""+tempInput;
						} catch (InputMismatchException inputE) {
							System.out.println("Not valid input. Back to main menu.");
						}
					}
					
					// Generated the date created based on today's date.
					if(newOfficial[9] != null){
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						Date date = new Date();
						newOfficial[8] = "" + dateFormat.format(date);
					
						
						sqlQuery = "INSERT INTO Official VALUES ("+ Integer.parseInt(newOfficial[0]) +
								",'"+newOfficial[1]+"','"+newOfficial[2]+"','"+newOfficial[3]+"','"+newOfficial[4]+
								"','"+newOfficial[5]+"','"+newOfficial[6]+"','"+newOfficial[7]+"','"+newOfficial[8]+
								"',"+newOfficial[9]+")";
						
						if(executeUpdate(sqlQuery)){
							System.out.println("Entered a new Official successfully.");
						}
						else{
							System.out.println("Could not enter new Official.");
						}
									
					}
				}
				
				// Given that we're adding a new coordinator, we recursively ask the user for information
				// regarding the new coordinator.
				else if(userInput == 3){
					System.out.println("We'll now begin to create a new Coordinator.");
					// Take in the PID of a user, make sure that it is an integer between the 
					// constraing values that we have in the database.
					System.out.println("Enter a CID [Between 540000 and 550000]: ");
					try {
						int tempInput = input.nextInt();
						if(tempInput > 540000 && tempInput < 550000){
							newCoordinator[0] = ""+tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					} catch (InputMismatchException inputE) {
						System.out.println("Not valid input. PID's are digits only. Back to main menu.");
					}
					
					// Take in the gender of the user, make sure that it's only one character in length
					// and specifies either a male or a female.
					if(newCoordinator[0] != null){
						System.out.println("Enter a gender [m or f]: ");
						String tempInput = input.next();
						if(tempInput.length() == 1 && (tempInput.charAt(0) == 'm' || tempInput.charAt(0) == 'f')){
							newCoordinator[1] = tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					}
					
					// Take in the last name of the user and check to make sure that it's the proper length.
					if(newCoordinator[1] != null){
						System.out.println("Enter a last name [up to 25 characters with first letter capital]: ");
						String tempInput = input.next();
						if(tempInput.length() <= 25 && (tempInput.charAt(0) >= 'A' && tempInput.charAt(0) <= 'Z')){
							newCoordinator[2] = tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					
					}
					
					// Take in the first name of the user and check to make sure that it's the proper length
					if(newCoordinator[2] != null){
					System.out.println("Enter a first name [up to 25 characters with first letter capital]: ");
					String tempInput = input.next();
					if(tempInput.length() <= 25 && (tempInput.charAt(0) >= 'A' && tempInput.charAt(0) <= 'Z')){
						newCoordinator[3] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
				
					}
					
					//Take in the address of the user and make sure that it's the proper length
					if(newCoordinator[3] != null){
					input = new Scanner(System.in);
					System.out.println("Enter an address [up to 100 characters]: ");
					String tempInput = input.nextLine();
					if(tempInput.length() <= 100){
						newCoordinator[4] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					}
					
					// Take in the phone number of the user and make sure it's a proper length
					if(newCoordinator[4] != null){
					System.out.println("Enter a phone number [10 digits]: ");
					
					try {
						String tempInput = input.next();
						// Get a reguar expression to check for digits since it's too large for int
						if(tempInput.length() == 10 && (tempInput.matches("^[0-9]{10,10}$"))){
							newCoordinator[5] = ""+tempInput;
						}
						else{
							System.out.println("Not valid input. Back to main menu.");
						}
					} catch (InputMismatchException inputE) {
						System.out.println("Not valid input. Phone nubers are digits only. Back to main menu.");
					}
					
					}
					
					// Take in the email of a user and make sure it's a proper length and has proper symbols
					if(newCoordinator[5] != null){
					System.out.println("Enter an email [up to 50 characters]: ");
					
					String tempInput = input.next();
					if(tempInput.length() <= 50 && tempInput.contains("@") && tempInput.contains(".")){
						newCoordinator[6] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					
					}
					
					// Take in the birthday of the user and make sure it's the proper format
					if(newCoordinator[6] != null){
					
					System.out.println("Enter a birthday [YYYY-MM-DD]: ");
					
					// Pull out some regular expressions on this...
					final String DatePattern = "^\\d{4}-\\d{2}-\\d{2}$";
					final Pattern pattern = Pattern.compile(DatePattern);
					
					String tempInput = input.next();
					
					final Matcher matcher = pattern.matcher(tempInput);
					
				
					if(matcher.matches()){
						newCoordinator[7] = tempInput;
					}
					else{
						System.out.println("Not valid input. Back to main menu.");
					}
					}
					
					if(newCoordinator[7] != null){
						System.out.println("Enter a yearly salary: ");
						try {
							int tempInput = input.nextInt();
							newCoordinator[9] = ""+tempInput;
						} catch (InputMismatchException inputE) {
							System.out.println("Not valid input. Back to main menu.");
						}
					}
					
					// Generated the date created based on today's date.
					if(newCoordinator[9] != null){
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						Date date = new Date();
						newCoordinator[8] = "" + dateFormat.format(date);
					
						
						sqlQuery = "INSERT INTO Coordinator VALUES ("+ Integer.parseInt(newCoordinator[0]) +
								",'"+newCoordinator[1]+"','"+newCoordinator[2]+"','"+newCoordinator[3]+"','"+newCoordinator[4]+
								"','"+newCoordinator[5]+"','"+newCoordinator[6]+"','"+newCoordinator[7]+"','"+newCoordinator[8]+
								"',"+newCoordinator[9]+")";
						
						if(executeUpdate(sqlQuery)){
							System.out.println("Entered a new Coordinator successfully.");
						}
						else{
							System.out.println("Could not enter new Coordinator");
						}
									
					}
				}
				else{
					System.out.println("That was an inappropriate choice. Please try again.");
				}
				//input.close();
	}
	

	/**
	 * Presents the user with all of the upcoming games and allows the user to
	 * cancel an upcoming game and inform the captains of the teams which are to
	 * to play or move the game to a different date.
	 * 
	 * @throws SQLException
	 */
	private void deleteMoveGame() throws SQLException {
		// Takes the input from the user
		boolean hasGames = false;

		Scanner input = new Scanner(System.in);
		// Each query will have a spot in the array in case there are multiples
		ArrayList<String> Dates = new ArrayList<String>();
		// The SQL query which will be executed.
		String sqlQuery = "";
		// The response to be received.
		ResultSet sqlResponse = null;
		String gameDate = "";
		String team1 = "";
		String team2 = "";

		System.out.println("\n===================================");
		System.out.println("Move or Delete a Game");
		System.out.println("===================================");
		System.out.println("Here are the upcoming games:");

		// Get today's date so that we can check for the upcoming games from
		// today onward.
		String dateToAdd = "";

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();

		sqlQuery += "Select gdate, gtime, sport, llevel, name1, name2 from HasTeams where gdate > '"
				+ dateFormat.format(date) + "'";

		// Gather the response from the SQL Query and being to output to user.
		try {
			sqlResponse = executeQuery(sqlQuery);

			// If we didn't return any records, it means non exist.
			if (!sqlResponse.next()) {
				System.out.println("There are no upcoming games after "
						+ dateFormat.format(date));
				System.out.println("Perhaps you should add some.");
			}
			// Otherwise we show the user what the records return.
			else {
				do {
					hasGames = true;
					// Assign SQL output to string variables
					gameDate = sqlResponse.getString(1);
					Dates.add(gameDate); // We also add the dates so we can
											// refer to them without having to
											// reindex
					String gameTime = sqlResponse.getString(2);
					String sport = sqlResponse.getString(3);
					String level = sqlResponse.getString(4);
					team1 = sqlResponse.getString(5);
					team2 = sqlResponse.getString(6);

					// Begin output to use, since we know that there must be at
					// least one game.
					System.out.println("Upcoming games: After "
							+ dateFormat.format(date));
					System.out.println("-----");

					System.out.println("Game Date :" + gameDate);
					System.out.println("Game Time :" + gameTime);
					System.out.println("Sport :" + sport);
					System.out.println("League Level :" + level);
					System.out.println("Team 1 :" + team1);
					System.out.println("Team 2 :" + team2);
					System.out.println("-----");
				} while (sqlResponse.next());
			}
			// If something didn't work along the way, we catch it.
		} catch (SQLException sqlE) {
			System.err.println("Cannot execute the query please try again.");
		}

		if (hasGames) {
			input = new Scanner(System.in);

			System.out
					.println("Would you like to delete or move an existing game from above?");
			System.out.println("1 - Delete Game");
			System.out.println("2 - Move Game");
			System.out.println("Make your selection: ");

			try {
				int tempInput = input.nextInt();
				// Check if if we would like to delete a game
				if (tempInput == 1) {
					input = new Scanner(System.in);

					System.out
							.println("Enter the date of the game you would like to delete from above.");
					System.out.println("Date of Game (YYYY-MM-DD): ");
					// Take in the date of the game to be deleted
					String dateInput = input.nextLine();

					// If the date is valid continue
					if (Dates.contains(dateInput)) {

						sqlQuery = "Delete from Game where gdate = '"
								+ dateInput + "'";

						// Try to do the deletion and report
						if (executeUpdate(sqlQuery)) {
							System.out.println("Deleted game successfully.");

							int messageID = 0;

							input = new Scanner(System.in);
							System.out
									.println("Input a message ID [between 1000000 and 2000000]: ");
							try {
								int tempMsgID = input.nextInt();
								if (tempMsgID > 1000000 && tempInput < 2000000) {
									messageID = tempMsgID;
								} else {
									System.out
											.println("Not valid input. Back to main menu.");
								}
							} catch (InputMismatchException inputE) {
								System.out
										.println("Not valid input. Message id's are integers only. Back to main menu.");
							}

							if (messageID > 0) {
								System.out
										.println("The following message was sent:");
								System.out
										.println("Captains, your upcoming game for "
												+ gameDate
												+ " between "
												+ team1
												+ " and "
												+ team2
												+ " was cancelled.");

								sqlQuery = "Insert into Announcement values ("
										+ messageID
										+ ",'Captains, your upcoming game for "
										+ gameDate + " between " + team1
										+ " and " + team2
										+ " was cancelled', '"
										+ dateFormat.format(date) + "')";

								if (executeUpdate(sqlQuery)) {
									System.out
											.println("Message sent successfully.");
								}
							}

						} else {
							System.out.println("Could not delete game.");
						}

					} else {
						System.out
								.println("That wasn't one of the upcoming games. Back to main menu.");
					}

					// We now either deleted or we go back to the main menu
				}
				// We're moving the game instead of deleting it.
				else if (tempInput == 2) {
					input = new Scanner(System.in);

					System.out
							.println("Enter the date of the game you would like to modify from above.");
					System.out.println("Date of Game (YYYY-MM-DD): ");

					// We take in the date of the game the user wants to change.
					// If it's in the arraylist,
					// it was a valid date, and we can change it. Otherwise, it
					// wasn't on the list.
					String dateInput = input.nextLine();
					if (Dates.contains(dateInput)) {

						// Enter date and time to be reschedule the game for.
						System.out
								.println("Enter a date to reschedule the game (YYYY-MM-DD): ");
						String newDate = input.nextLine();

						final String DatePattern = "^\\d{4}-\\d{2}-\\d{2}$";
						final String TimePattern = "([01]?[0-9]|2[0-3]):[0-5][0-9]";

						final Pattern pattern = Pattern.compile(DatePattern);
						final Pattern pattern2 = Pattern.compile(TimePattern);

						final Matcher matcher = pattern.matcher(newDate);

						// If it matches the date format, carry on.
						if (matcher.matches()) {
							System.out
									.println("Enter a time to reschedule the game (HH:MM): ");
							String newTime = input.nextLine();
							final Matcher matcher2 = pattern2.matcher(newTime);

							// If it matches the time format, carry on
							if (matcher2.matches()) {

								sqlQuery = "Select * from hasteams where gdate = '"
										+ dateInput + "'";

								try {
									ResultSet a = executeQuery(sqlQuery);

									if (!a.next() || a == null) {
										System.out
												.println("Couldn't load the information on the game...");
									} else {

										String Sport = a.getString(3);
										String Llevel = a.getString(4);
										String Name1 = a.getString(5);
										String Syear1 = a.getString(6);
										String Sport1 = a.getString(7);
										String Llevel1 = a.getString(8);
										String Name2 = a.getString(9);
										String Syear2 = a.getString(10);
										String Sport2 = a.getString(11);
										String Llevel2 = a.getString(12);

										// We construct the insert for game
										// based on what we got from the
										// hasteams table
										sqlQuery = "Insert into Game values ('"
												+ newTime + ":00', '" + newDate
												+ "', '" + Sport + "' , '"
												+ Llevel + "')";

										if (executeUpdate(sqlQuery)) {
											System.out
													.println("Added new game successfully to Games.");

										} else {
											System.out
													.println("Could not add new game to Games.");
										}

										// Insert the new game into hasteams
										sqlQuery = "Insert into HasTeams values ('"
												+ newTime
												+ ":00', '"
												+ newDate
												+ "', '"
												+ Sport
												+ "','"
												+ Llevel
												+ "', '"
												+ Name1
												+ "', "
												+ Syear1
												+ ", '"
												+ Sport1
												+ "', '"
												+ Llevel1
												+ "', '"
												+ Name2
												+ "', "
												+ Syear2
												+ ", '"
												+ Sport2
												+ "', '" + Llevel2 + "') ";

										if (executeUpdate(sqlQuery)) {
											System.out
													.println("Added teams and new game successfully to HasTeams.");

											// We would like to assign the same
											// official to the new game, giving
											// him/her the same role
											sqlQuery = "Select * from Officiates where gdate = '"
													+ a.getString(2)
													+ "' and gtime = '"
													+ a.getString(1) + "'";
											ResultSet b = executeQuery(sqlQuery);

											if (!b.next() || b == null) {
												System.out
														.println("Couldn't load the information on the officials...");
											} else {

												String oid = b.getString(1);
												String ofSport = b.getString(4);
												String ofLevel = b.getString(5);
												String role = b.getString(6);

												sqlQuery = "Insert into Officiates values ("
														+ oid
														+ ", '"
														+ newTime
														+ ":00' , '"
														+ newDate
														+ "', '"
														+ ofSport
														+ "', '"
														+ ofLevel
														+ "', '" + role + "')";

												if (executeUpdate(sqlQuery)) {
													System.out
															.println("Added new official information to new game. ");

													System.out
															.println("Moved game to "
																	+ newDate
																	+ " at "
																	+ newTime);

													sqlQuery = "Delete from Game where gdate = '"
															+ dateInput + "'";
													if (executeUpdate(sqlQuery)) {
														System.out
																.println("Deleted old game successfully.");

														int messageID = 0;

														input = new Scanner(
																System.in);
														System.out
																.println("Input a message ID [between 1000000 and 2000000]: ");
														try {
															int tempMsgID = input
																	.nextInt();
															if (tempMsgID > 1000000
																	&& tempInput < 2000000) {
																messageID = tempMsgID;
															} else {
																System.out
																		.println("Not valid input. Back to main menu.");
															}
														} catch (InputMismatchException inputE) {
															System.out
																	.println("Not valid input. Message id's are integers only. Back to main menu.");
														}

														if (messageID > 0) {
															System.out
																	.println("The following message was sent:");
															System.out
																	.println("Captains, your upcoming game for "
																			+ gameDate
																			+ " between "
																			+ team1
																			+ " and "
																			+ team2
																			+ " was cancelled.");

															sqlQuery = "Insert into Announcement values ("
																	+ messageID
																	+ ",'Captains, your upcoming game for "
																	+ gameDate
																	+ " between "
																	+ team1
																	+ " and "
																	+ team2
																	+ " was moved to "
																	+ newDate
																	+ " at "
																	+ newTime
																	+ ".' , '"
																	+ dateFormat
																			.format(date)
																	+ "')";

															if (executeUpdate(sqlQuery)) {
																System.out
																		.println("Message sent successfully.");
															}
														}
													} else {
														System.out
																.println("Could not delete old game.");
													}

												} else {
													System.out
															.println("Could not add new official information.");
												}

											}

										} else {
											System.out
													.println("Could not add new game to HasTeams.");
										}

									}
								} catch (SQLException sqlE) {
									System.err
											.println("Cannot execute the query please try again.");
									System.out.println(sqlE.toString());
								}

							} else {
								System.out
										.println("The time is not valid. Returning to main.");
							}
						} else {
							System.out
									.println("The date is not valid. Returning to main.");
						}

					} else {
						System.out
								.println("That wasn't one of the upcoming games. Back to main menu.");
					}

				} else {
					System.out.println("Not valid input. Back to main menu.");
				}
			} catch (InputMismatchException inputE) {
				System.out
						.println("Not valid input. Integers only. Back to main menu.");
			}
		}

	}
	
	/**Searches for a player to promote to captain
	 * 
	 */
	private void promoteCaptain(){

		Scanner input = new Scanner(System.in);
		int userInput;
		long longInput, max = Long.parseLong("9999999999999999"), min = Long.parseLong("1000000000000000");
		boolean validSelection;
		Date current = new Date();
		String textInput;
		String[] cptnInfo = new String[6];
		// The SQL query which will be executed.
		String sqlQuery = "";
		// The response to be received.
		ResultSet sqlResponse;
		
		//Regular expressions for card date input format
		final String DatePattern = "^\\d{4}-\\d{2}-\\d{2}$";
		final Pattern pattern = Pattern.compile(DatePattern);
		Matcher matcher;
		
		try
		{
			sqlResponse = executeQuery("SELECT P.pid, P.fname, P.lname FROM Player P WHERE P.pid not in (SELECT C.cptnid FROM Captain C)");
			if (!sqlResponse.next()) {
				System.out.println("There are no Players in the database");
			}
			else {
				System.out.println("Players available to be promoted to captain:");
				System.out.println("ID        NAME");
				do {
					System.out.println(sqlResponse.getString(1) + " " + sqlResponse.getString(2) + " " + sqlResponse.getString(3));
				} while (sqlResponse.next());
				
				System.out.println("------------------------------------------------");
				System.out.println("Enter the ID of the player you would like to promote (0 to cancel):");
				userInput = input.nextInt();
				try
				{
					do{
					sqlResponse = executeQuery("SELECT P.pid FROM Player P WHERE P.pid=" + userInput +" AND P.pid not in (SELECT C.cptnid FROM Captain C)");
					validSelection = sqlResponse.next();
					
						if(!validSelection && userInput != 0)
						{
							System.out.println("Please select a valid ID");
							userInput = input.nextInt();
						}
					}while(!validSelection && userInput != 0);

					if(userInput != 0)
					{
						//Stores the ID
						cptnInfo[0] = "" + userInput;
						input = new Scanner(System.in);
						//billing address
						System.out.println("Please enter the billing address of this player [up to 100 characters]:");
						textInput = input.nextLine();
						while(textInput.length() > 100){
							System.out.println("Address is too long, please try again:");
							textInput = input.nextLine();
						}
						cptnInfo[1] = textInput;
						
						//card number
						System.out.println("Please enter the player's card number (16 digits, must not begin with a zero):");
						longInput = input.nextLong();
						while(longInput < min || longInput > max){
							System.out.println("Invalid number, try again:");
							longInput = input.nextLong();
						}
						cptnInfo[2] = "" + longInput;
						
						//card holder name
						System.out.println("Please enter the name of the card holder as printed on the card [up to 50 characters]:");
						textInput = input.nextLine();
						while(textInput.length() > 50){
							System.out.println("Address is too long, please try again:");
							textInput = input.nextLine();
						}
						cptnInfo[3] = textInput;
						
						//expiration date
						do{	
							System.out.println("Card expiration date [YYYY-MM]: ");
							
							textInput = input.next() + "-01";
							if(Integer.parseInt(textInput.split("-")[1]) > 12 ||Integer.parseInt(textInput.split("-")[1]) < 1){
								validSelection = false;
							}
							else{
								validSelection = true;
							}
							matcher = pattern.matcher(textInput);
							if(!matcher.matches() || !validSelection)
							{
								System.out.println("Invalid expiration date, please try again.");
							}
						}while(!matcher.matches() || !validSelection);
								
						cptnInfo[4] = textInput;
						
						//card type
						input = new Scanner(System.in);
						System.out.println("Please enter the card type [up to 25 characters]:");
						textInput = input.nextLine();
						while(textInput.length() > 25){
							System.out.println("Input is too long, please try again:");
							textInput = input.nextLine();
						}
						cptnInfo[5] = textInput;
						
						sqlQuery = ("INSERT INTO Captain VALUES (" + cptnInfo[0] + ", '" + cptnInfo[1] + "', '" +
									cptnInfo[2] + "', '" + cptnInfo[3] + "', '" + cptnInfo[4] + "', '" + cptnInfo[5] + "')");
						if(executeUpdate(sqlQuery)){
							System.out.println("Player successfully promoted.");
						}
						else{
							System.out.println("Promotion failed.");
						}
					}
				}
				catch (SQLException sqlE) {
					System.err.println("Cannot execute the query please try again.");
				}
			}
		}
		catch (SQLException sqlE) {
			System.err.println("Cannot execute the query please try again.");
		}
	}
	
	/**
	 * Alter the salary of an employee by either a percentage or a flat value.
	 */
	private void alterSalary()
	{
		Scanner input = new Scanner(System.in);
		int userInput, id, salary;
		ResultSet sqlResponse, sqlResponse2;
		boolean hasOfficials, hasCoordinators, validSelection, increase, flat, coord=false, execution;
		String textInput;
		double doubInput;
		
		try
		{
			//coordinators
			sqlResponse = executeQuery("SELECT cid, fname, lname, yearlysal FROM Coordinator");
			sqlResponse2 = executeQuery("SELECT oid, fname, lname, hourlysal FROM Official");
			hasCoordinators = sqlResponse.next();
			hasOfficials = sqlResponse2.next();
			if (!hasOfficials && !hasCoordinators) {
				System.out.println("There are no coordinators or officials in the database");
			}
			else {
				if(hasCoordinators)
				{
					System.out.println("Coordinators:");
					System.out.println("ID      YEARLY SALARY	NAME");
					do {
						System.out.println(sqlResponse.getString(1) + "  " + sqlResponse.getString(4) + "            " + sqlResponse.getString(2) + " " + sqlResponse.getString(3));
					} while (sqlResponse.next());
				}
				else {
					System.out.println("There are no coordinators in the database.");
				}
				if(hasOfficials)
				{
					System.out.println("Officials:");
					System.out.println("ID      HOURLY SALARY	NAME");
					do {
						System.out.println(sqlResponse2.getString(1) + "  " + sqlResponse2.getString(4) + "               " + sqlResponse2.getString(2) + " " + sqlResponse2.getString(3));
					} while (sqlResponse2.next());
				}
				else{
					System.out.println("There are no Officials in the database");
				}
				System.out.println("------------------------------------------------");
				System.out.println("Enter the ID of the Coordinator or Official whose salary you would like to change (0 to cancel):");
				userInput = input.nextInt();
				try
				{
					do{
					sqlResponse = executeQuery("SELECT cid FROM Coordinator  WHERE cid=" + userInput);
					sqlResponse2 = executeQuery("SELECT oid FROM Official WHERE oid=" + userInput);
					validSelection = sqlResponse.next() || sqlResponse2.next();
					
						if(!validSelection && userInput != 0)
						{
							System.out.println("Please select a valid ID");
							userInput = input.nextInt();
						}
					}while(!validSelection && userInput != 0);

					if(userInput != 0)
					{
						id = userInput;
						if(id > 540000)
						{
							coord = true;
						}
						System.out.println("Would you like to increase[i] or decrease[d] their salary?");
						textInput = input.next();
						while(!textInput.equals("i") && !textInput.equals("d"))
						{
							System.out.println("Please input 'i' or 'd'");
							textInput = input.next();
						}
						if(textInput.equals("i"))
						{
							increase = true;
							System.out.println("Would you like to increase the salary by a flat rate[f] or by a percentage[p]?");
						}
						else
						{
							increase = false;
							System.out.println("Would you like to decrease the salary by a flat rate[f] or by a percentage[p]?");
						}
						textInput = input.next();
						while(!textInput.equals("f") && !textInput.equals("p"))
						{
							System.out.println("Please input 'f' or 'p'");
							textInput = input.next();
						}
						if(textInput.equals("f"))
						{
							flat = true;	
							if(coord)
							{
								if(increase)
									System.out.println("By how much would you like to increase the salary (per year)?");
								else
									System.out.println("By how much would you like to decrease the salary (per year)?");
							}
							else
							{
								if(increase)
									System.out.println("By how much would you like to increase the salary (per hour)?");
								else
									System.out.println("By how much would you like to decrease the salary (per hour)?");
							}
						}
						else
						{
							flat = false;
							if(coord)
							{
								if(increase)
									System.out.println("By what percentage would you like to increase the salary (per year)?");
								else
									System.out.println("By what percentage would you like to decrease the salary (per year)?");
							}
							else
							{
								if(increase)
									System.out.println("By what percentage would you like to increase the salary (per hour)?");
								else
									System.out.println("By what percentage would you like to decrease the salary (per hour)?");
							}
						}
						doubInput = input.nextDouble();
						while(doubInput < 0)
						{
							System.out.println("Please enter a positive value.");
							doubInput = input.nextDouble();
						}

						if(coord)
						{
							sqlResponse = executeQuery("SELECT yearlysal FROM Coordinator WHERE cid =" + id);
							sqlResponse.next();
							salary = sqlResponse.getInt(1);
						}
						else
						{
							sqlResponse2 = executeQuery("SELECT hourlysal FROM Official WHERE oid =" + id);
							sqlResponse2.next();
							salary = sqlResponse2.getInt(1);
						}
						
						if(flat)
						{
							if(increase)
							{
								salary = salary + (int)Math.round(doubInput);
								if(coord)
									execution = executeUpdate("UPDATE Coordinator SET yearlysal =" + salary + " WHERE cid = " + id);
								else
									execution = executeUpdate("UPDATE Official SET hourlysal =" + salary + " WHERE oid = " + id);
							}
							else
							{
								salary = salary - (int)Math.round(doubInput);
								if(coord)
									execution = executeUpdate("UPDATE Coordinator SET yearlysal =" + salary + " WHERE cid = " + id);
								else
									execution = executeUpdate("UPDATE Official SET hourlysal =" + salary + " WHERE oid = " + id);
							}
						}
						else
						{
							if(increase)
							{
								salary = salary + (int)Math.round(salary * (doubInput/100));
								if(coord)
									execution = executeUpdate("UPDATE Coordinator SET yearlysal =" + salary + " WHERE cid = " + id);
								else
									execution = executeUpdate("UPDATE Official SET hourlysal =" + salary + " WHERE oid = " + id);
							}
							else
							{
								salary = salary - (int)Math.round(salary * (doubInput/100));
								if(coord)
									execution = executeUpdate("UPDATE Coordinator SET yearlysal =" + salary + " WHERE cid = " + id);
								else
									execution = executeUpdate("UPDATE Official SET hourlysal =" + salary + " WHERE oid = " + id);
							}
						}
						//Determines if the update was successful or not.
						if(execution){
							if(coord)
								System.out.println("Salary successfully updated. New salary = $" + salary + "/year");
							else
								System.out.println("Salary successfully updated. New salary = $" + salary + "/hour");
						}
						else{
							System.out.println("Update failed.");
						}
					}
				}
				catch (SQLException sqlE) {
					System.err.println("Cannot execute the query please try again.");
				}
			}
		}
		catch (SQLException sqlE) {
			System.err.println("Cannot execute the query please try again.");
		}

	}
	
	/**
	 * Main method for the client side interaction with the user.
	 */
	public static void main(String[] args) throws SQLException {
		
		/* 
		 * Enter your credentials here
		 */
		
		/**
		String uname = "cs421g30";
		String pword = "Cs421T3_30";
		String url = "jdbc:db2://db2.cs.mcgill.ca:50000/cs421";
		**/
		
		String uname = "jmccor6";
		String pword = "aHG9dh6K";
		String url = "jdbc:db2://db2.cs.mcgill.ca:50000/cs421";
		
		DatabaseUI JAM = new DatabaseUI(uname, pword, url);
		JAM.start();
		
	}
}
