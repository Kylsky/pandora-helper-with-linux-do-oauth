package fun.yeelo.oauth.timer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.domain.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UpdateTimer {
    @Autowired
    private AccountService accountService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private RestTemplate restTemplate;

    private static final String REFRESH_URL = "https://token.oaifree.com/api/auth/refresh";


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(cron = "0 0 2 * * ?")
    public void updateRefreshToken() {
        log.info("开始刷新access_token");
        List<Account> accounts = accountService.list().stream()
                                         .filter(e -> StringUtils.hasText(e.getRefreshToken()))
                                         .collect(Collectors.toList());
        accounts.forEach(account -> {
            try {
                Integer accountId = account.getId();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("refresh_token", account.getRefreshToken());
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(REFRESH_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                if (map.containsKey("access_token")) {
                    log.info("refresh success");
                    String newToken = map.get("access_token").toString();
                    Account updateDTO = new Account();
                    updateDTO.setId(accountId);
                    updateDTO.setAccessToken(newToken);
                    accountService.saveOrUpdate(updateDTO);
                }
            } catch (Exception e) {
                log.error("刷新access_token异常,异常账号:{}",account.getEmail(), e);
            }
        });
        log.info("刷新access_token结束");
    }


    @Scheduled(cron = "0 0 3 */2 * ?")
    public void updateShareToken() {
        log.info("开始刷新share_token");
        List<Share> shares = shareService.list().stream()
                                     .filter(e -> StringUtils.hasText(e.getShareToken()))
                                     .collect(Collectors.toList());

        Map<Integer, Account> accountIdMap = accountService.list()
                                                     .stream()
                                                     .collect(Collectors.toMap(Account::getId, Function.identity()));

        shares.forEach(share -> {
            try {

                Share update = new Share();
                update.setId(share.getId());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("access_token", accountIdMap.get(share.getAccountId()).getAccessToken());
                personJsonObject.add("unique_name", share.getUniqueName());
                personJsonObject.add("expires_in", 0);
                personJsonObject.add("gpt35_limit", -1);
                personJsonObject.add("gpt4_limit", -1);
                personJsonObject.add("site_limit", "");
                personJsonObject.add("show_userinfo", false);
                personJsonObject.add("show_conversations", false);
                personJsonObject.add("reset_limit", true);
                personJsonObject.add("temporary_chat", false);
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(CommonConst.SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                update.setShareToken(map.get("token_key").toString());
                shareService.updateById(update);
            } catch (IOException e) {
                log.error("Check user error:", e);
            }
        });
        log.info("刷新share_token结束");
    }
}
