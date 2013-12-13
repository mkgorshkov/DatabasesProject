README:

In order to have the jdbc driver work on our personal windows machines,
we implemented the following:

1. Eclipse - Setting up the External JARs with the DB2 license
	The JARS in the "EXT_JAR" folder should be added as external jars to the Eclipse project.
		Right click on the project and select "Properties".
		Select the "Build Path" option on the left side.
		Click on the "Libraries" tab.
		Click on the "Add External JARs" on the right and select the files in "EXT_JAR" folder.
			
2. VPN SOCS Server - Setting up connection so driver can verify the license
	A VPN connection needs to be established to SOCS Servers (not McGill VPN)
		Go to the Control Panel and double click on the Network Connections icon.
		Double click on New Connection Wizard.
		Click the Next button.
		Select the Connect to the network at my workplace radio button and click the Next button.
		Select the Virtual Private Network connection radio button and click the Next button.
		Name the new VPN connection and click the Next button.
		Fill in pptp.cs.mcgill.ca as the hostname and click the Next button.
		Specify if this is for all users or not and click the Next button.
		Click the Finish button.
		
		As now this should part of your networking options, just connect to this VPN.
	
	For non-windows machines use more information on http://socsinfo.cs.mcgill.ca/wiki/VPN
	
2. Bonus Question
	For the bonus question we implemented the Stored Procedure in Java alongside the SQL procedure
	that we made for Question 2. It is run with a user interface from the .zip file which also contains
	question 3.