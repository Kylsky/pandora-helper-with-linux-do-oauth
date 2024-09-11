package fun.yeelo.oauth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class ApiUrlInjector {

    @Value("${linux-do.oauth2.client.registration.redirect-uri}")
    private String apiUrl;

    @PostConstruct
    public void injectApiUrl() throws IOException {
        String filePath = "src/main/resources/static/index.html";
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        content = content.replace("__API_URL__", apiUrl);
        Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }
}
