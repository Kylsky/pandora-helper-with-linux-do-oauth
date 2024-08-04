package fun.yeelo.oauth.timer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.ClaudeConfigService;
import fun.yeelo.oauth.service.GptConfigService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private GptConfigService gptConfigService;

    @Autowired
    private ClaudeConfigService claudeConfigService;

    private static final String REFRESH_URL = "https://token.oaifree.com/api/auth/refresh";


    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(cron = "0 0 1 * * ?")
    public void updateExpire() {
        List<Share> shares = shareService.list().stream().filter(e -> e.getExpiresAt() != null).collect(Collectors.toList());
        shares.forEach(share -> {
            try {
                LocalDate expireData = LocalDate.parse(share.getExpiresAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                // 大于等于过期时间的，删除share config
                if (expireData.isEqual(LocalDate.now()) || !expireData.isAfter(LocalDate.now())) {
                    gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, share.getId()));
                    claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, share.getId()));
                }
            }catch (Exception ex) {
                log.error("expire detect error,unique_name:{}", share.getUniqueName());
            }
        });
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void updateRefreshToken() {
        log.info("开始刷新access_token");
        List<Account> accounts = accountService.list().stream()
                                         .filter(e -> StringUtils.hasText(e.getRefreshToken())&& e.getAccountType().equals(1))
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

        Map<Integer, ShareGptConfig> gptConfigMap = gptConfigService.list().stream().collect(Collectors.toMap(ShareGptConfig::getShareId, Function.identity()));

        for (Share share : shares) {
            ShareGptConfig gptConfig = gptConfigMap.get(share.getId());
            if (gptConfig == null) {
                continue;
            }
            try {
                Share update = new Share();
                update.setId(share.getId());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("access_token", accountIdMap.get(gptConfig.getAccountId()).getAccessToken());
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
                log.error("update share token error,unique_name:{}",share.getUniqueName(), e);
            }
        }
        log.info("刷新share_token结束");
    }
}
