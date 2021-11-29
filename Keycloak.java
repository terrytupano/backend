import java.util.*;
import java.util.logging.*;

import com.github.scribejava.apis.*;
import com.github.scribejava.core.builder.*;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.*;

public class Keycloak {

	private static final Logger logger = Logger.getLogger(Keycloak.class.getName());

	private String apiKey;
	private String apiSecret;
	private String callback;
	private String baseUrl;
	private String realm;
	private boolean isActive;
	private String protectedResourceUrl;
	private OAuth20Service service;
	private OAuth2AccessToken accessToken;
	private Hashtable<String, String> tokens = new Hashtable<>();

	public Keycloak() {
		this.apiKey = Main.envVariables.get("keycloack.apiKey");
		this.apiSecret = Main.envVariables.get("keycloack.apiSecret");
		this.callback = Main.envVariables.get("myURL");
		this.baseUrl = Main.envVariables.get("keycloack.baseUrl");
		this.realm = Main.envVariables.get("keycloack.realm");
		// userinfo
		// .well-known/uma2-configuration
		this.protectedResourceUrl = baseUrl + "/auth/realms/" + realm + "/protocol/openid-connect/userinfo";
		this.service = new ServiceBuilder(apiKey).apiSecret(apiSecret).defaultScope("openid").callback(callback)
				.build(KeycloakApi.instance(baseUrl, realm));
		
		// is active??
		isActive = Main.envVariables.get("keycloack.isActive").equals("true");
		logger.info("Keyclocker is " + (isActive ? "active" : "inactive"));
		}

	/**
	 * return <code>true</code> when the keycloack server is active. 
	 * 
	 * @return <code>true</code> for active server
	 */
	public boolean isActive() {
		return isActive;
	}
	
	// Obtain the Authorization URL
	public String getAuthorizationForUrl() {
		logger.info("Fetching the Authorization URL...");
		return service.getAuthorizationUrl();
	}

	/**
	 * return the value fo a token. or empty string if the token don.t exist
	 * 
	 * @param tokenId token
	 * @return value
	 */
	public String getToken(String tokenId) {
		String tmp = tokens.getOrDefault(tokenId, "");
		return tmp;
	}

	/**
	 * set the code senden by keycloack. After the code is setted, this method will retrive the basic user information
	 * from keycloak and stored it in the gloval variable {@link #tokens}
	 * 
	 * @param code - the Code
	 */
	public void setCode(String code) {
		try {
			logger.info("Trading the Authorization Code for an Access Token...");
			accessToken = service.getAccessToken(code);
			parseJson(accessToken.getRawResponse());
			logger.info("Retriving user information ...");
			logger.info(protectedResourceUrl);
			OAuthRequest request = new OAuthRequest(Verb.GET, protectedResourceUrl);
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			int rcode = response.getCode();
			logger.info("Return code: " + rcode);
			tokens.remove("code");
			if (rcode == 200) {
				parseJson(response.getBody());
				tokens.put("code", code);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "", e);
		}
	}

	// parse response
	private void parseJson(String json) {
		json = json.replace("{", "");
		json = json.replace("}", "");
		json = json.replace("\"", "");
		String kv[] = json.split("[,]");
		for (int i = 0; i < kv.length; i++) {
			String kv2[] = kv[i].split("[:]");
			tokens.put(kv2[0], kv2[1]);
		}
		// tokens.forEach((k, v) -> System.out.println(k + " " + v));
	}
}
