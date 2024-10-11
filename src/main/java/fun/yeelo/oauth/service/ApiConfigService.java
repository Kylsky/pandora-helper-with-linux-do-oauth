package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ApiConfigMapper;
import fun.yeelo.oauth.dao.ClaudeConfigMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.domain.ShareApiConfig;
import fun.yeelo.oauth.domain.ShareClaudeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ApiConfigService extends ServiceImpl<ApiConfigMapper, ShareApiConfig> implements IService<ShareApiConfig> {

    public HttpResult<Boolean> addShare(Account account, int shareId, Integer expire) {
        // 删除原有的
        this.baseMapper.delete(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId, shareId));

        ShareApiConfig shareClaudeConfig = new ShareApiConfig();
        shareClaudeConfig.setShareId(shareId);
        shareClaudeConfig.setAccountId(account.getId());
        save(shareClaudeConfig);
        return HttpResult.success();
    }
}
