package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.GptConfigMapper;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareGptConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class GptConfigService extends ServiceImpl<GptConfigMapper, ShareGptConfig> implements IService<ShareGptConfig> {
    @Autowired
    private GptConfigMapper gptConfigMapper;
    @Autowired
    private ShareService shareService;

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

    public HttpResult<Boolean> addShare(Account account, Integer shareId, Integer expire, String expireAt) {
        // 更新过期时间
        if (expire != null) {

            ShareGptConfig byId = this.getByShareId(shareId);
            if (byId != null) {
                LocalDateTime expireDateTime;
                if (byId.getExpiresAt() != null) {
                    expireDateTime = byId.getExpiresAt();
                } else {
                    expireDateTime = LocalDateTime.now();
                }
                byId.setExpiresAt(expireDateTime.plusDays(expire));
                this.updateById(byId);
                return HttpResult.success();
            } else {
                expireAt = LocalDateTime.now().plusDays(expire).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        }
        this.baseMapper.delete(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, shareId));

        // 增加新的
        ShareGptConfig gptConfig = new ShareGptConfig();
        gptConfig.setShareId(shareId);
        gptConfig.setAccountId(account.getId());
        gptConfig.setShareToken("OAIFree Great");
        gptConfig.setExpiresIn(0);
        gptConfig.setGpt4Limit(-1);
        gptConfig.setGpt35Limit(-1);
        gptConfig.setShowUserinfo(false);
        gptConfig.setShowConversations(true);
        gptConfig.setRefreshEveryday(true);
        gptConfig.setTemporaryChat(false);
        // 根据expireAt设置过期时间，格式是精确到日 的
        if (StringUtils.hasText(expireAt)) {
            gptConfig.setExpiresAt(LocalDateTime.parse(expireAt + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
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
        //if (gptAccount != null) {
        //    try {
        //        HttpHeaders headers = new HttpHeaders();
        //        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //        MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
        //        personJsonObject.add("access_token", gptAccount.getAccessToken());
        //        personJsonObject.add("unique_name", share.getUniqueName());
        //        personJsonObject.add("expires_in", -1);
        //        personJsonObject.add("gpt35_limit", -1);
        //        personJsonObject.add("gpt4_limit", -1);
        //        personJsonObject.add("site_limit", "");
        //        personJsonObject.add("show_userinfo", false);
        //        personJsonObject.add("show_conversations", false);
        //        personJsonObject.add("reset_limit", true);
        //        personJsonObject.add("temporary_chat", false);
        //        ResponseEntity<String> stringResponseEntity = restTemplate.exchange(CommonConst.SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
        //        Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
        //        if (map.containsKey("detail") && map.get("detail").equals("revoke token key successfully")) {
        //            log.info("delete success");
        //            return HttpResult.success(true);
        //        }
        //    } catch (Exception e) {
        //        log.error("Check user error:", e);
        //        return HttpResult.error("删除用户异常");
        //    }
        //}

        return HttpResult.success();
    }

}
