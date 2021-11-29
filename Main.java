import java.io.*;
import java.util.*;
import java.util.logging.*;

import org.apache.commons.cli.*;
import org.javalite.activejdbc.*;

import fi.iki.elonen.*;
import models.*;

public class Main extends NanoHTTPD {

	public static Map<String, String> envVariables = new Hashtable<String, String>();

	private static Logger logger = Logger.getLogger(Main.class.getName());
	private static Options options;
	private static int port;
	private Keycloak keycloak;
	public Main() throws IOException {
		super(port);
		this.keycloak = new Keycloak();
		logger.info("Running! Point your browsers to " + envVariables.get("myURL"));
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	/**
	 * precess CLI commands
	 * 
	 * @param args - arguments
	 */
	public static void CLI(String[] args) {
		options = new Options();
		Option n = Option.builder("n").argName("name").hasArg().required().desc("The name to say hello").build();
		options.addOption(n);

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption('n'))
				System.out.println("Hello " + cmd.getOptionValue('n'));
			else
				displayHelp();
		} catch (ParseException e) {
			System.err.println("Unexpected exception: " + e.getMessage());
			// e.printStackTrace();
			displayHelp();
		}
	}

	public static void main(String[] args) {
		try {
			Map<String, String> env = System.getenv();
//			System.out.println("----------------");
//			env.forEach((k, v) -> System.out.println(k + " " + v));
//			System.out.println("----------------");
			envVariables.putAll(env);

			// insert the systems.properties file
			Properties prp = new Properties();
			prp.load(new FileInputStream("system.properties"));
			prp.forEach((k, v) -> envVariables.put(k.toString(), v.toString()));
			// System.out.println("System eviorement variables");
			// envVariables.forEach((k, v) -> System.out.println(k + " " + v));
			String p = envVariables.get("local.port");
			port = Integer.parseInt(envVariables.getOrDefault("PORT", p));
			String host = envVariables.getOrDefault("host", "localhost");
			envVariables.put("myURL", "http://" + host + ":" + port);
			new Main();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "", e);
		}
	}

	/**
	 * display usahe help
	 */
	private static void displayHelp() {
		HelpFormatter help = new HelpFormatter();
		help.printHelp("Main", options);
	}
	@Override
	public Response serve(IHTTPSession session) {
		openDB();
		// Map<String, String> hds = session.getHeaders();
		// System.out.println("Headers\n------------------------");
		// hds.forEach((k, v) -> System.out.println(k + " " + v));

		Map<String, List<String>> parms = session.getParameters();
		// System.out.println("Parameters\n------------------------");
		// parms.forEach((k, v) -> System.out.println(k + " " + v));

		// remote server has no keycloack acces
		if (keycloak.isActive()) {
			// response sended by keylocker
			if (parms.containsKey("code") && parms.containsKey("session_state")) {
				keycloak.setCode(parms.get("code").get(0));
			}
			// not autenticated sesion
			if (keycloak.getToken("code").equals("")) {
				String aut = keycloak.getAuthorizationForUrl();
				Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
				r.addHeader("Location", aut);
				return r;
			}
		}

		// normal website operations
		String page = null;
		if (parms.get("user") == null) {
			page = readPage("index");
			page = page.replace("${alertMsg}", getAlertMsg("alert-secondary", "Info!",
					"Dieses Warnfeld wird die Antwort nach Überprüfung enthalten."));
			page = page.replace("${userElemets}", getSelectOptions());
		} else {
			String horse = (String) parms.get("user").get(0);
			String tit = "ACHTUNG !!";
			String tmp = "Der ausgewählte Element gehört zum den Dark side of the force.";
			String allerT = "alert-danger";
			Race race = Race.findFirst("rehorse = ?", horse);
			if (race.getInteger("restar_lane") == 0) {
				allerT = "alert-success";
				tit = "The Force is Strong in this one !!";
				tmp = "Der ausgewählte Element ist a Jedi.";
			}
			// if the user has a verified emal, cann see the Midi-chlorian count
			String em = keycloak.getToken("email_verified");
			String midic = "(Der Benutzer muss eine überprüfte E-Mail haben, um the Midi-chlorian zu sehen.)";
			if (em.equals("true")) {
				midic = race.getDouble("rejockey_weight").toString();
			}
			String msg = tmp + "<p>Master: " + race.getString("rejockey") + "<p>Midi-chlorian: " + midic
					+ "<p>Letzter Kampf: " + race.getDate("date");

			page = readPage("answer");
			page = page.replace("${reqestUser}", horse);
			page = page.replace("${alertMsg}", getAlertMsg(allerT, tit, msg));
		}

		return newFixedLengthResponse(page);
	}

	/**
	 * retrun the html fragment for alert mesage.
	 * 
	 * @param alertType - any of Bootstrap 5 Alerts: alert-success, alert-danger, alert-secondary ...
	 * @param title - message title
	 * @param msg - messge
	 * 
	 * @return html div fragment
	 */
	private String getAlertMsg(String alertType, String title, String msg) {
		String rmsg = "<div class='alert " + alertType + "'> " + "<strong>" + title + "</strong><p><p>" + msg
				+ "</div>";
		return rmsg;
	}

	/**
	 * return the elements for a <code>select</code> html form element
	 * 
	 * @return the list of elements in html format
	 */
	private String getSelectOptions() {
		String patt = "<option value='<Value>'><text></option>";
		String userElemets = "";
		// list of element in combobox
		List<String> horses = Race.findAll().orderBy("rehorse").collect("rehorse");
		for (String horse : horses) {
			userElemets += patt.replace("<Value>", horse).replace("<text>", horse) + "\n";
		}
		return userElemets;
	}

	/**
	 * 
	 * opent the DB
	 * 
	 */
	private void openDB() {
		try {
			Base.connection();
			return;
		} catch (Exception e) {
			// Exception mean db is not open
		}
		logger.info("Opening database... ");
		// String dbu = env.getOrDefault("JDBC_DATABASE_URL",
		// "jdbc:postgresql://localhost/" + dbName + "?user=postgres&password=root&ssl=false");

		// String locurl = "jdbc:postgresql://localhost/flicka2?user=postgres&password=root&ssl=false";
		String locurl = envVariables.get("db.url");
		String dbu = envVariables.getOrDefault("JDBC_DATABASE_URL", locurl);
		Base.open("org.postgresql.Driver", dbu, null);
		// logger.info("Database opened: " + dbu);
	}

	/**
	 * read and return a html page stored in html directory
	 * 
	 * @param pageName - name
	 * 
	 * @return page
	 */
	private String readPage(String pageName) {
		// nibusOAuth2.requestAuthorisation();
		String rPage = null;
		try {
			InputStream stream = getClass().getResourceAsStream("html/" + pageName + ".html");
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int count;
			while ((count = stream.read(buffer)) >= 0) {
				bytes.write(buffer, 0, count);
			}
			rPage = bytes.toString("UTF-8");

			// replace common variables
			rPage = rPage.replace("${given_name}", keycloak.getToken("given_name"));
			rPage = rPage.replace("${family_name}", keycloak.getToken("family_name"));

		} catch (Exception e) {
			logger.log(Level.SEVERE, "", e);
			rPage = "<html><body><h1>404</h1><p>Page Not found</p>\n";
		}
		return rPage;
	}
}
