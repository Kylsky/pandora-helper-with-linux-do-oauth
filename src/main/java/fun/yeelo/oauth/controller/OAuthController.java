package fun.yeelo.oauth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.util.JSONPObject;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.domain.ShareVO;
import fun.yeelo.oauth.service.ShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/oauth2")
public class OAuthController {
    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);
    @Value("${linux-do.oauth2.client.registration.client-id}")
    private String clientId;

    @Value("${linux-do.oauth2.client.registration.client-secret}")
    private String clientSecret;

    @Value("${linux-do.oauth2.client.registration.redirect-uri}")
    private String redirectUri;

    @Value("${linux-do.oauth2.client.provider.authorization-uri}")
    private String authorizationEndpoint;

    @Value("${linux-do.oauth2.client.provider.token-uri}")
    private String tokenEndpoint;

    @Value("${linux-do.oauth2.client.provider.user-info-uri}")
    private String userEndpoint;

    @Autowired
    private ShareService shareService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/initiate")
    public String initiateAuth(HttpServletRequest request,
                               @RequestParam(required = false) String type) {
        HttpSession session = request.getSession();
        String state = new BigInteger(130, new SecureRandom()).toString(32) + "-" + type;
        session.setAttribute("oauth2State", state);
        String redirectUrl = String.format("%s?client_id=%s&response_type=code&redirect_uri=%s&scope=%s&state=%s",
                authorizationEndpoint, clientId, redirectUri, "read,write", state);
        return redirectUrl;
        //response.sendRedirect(redirectUrl);
    }

    @GetMapping("/callback")
    public HttpEntity<String> handleAuthorizationCode(@RequestParam("code") String code,
                                                      @RequestParam("state") String state,
                                                      HttpServletRequest request) {
        HttpSession session = request.getSession();
        String sessionState = (String) session.getAttribute("oauth2State");
        if (!StringUtils.hasText(state) || !sessionState.equals(state)) {
            return new ResponseEntity<>("Invalid state", HttpStatus.UNAUTHORIZED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.add("Authorization", "Basic " + encodedAuth);
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", code);
        requestBody.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, requestEntity, Map.class);

        Map responseBody = response.getBody();

        if (responseBody != null && responseBody.containsKey("access_token")) {
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.add("User-Agent", "curl/7.64.1");
            userHeaders.add("Host", "connect.linux.do");
            userHeaders.setBearerAuth(responseBody.get("access_token").toString());
            HttpEntity<String> entity = new HttpEntity<>(userHeaders);
            ResponseEntity<Map> userResponse = restTemplate.exchange(userEndpoint, HttpMethod.GET, entity, Map.class);

            Map userResBody = userResponse.getBody();

            if (userResBody != null) {
                String jmc = new BigInteger(130, new SecureRandom()).toString(32);
                session.setAttribute("jmc", jmc);
                userResBody.put("jmc", jmc);
                if (state.contains("Claude")) {
                    userResBody.put("type", "Claude");
                } else if (state.contains("panel")){
                    userResBody.put("type", "panel");
                }else {
                    userResBody.put("type", "ChatGPT");
                }
                String jsonString = JSON.toJSONString(userResBody, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                        SerializerFeature.WriteDateUseDateFormat);
                log.info("user info:{}", jsonString);
                ShareVO share = JSON.parseObject(jsonString, ShareVO.class);
                Share user = shareService.getByUserName(share.getUsername());
                if (Objects.isNull(user)) {
                    log.info("添加新用户,{}", share.getUsername());
                    ShareVO userToAdd = new ShareVO();
                    userToAdd.setUniqueName(share.getUsername());
                    userToAdd.setAvatarUrl(share.getAvatarUrl());
                    userToAdd.setIsShared(false);
                    userToAdd.setTrustLevel(share.getTrustLevel());
                    userToAdd.setPassword(passwordEncoder.encode("123456"));
                    userToAdd.setComment("");
                    shareService.save(share);
                }else {
                    Integer trustLevel = share.getTrustLevel();
                    String avatarUrl = share.getAvatarUrl();
                    if (!trustLevel.equals(user.getTrustLevel()) || !avatarUrl.equals(user.getAvatarUrl())) {
                        log.info("更新新用户{}，等级:{},头像:{}", share.getUsername(),share.getTrustLevel(),share.getAvatarUrl());
                        Share toUpdate = new Share();
                        toUpdate.setId(user.getId());
                        toUpdate.setAvatarUrl(avatarUrl);
                        toUpdate.setTrustLevel(trustLevel);
                        shareService.updateById(toUpdate);
                    }
                }

                return new ResponseEntity<>(jsonString, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Failed to obtain user info", HttpStatus.UNAUTHORIZED);
            }
        } else {
            return new ResponseEntity<>("Failed to obtain access token", HttpStatus.UNAUTHORIZED);
        }


    }

}
