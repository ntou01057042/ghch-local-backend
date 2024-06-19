package ntou.cse.ghchlocalbackend;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    private final String CLIENT_ID = "Iv23liedhHScKQ2pfbdD";
    private final String CLIENT_SECRET = "2d09612e663f8aef8f2a05ad02cf5d3cc7681f4f";

    private final RestTemplate restTemplate = new RestTemplate();

    // Redirect to GitHub login page.
    @GetMapping
    public RedirectView login() {
        String link = "https://github.com/login/oauth/authorize?client_id=" + CLIENT_ID;
        return new RedirectView(link);
    }

    // Callback function after authentication.
    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code) {
        Map<?, ?> tokenData = exchangeCode(code);
        if (tokenData.containsKey("access_token")) {
            String token = (String) tokenData.get("access_token");
            System.out.println("Token: " + token);
            Map<?, ?> userInfo = userInfo(token);
            String handle = (String) userInfo.get("login");
            String name = (String) userInfo.get("name");
            return "Successfully authorized! Welcome, " + name + " (" + handle + ")";
//            return "Successfully authorized! Got code " + code + " and exchanged it for a user access token ending in " + token.substring(token.length() - 9);
        } else {
            return "Authorized, but unable to exchange code " + code + " for token.";
        }
    }

    // Exchange code parameter for user access token.
    private Map<?, ?> exchangeCode(final String code) {
        String url = "https://github.com/login/oauth/access_token";
        Map<String, String> params = new HashMap<>();
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("code", code);
        return restTemplate.postForObject(url, params, Map.class);
    }

    // Get information of user.
    private Map<?, ?> userInfo(final String token) {
        String url = "https://api.github.com/user";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        RequestEntity<Void> request = RequestEntity
                .get(url)
                .headers(headers)
                .build();
        return restTemplate.exchange(request, Map.class).getBody();
    }
}
