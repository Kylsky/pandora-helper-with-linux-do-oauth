package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.GptConfigMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GptConfigService extends ServiceImpl<GptConfigMapper, ShareGptConfig> implements IService<ShareGptConfig> {
    @Autowired
    private GptConfigMapper gptConfigMapper;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccountService accountService;


    public List<ShareGptConfig> findAll() {
        return gptConfigMapper.selectList(null);
    }

    public ShareGptConfig findById(Integer id) {
        return gptConfigMapper.selectById(id);
    }

    public ShareGptConfig getByShareId(Integer shareId) {
        List<ShareGptConfig> configs = gptConfigMapper.selectList(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, shareId));
        if (CollectionUtils.isEmpty(configs)) {
            return null;
        }
        return configs.get(0);
    }

    public HttpResult<Boolean> addShare(Account account, String uniqueName, Integer shareId, Integer expire) {
        long duration = 0L;
        //if (StringUtils.hasText(expire) && !expire.equals("-")) {
        //    expire += " 00:00:00";
        //    LocalDateTime expireDay = LocalDateTime.parse(expire, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //    duration = Duration.between(LocalDateTime.now(), expireDay).getSeconds();
        //}
        String shareToken;
        if (expire != null) {
            Share share = new Share();
            share.setId(shareId);
            Share byId = shareService.getById(shareId);
            LocalDateTime expireDateTime;
            if (byId != null && StringUtils.hasText(byId.getExpiresAt()) && !byId.getExpiresAt().equals("-")) {
                String expiresAt = byId.getExpiresAt();
                expireDateTime = LocalDateTime.parse(expiresAt+" 00:00:00",DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }else {
                expireDateTime = LocalDateTime.now();
            }
            share.setExpiresAt(expireDateTime.plusDays(expire).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            shareService.updateById(share);
        }
        // 删除旧的share token
        try {
            List<ShareGptConfig> list = this.list(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, shareId));
            if (!CollectionUtils.isEmpty(list)) {
                Integer accountId = list.get(0).getAccountId();
                Account formerAccount = accountService.getById(accountId);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("access_token", formerAccount.getAccessToken());
                personJsonObject.add("unique_name", uniqueName);
                personJsonObject.add("expires_in", -1);
                personJsonObject.add("gpt35_limit", -1);
                personJsonObject.add("gpt4_limit", -1);
                personJsonObject.add("site_limit", "");
                personJsonObject.add("show_userinfo", false);
                personJsonObject.add("show_conversations", false);
                personJsonObject.add("reset_limit", true);
                personJsonObject.add("temporary_chat", false);
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(CommonConst.SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                if (map.containsKey("detail") && map.get("detail").equals("revoke token key successfully")) {
                    log.info("delete success");
                }
            }
        } catch (Exception e) {
            log.error("删除旧的账号失败", e);
            return HttpResult.error("删除旧账号失败");
        }

        // 获取新的share token
        try {
            log.info("开始新增share");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
            personJsonObject.add("access_token", account.getAccessToken());
            personJsonObject.add("unique_name", uniqueName);
            personJsonObject.add("expires_in", duration);
            personJsonObject.add("gpt35_limit", -1);
            personJsonObject.add("gpt4_limit", -1);
            personJsonObject.add("site_limit", "");
            personJsonObject.add("show_userinfo", false);
            personJsonObject.add("show_conversations", false);
            personJsonObject.add("reset_limit", true);
            personJsonObject.add("temporary_chat", false);
            ResponseEntity<String> stringResponseEntity = restTemplate.exchange(CommonConst.SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            shareToken = map.get("token_key").toString();
            log.info("新增share完成,share_token:{}", shareToken);
        } catch (Exception e) {
            log.error("新增 chatgpt share 异常:", e);
            return HttpResult.error("调取share token异常");
        }
        // 删除旧的
        this.baseMapper.delete(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, shareId));

        // 增加新的
        ShareGptConfig gptConfig = new ShareGptConfig();
        gptConfig.setShareId(shareId);
        gptConfig.setAccountId(account.getId());
        gptConfig.setShareToken(shareToken);
        gptConfig.setExpiresIn(0);
        gptConfig.setGpt4Limit(-1);
        gptConfig.setGpt35Limit(-1);
        gptConfig.setShowUserinfo(false);
        gptConfig.setShowConversations(true);
        gptConfig.setRefreshEveryday(true);
        gptConfig.setTemporaryChat(false);
        int insert = this.baseMapper.insert(gptConfig);
        log.info("添加gpt配置结果:{}", insert);
        return HttpResult.success();
    }

    public HttpResult<Boolean> deleteShare(Integer shareId) {
        Share share = shareService.findById(shareId);
        Account gptAccount = null;
        ShareGptConfig one = this.getOne(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, shareId));
        if (one != null) {
            gptAccount = accountService.getById(one.getAccountId());
        }
        this.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, shareId));

        // 删除oaifree的share token
        if (gptAccount != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("access_token", gptAccount.getAccessToken());
                personJsonObject.add("unique_name", share.getUniqueName());
                personJsonObject.add("expires_in", -1);
                personJsonObject.add("gpt35_limit", -1);
                personJsonObject.add("gpt4_limit", -1);
                personJsonObject.add("site_limit", "");
                personJsonObject.add("show_userinfo", false);
                personJsonObject.add("show_conversations", false);
                personJsonObject.add("reset_limit", true);
                personJsonObject.add("temporary_chat", false);
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(CommonConst.SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                if (map.containsKey("detail") && map.get("detail").equals("revoke token key successfully")) {
                    log.info("delete success");
                    return HttpResult.success(true);
                }
            } catch (Exception e) {
                log.error("Check user error:", e);
                return HttpResult.error("删除用户异常");
            }
        }

        return HttpResult.success();
    }

}
