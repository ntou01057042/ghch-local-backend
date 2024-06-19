package ntou.cse.ghchlocalbackend;

import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AppUserService {

    @Getter
    private AppUser appUser;

    private final RestTemplate restTemplate = new RestTemplate();

    public AppUser loginAccount(String username) {
        AppUser newAppUserRequest = new AppUser();
        newAppUserRequest.setUsername(username);
        ResponseEntity<Void> createResponse = restTemplate.postForEntity(
                "http://localhost:8081/app-users",
                newAppUserRequest,
                Void.class
        );

        //
        HttpHeaders headers = createResponse.getHeaders();
        List<String> locationHeader = headers.get(HttpHeaders.LOCATION);
        if (locationHeader != null && !locationHeader.isEmpty()) {
            String locationUrl = locationHeader.get(0);
            ResponseEntity<AppUser> response = restTemplate.getForEntity(locationUrl, AppUser.class);
            appUser = response.getBody();
            System.out.println(appUser);
        }

        return appUser;
    }
}
