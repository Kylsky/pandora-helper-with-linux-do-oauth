package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ShareService extends ServiceImpl<ShareMapper, Share> implements IService<Share> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private ShareMapper shareMapper;
    @Autowired
    private AccountService accountService;
    @Autowired
    private GptConfigService gptConfigService;
    @Autowired
    private ClaudeConfigService claudeConfigService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ApiConfigService apiConfigService;


    public List<Share> findAll() {
        return shareMapper.selectList(null);
    }

    public Share findById(Integer id) {
        return shareMapper.selectById(id);
    }

    public Share getByUserName(String username) {
        List<Share> shares = shareMapper.selectList(new LambdaQueryWrapper<Share>().eq(Share::getUniqueName, username));
        if (CollectionUtils.isEmpty(shares)) {
            return null;
        }
        return shares.get(0);
    }

    public HttpResult deleteShare(Integer id, Share user){
        Share share = findById(id);
        Integer accountId = share.getAccountId();
        Account account = accountService.findById(accountId);
        if (share != null && account.getUserId().equals(user.getId())) {
            removeById(id);
            gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId,id));
            claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId,id));
        } else {
            return HttpResult.error("您无权删除该账号");
        }

        // 删除oaifree的share token
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
            personJsonObject.add("access_token", account.getAccessToken());
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
        return HttpResult.success();
    }

    public HttpResult<Boolean> distribute(ShareVO share) {
        Account account = accountService.getById(share.getAccountId());
        Share byId = this.getById(share.getId());
        if (share.getAccountId()!=null && share.getAccountId().equals(-1)) {
            gptConfigService.deleteShare(share.getId());
            return HttpResult.success();
        }else if (share.getAccountId()!=null && share.getAccountId().equals(-2)) {
            claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId,share.getId()));
            return HttpResult.success();
        }
        else if (share.getAccountId()!=null && share.getAccountId().equals(-3)) {
            apiConfigService.remove(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId,share.getId()));
            return HttpResult.success();
        }
        else if (account == null) {
            return HttpResult.error("账号不存在");
        }

        switch (account.getAccountType()) {
            case 1:
                return gptConfigService.addShare(account, byId.getUniqueName(), byId.getId(), share.getDuration());
            case 2:
                return claudeConfigService.addShare(account, byId.getId(), null);
            case 3:
                return apiConfigService.addShare(account, byId.getId(), null);
            default:
                return HttpResult.error("激活出现异常");
        }
    }
}
