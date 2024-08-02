package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ClaudeConfigMapper;
import fun.yeelo.oauth.dao.GptConfigMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.domain.ShareClaudeConfig;
import fun.yeelo.oauth.domain.ShareGptConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeConfigService extends ServiceImpl<ClaudeConfigMapper, ShareClaudeConfig> implements IService<ShareClaudeConfig> {
    @Autowired
    private ClaudeConfigMapper claudeConfigMapper;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ShareService shareService;
    private final ObjectMapper objectMapper = new ObjectMapper();



    public List<ShareClaudeConfig> findAll() {
        return claudeConfigMapper.selectList(null);
    }

    public ShareClaudeConfig findById(Integer id) {
        return claudeConfigMapper.selectById(id);
    }

    public ShareClaudeConfig getByShareId(Integer shareId) {
        List<ShareClaudeConfig> configs = claudeConfigMapper.selectList(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, shareId));
        if (CollectionUtils.isEmpty(configs)) {
            return null;
        }
        return configs.get(0);
    }


    public HttpResult<Boolean> addShare(Account account, int shareId) {
        // 删除原有的
        this.baseMapper.delete(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId,shareId));
        Share byId = shareService.getById(shareId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "curl/7.64.1");  // 模拟 curl 的 User-Agent

        ObjectNode personJsonObject = objectMapper.createObjectNode();

        personJsonObject.put("session_key", account.getAccessToken());
        personJsonObject.put("unique_name", byId.getUniqueName());
        personJsonObject.put("expires_in", 3600 * 24 * 30);

        HttpEntity<ObjectNode> requestEntity = new HttpEntity<>(personJsonObject, headers);
        try {
            ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity("https://fuclaude.yeelo.fun/manage-api/auth/oauth_token", requestEntity, String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            String oauthToken = map.get("oauth_token").toString();

            ShareClaudeConfig shareClaudeConfig = new ShareClaudeConfig();
            shareClaudeConfig.setShareId(shareId);
            shareClaudeConfig.setOauthToken(oauthToken);
            shareClaudeConfig.setAccountId(account.getId());
            save(shareClaudeConfig);
            return HttpResult.success();
        }catch (Exception ex) {
            log.error("获取oauth_token异常");
            return HttpResult.error("获取oauth_token异常");
        }
    }
}
