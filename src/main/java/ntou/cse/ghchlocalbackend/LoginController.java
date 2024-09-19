package ntou.cse.ghchlocalbackend;

import lombok.Getter;
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

    private final String CLIENT_ID = "Ov23ctAa63hp5klCm3oy";
    private final String CLIENT_SECRET = "8d86c3affa060b078b83ac3b083f3b1a5f225590";

    private final AppUserService appUserService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Getter
    private String gitHubToken;

    public LoginController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    // Redirect to GitHub login page.
    @GetMapping
    public RedirectView login() {
        String link = "https://github.com/login/oauth/authorize?client_id=" + CLIENT_ID;
        link += "&scope=repo, delete_repo";
        return new RedirectView(link);
    }

    // Callback function after authentication.
    @GetMapping("/callback")
    public RedirectView callback(@RequestParam("code") String code) {
        Map<?, ?> tokenData = exchangeCode(code);
        if (tokenData.containsKey("access_token")) {
            gitHubToken = (String) tokenData.get("access_token");
            System.out.println("token: " + gitHubToken);
            Map<?, ?> userInfo = userInfo(gitHubToken);
            String handle = (String) userInfo.get("login");
//            String name = (String) userInfo.get("name");
            AppUser currentAppUser = appUserService.loginAccount(handle);
            return new RedirectView("http://localhost:3000?id=" + currentAppUser.getId() + "&username=" + currentAppUser.getUsername() + "&token=" + gitHubToken);
//            return "Successfully authorized! Welcome, " + name + " (" + handle + ")";
//            return "Successfully authorized! Got code " + code + " and exchanged it for a user access token ending in " + token.substring(token.length() - 9);
        } else {
            return new RedirectView("http://localhost:3000");
//            return "Authorized, but unable to exchange code " + code + " for token.";
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
