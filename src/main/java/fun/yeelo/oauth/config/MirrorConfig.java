package fun.yeelo.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class MirrorConfig {
    @Value("${mirror.host}")
    private String mirrorHost;

    @Value("${mirror.password}")
    private String mirrorPwd;

    @Autowired
    private AccountService accountService;

    @Autowired
    private RestTemplate restTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    public HttpResult<ShareVO> getMirrorUrl(String username, Integer accountId) {
        Account account = accountService.getById(accountId);
        ShareVO res = new ShareVO();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!mirrorPwd.equals("-")) {
            headers.setBearerAuth(mirrorPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", username.length() < 4 ? username + "####" : username);
        if (Boolean.TRUE.equals(account.getConversationIsolated())) {
            personJsonObject.put("isolated_session", true);
        }else {
            personJsonObject.put("isolated_session", false);
        }
        personJsonObject.put("access_token", account.getAccessToken());

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(mirrorHost + "/api/login", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("user-gateway-token")) {
                String gatewayToken = map.get("user-gateway-token").toString();
                res.setAddress(mirrorHost + "/api/not-login?user_gateway_token=" + gatewayToken);
                res.setIsShared(true);
                return HttpResult.success(res);
            } else {
                return HttpResult.error("获取Gateway Token 异常");
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("系统内部异常");
        }
    }

    public HttpResult<ShareVO> getMirrorUrl(String username, Integer accountId, String customHost, String customPwd) {
        Account account = accountService.getById(accountId);
        ShareVO res = new ShareVO();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(customPwd)) {
            headers.setBearerAuth(customPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", username.length() < 4 ? username + "####" : username);
        if (Boolean.TRUE.equals(account.getConversationIsolated())) {
            personJsonObject.put("isolated_session", true);
        }else {
            personJsonObject.put("isolated_session", false);
        }
        personJsonObject.put("access_token", account.getAccessToken());

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(customHost + "/api/login", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("user-gateway-token")) {
                String gatewayToken = map.get("user-gateway-token").toString();
                res.setAddress(customHost + "/api/not-login?user_gateway_token=" + gatewayToken);
                res.setIsShared(true);
                return HttpResult.success(res);
            } else {
                return HttpResult.error("获取Gateway Token 异常");
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("系统内部异常");
        }
    }

    public HttpResult<String> getSimpleMirrorUrl(String username, Integer accountId) {
        Account account = accountService.getById(accountId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!mirrorPwd.equals("-")) {
            headers.setBearerAuth(mirrorPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", username.length() < 4 ? username + "####" : username);
        if (Boolean.TRUE.equals(account.getConversationIsolated())) {
            personJsonObject.put("isolated_session", true);
        }else {
            personJsonObject.put("isolated_session", false);
        }
        personJsonObject.put("access_token", account.getAccessToken());

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(mirrorHost + "/api/login", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("user-gateway-token")) {
                String gatewayToken = map.get("user-gateway-token").toString();
                return HttpResult.success(mirrorHost + "/api/not-login?user_gateway_token=" + gatewayToken);
            } else {
                return HttpResult.error("获取Gateway Token 异常");
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("系统内部异常");
        }
    }

    public HttpResult<String> getSimpleMirrorUrl(String username, Integer accountId, String customHost, String customPwd) {
        Account account = accountService.getById(accountId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(customPwd)) {
            headers.setBearerAuth(customPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");

        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", username.length() < 4 ? username + "####" : username);
        if (Boolean.TRUE.equals(account.getConversationIsolated())) {
            personJsonObject.put("isolated_session", true);
        }else {
            personJsonObject.put("isolated_session", false);
        }
        personJsonObject.put("access_token", account.getAccessToken());

        headers.setContentLength(personJsonObject.toString().getBytes().length);
        try {
            ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(customHost + "/api/login", new HttpEntity<>(personJsonObject, headers), String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("user-gateway-token")) {
                String gatewayToken = map.get("user-gateway-token").toString();
                return HttpResult.success(customHost + "/api/not-login?user_gateway_token=" + gatewayToken);
            } else {
                return HttpResult.error("获取Gateway Token 异常");
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("系统内部异常:{}", e.getMessage());
        }
    }
}
