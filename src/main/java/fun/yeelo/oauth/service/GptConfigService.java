package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.GptConfigMapper;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareClaudeConfig;
import fun.yeelo.oauth.domain.share.ShareGptConfig;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

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

        return HttpResult.success();
    }

}
