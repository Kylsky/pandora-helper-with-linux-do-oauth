package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.GptConfigMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.ShareGptConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GptConfigService extends ServiceImpl<GptConfigMapper, ShareGptConfig> implements IService<ShareGptConfig> {
    @Autowired
    private GptConfigMapper gptConfigMapper;
    @Autowired
    private RestTemplate restTemplate;
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

    public HttpResult<Boolean> addShare(Account account, String uniqueName, Integer shareId) {
        String shareToken;
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
            personJsonObject.add("access_token", account.getAccessToken());
            personJsonObject.add("unique_name", uniqueName);
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
            shareToken = map.get("token_key").toString();
        } catch (IOException e) {
            log.error("新增 chatgpt share 异常:", e);
            return HttpResult.error("新增 chatgpt share 异常");
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
        this.save(gptConfig);
        return HttpResult.success();
    }


}
