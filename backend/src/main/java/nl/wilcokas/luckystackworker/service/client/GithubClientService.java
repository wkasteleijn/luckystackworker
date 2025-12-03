package nl.wilcokas.luckystackworker.service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Service
public class GithubClientService {

    private final RestClient restClient;

    @Value("${github.api.url}")
    private String githubApiUrl;

    public String getAppInfo() {
        return restClient.get().uri(githubApiUrl).retrieve().body(String.class);
    }
}
