import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.github.scribejava.apis.*;
import com.github.scribejava.core.builder.*;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.*;

public class KeycloakTest {

	private static Logger logger = Logger.getLogger(KeycloakTest.class.getName());

	public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
		// Replace these with your own api key, secret, callback, base url and realm
		final String apiKey = "backend.heroku";
		final String apiSecret = "your_api_secret";
		final String callback = "http://localhost:5001";
		final String baseUrl = "http://localhost:8080";
		final String realm = "myrealm";
		final String protectedResourceUrl = baseUrl + "/auth/realms/" + realm + "/protocol/openid-connect/userinfo";
		final OAuth20Service service = new ServiceBuilder(apiKey).apiSecret(apiSecret).defaultScope("openid")
				.callback(callback).build(KeycloakApi.instance(baseUrl, realm));

		// list of know endspoints
		final ArrayList<String> list = new ArrayList<>();
		list.add("http://localhost:8080/auth/realms/myrealm");
		list.add("http://localhost:8080/auth/realms/myrealm/protocol/openid-connect/auth");
		list.add("http://localhost:8080/auth/realms/myrealm/protocol/openid-connect/token");
		list.add("http://localhost:8080/auth/realms/myrealm/protocol/openid-connect/token/introspect");
		list.add("http://localhost:8080/auth/realms/myrealm/protocol/openid-connect/logout");
		list.add("http://localhost:8080/auth/realms/myrealm/protocol/openid-connect/certs");

		try {
			System.out.println("Fetching the Authorization URL...");
			final String authorizationUrl = service.getAuthorizationUrl();
			System.out.println("Got the Authorization URL!");
			System.out.println(authorizationUrl);
			System.out.println("Now go and authorize ScribeJava here:");

			Desktop desktop = java.awt.Desktop.getDesktop();
			desktop.browse(new URI(authorizationUrl));

			System.out.println(authorizationUrl);
			System.out.println("And paste the authorization code here");
			System.out.print(">>");
			final Scanner in = new Scanner(System.in);
			final String code = in.nextLine();
			in.close();
			System.out.println("Trading the Authorization Code for an Access Token...");
			final OAuth2AccessToken accessToken = service.getAccessToken(code);
			System.out.println("Got the Access Token!");
			System.out.println("(The raw response looks like this: " + accessToken.getRawResponse() + "')");

			// list of all know endpoints 
//			list.forEach(endp -> );
			// Now let's go and ask for a protected resource!
			System.out.println("Now we're going to access a protected resource...");
			System.out.println(protectedResourceUrl);
			final OAuthRequest request = new OAuthRequest(Verb.GET, protectedResourceUrl);
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			System.out.println("Got it! Lets see what we found...");
			System.out.println("" + response.getCode());
			System.out.println(response.getBody());
			logger.info("Thats it man! Go and build something awesome with ScribeJava! :)");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
