package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ApiConfigMapper;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.ShareApiConfig;
import fun.yeelo.oauth.domain.share.ShareClaudeConfig;
import fun.yeelo.oauth.domain.share.ShareGptConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ApiConfigService extends ServiceImpl<ApiConfigMapper, ShareApiConfig> implements IService<ShareApiConfig> {

    private final ApiConfigMapper apiConfigMapper;

    public ApiConfigService(@Qualifier("apiConfigMapper") ApiConfigMapper apiConfigMapper) {
        this.apiConfigMapper = apiConfigMapper;
    }

    public HttpResult<Boolean> addShare(Account account, int shareId, Integer duration, String expireAt) {
        if (duration != null) {

            ShareApiConfig byId = this.getByShareId(shareId);
            if (byId != null) {
                LocalDateTime expireDateTime;
                if (byId.getExpiresAt()!=null) {
                    expireDateTime = byId.getExpiresAt();
                }else {
                    expireDateTime = LocalDateTime.now();
                }
                byId.setExpiresAt(expireDateTime.plusDays(duration));
                this.updateById(byId);
                return HttpResult.success();
            }else {
                expireAt = LocalDateTime.now().plusDays(duration).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        }
        // 删除原有的
        this.baseMapper.delete(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId, shareId));

        ShareApiConfig shareApiConfig = new ShareApiConfig();
        shareApiConfig.setShareId(shareId);
        shareApiConfig.setAccountId(account.getId());
        // 根据expireAt设置过期时间，格式是精确到日 的
        if (StringUtils.hasText(expireAt)) {
            shareApiConfig.setExpiresAt(LocalDateTime.parse(expireAt + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        save(shareApiConfig);
        return HttpResult.success();
    }

    public ShareApiConfig getByShareId(Integer shareId) {
        List<ShareApiConfig> configs = apiConfigMapper.selectList(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId, shareId));
        if (CollectionUtils.isEmpty(configs)) {
            return null;
        }
        return configs.get(0);
    }
}
