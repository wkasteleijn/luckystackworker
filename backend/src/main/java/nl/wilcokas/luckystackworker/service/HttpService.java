package nl.wilcokas.luckystackworker.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HttpService {
  public String sendHttpGetRequest(HttpClient.Version httpVersion, String url, int timeoutSeconds) {
    HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).version(httpVersion).build();
    HttpResponse<String> response = null;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        log.info("HTTP request successful");
        return response.body();
      } else {
        log.warn(
            "Unable to do HTTP request, got the following response code from server: {}",
            response.statusCode());
      }
    } catch (Exception e) {
      log.warn(
          "Unable to do HTTP request, got the following response code and message from server: {}, {}",
          response == null ? null : response.statusCode(),
          e.getMessage());
    }
    return null;
  }
}
