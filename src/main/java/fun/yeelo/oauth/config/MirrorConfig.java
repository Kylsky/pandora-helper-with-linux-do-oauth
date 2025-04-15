package fun.yeelo.oauth.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareGrokConfig;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.GrokConfigService;
import fun.yeelo.oauth.service.ShareService;
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
    private String gptMirrorHost;

    @Value("${mirror.password}")
    private String gptMirrorPwd;

    @Value("${mirror.grok.host}")
    private String grokMirrorHost;

    @Value("${mirror.grok.password}")
    private String grokMirrorPwd;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private RestTemplate restTemplate;

    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private GrokConfigService grokConfigService;

    public HttpResult<ShareVO> getMirrorUrl(String username, Integer accountId) {
        Account account = accountService.getById(accountId);
        ShareVO res = new ShareVO();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!gptMirrorPwd.equals("-")) {
            headers.setBearerAuth(gptMirrorPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", username.length() < 4 ? username + "####" : username);
        if (Boolean.TRUE.equals(account.getConversationIsolated())) {
            personJsonObject.put("isolated_session", true);
        }else {
            personJsonObject.put("isolated_session", false);
        }
        Share user = shareService.getByUserName(username);
        if (StringUtils.hasText(user.getProxyUrl())) {
            personJsonObject.put("proxy_url",user.getProxyUrl());
        }
        personJsonObject.put("access_token", account.getAccessToken());

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(gptMirrorHost + "/api/login", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("user-gateway-token")) {
                String gatewayToken = map.get("user-gateway-token").toString();
                res.setAddress(gptMirrorHost + "/api/not-login?user_gateway_token=" + gatewayToken);
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
        Share user = shareService.getByUserName(username);
        if (StringUtils.hasText(user.getProxyUrl())) {
            personJsonObject.put("proxy_url",user.getProxyUrl());
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
        if (!gptMirrorPwd.equals("-")) {
            headers.setBearerAuth(gptMirrorPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", username.length() < 4 ? username + "####" : username);
        if (Boolean.TRUE.equals(account.getConversationIsolated())) {
            personJsonObject.put("isolated_session", true);
        }else {
            personJsonObject.put("isolated_session", false);
        }
        Share user = shareService.getByUserName(username);
        if (StringUtils.hasText(user.getProxyUrl())) {
            personJsonObject.put("proxy_url",user.getProxyUrl());
        }
        personJsonObject.put("access_token", account.getAccessToken());

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(gptMirrorHost + "/api/login", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("user-gateway-token")) {
                String gatewayToken = map.get("user-gateway-token").toString();
                return HttpResult.success(gptMirrorHost + "/api/not-login?user_gateway_token=" + gatewayToken);
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
        Share user = shareService.getByUserName(username);
        if (StringUtils.hasText(user.getProxyUrl())) {
            personJsonObject.put("proxy_url",user.getProxyUrl());
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

    public String getGrokMirrorMd5(String ssoToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!grokMirrorPwd.equals("-")) {
            headers.setBearerAuth(gptMirrorPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.putArray("sso_token_list").add(ssoToken);

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(grokMirrorHost + "/api/batch-add-grok-token", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            JSONObject map = objectMapper.readValue(stringResponseEntity.getBody(), JSONObject.class);
            if (map.containsKey("email_list")) {
                JSONArray gatewayToken = map.getJSONArray("email_list");
                JSONObject email = gatewayToken.getObject(0, JSONObject.class);
                String emailMd5 = email.getString("email_md5");
                return emailMd5;
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return null;
        }
    }

    public HttpResult<String> getGrokMirrorUrl(Account account, Share user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!grokMirrorPwd.equals("-")) {
            headers.setBearerAuth(grokMirrorPwd);
        }
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", user.getUniqueName());
        //personJsonObject.put("email_md5", account.getRefreshToken());
        personJsonObject.put("sso_token", account.getAccessToken());

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(grokMirrorHost + "/api/login-v2", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            JSONObject map = objectMapper.readValue(stringResponseEntity.getBody(), JSONObject.class);
            if (map.containsKey("login_url")) {
                String loginUrl = map.getString("login_url");
                return HttpResult.success(grokMirrorHost+loginUrl);
            } else {
                return HttpResult.success();
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.success();
        }
    }
}
