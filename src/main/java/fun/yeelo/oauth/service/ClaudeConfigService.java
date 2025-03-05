package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ClaudeConfigMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareClaudeConfig;
import fun.yeelo.oauth.domain.share.ShareVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClaudeConfigService extends ServiceImpl<ClaudeConfigMapper, ShareClaudeConfig> implements IService<ShareClaudeConfig> {
    @Autowired
    private ClaudeConfigMapper claudeConfigMapper;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ShareService shareService;

    @Autowired
    private MidjourneyService midjourneyService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${fuclaudeProxy}")
    private String fuclaudeUrl;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountService accountService;


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


    public HttpResult<Boolean> addShare(Account account, int shareId, Integer duration, String expireAt) {
        if (duration != null) {

            ShareClaudeConfig byId = this.getByShareId(shareId);
            if (byId != null) {
                LocalDateTime expireDateTime;
                if (byId.getExpiresAt() != null) {
                    expireDateTime = byId.getExpiresAt();
                } else {
                    expireDateTime = LocalDateTime.now();
                }
                byId.setExpiresAt(expireDateTime.plusDays(duration));
                this.updateById(byId);
                return HttpResult.success();
            } else {
                expireAt = LocalDateTime.now().plusDays(duration).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        }
        // 删除原有的
        this.baseMapper.delete(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, shareId));

        ShareClaudeConfig shareClaudeConfig = new ShareClaudeConfig();
        shareClaudeConfig.setShareId(shareId);
        shareClaudeConfig.setAccountId(account.getId());
        // 根据expireAt设置过期时间，格式是精确到日 的
        if (StringUtils.hasText(expireAt)) {
            shareClaudeConfig.setExpiresAt(LocalDateTime.parse(expireAt + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        save(shareClaudeConfig);

        return HttpResult.success();
    }

    public String generateAutoToken(Account account, Share byId, Integer expire) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "curl/7.64.1");  // 模拟 curl 的 User-Agent

        ObjectNode personJsonObject = objectMapper.createObjectNode();

        long duration = 0L;
        String expiresAt = byId.getExpiresAt();
        if (StringUtils.hasText(expiresAt) && !expiresAt.equals("-")) {
            expiresAt += " 00:00:00";
            LocalDateTime expireDay = LocalDateTime.parse(expiresAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            duration = Duration.between(LocalDateTime.now(), expireDay).getSeconds();
        }

        personJsonObject.put("session_key", account.getAccessToken());
        if (Boolean.TRUE.equals(account.getConversationIsolated())) {
            personJsonObject.put("unique_name", byId.getUniqueName());
        }
        if (duration <= 0L) {
            personJsonObject.put("expires_in", 3600 * 24 * 7);
        }
        if (expire != null) {
            personJsonObject.put("expires_in", expire);
        }

        HttpEntity<ObjectNode> requestEntity = new HttpEntity<>(personJsonObject, headers);
        try {
            ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(fuclaudeUrl + "/manage-api/auth/oauth_token", requestEntity, String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            String oauthToken = map.get("oauth_token").toString();

            return fuclaudeUrl + "/login_oauth?token=" + oauthToken;
        } catch (Exception ex) {
            log.error("获取oauth_token异常", ex);
            return null;
        }
    }

    public HttpResult<String> checkLinuxDoUser(String username, String jmc, HttpServletRequest request) {
        String jmcFromSession = request.getSession().getAttribute("jmc") == null ? "" : request.getSession().getAttribute("jmc").toString();
        if (!StringUtils.hasText(jmc) || !jmc.equals(jmcFromSession)) {
            return HttpResult.error("请遵守登录规范！");
        }
        Share user = shareService.getByUserName(username);
        if (Objects.isNull(user)) {
            // 新建默认share
            ShareVO share = new ShareVO();
            share.setUniqueName(username);
            share.setIsShared(false);
            share.setPassword(passwordEncoder.encode("123456"));
            share.setComment("");
            shareService.save(share);
            midjourneyService.addUser(share, "DISABLED");

            return HttpResult.error("用户未激活,请联系管理员");
        }
        ShareClaudeConfig claudeShare = getByShareId(user.getId());
        if (claudeShare == null) {
            return HttpResult.error("权限未激活,请联系管理员");
        }
        Account account = accountService.getById(claudeShare.getAccountId());
        String token = generateAutoToken(account, user, null);
        return HttpResult.success(token);
    }

    public HttpResult<String> login(LoginDTO resetDTO) {
        String username = resetDTO.getUsername();
        String password = resetDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return HttpResult.error("用户名或密码不能为空");
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请重试");
        }
        ShareClaudeConfig claudeShare = getByShareId(user.getId());
        if (claudeShare == null) {
            return HttpResult.error("当前用户未激活Claude");
        }
        Account account = accountService.getById(claudeShare.getAccountId());
        String token = generateAutoToken(account, user, null);
        if (token == null) {
            return HttpResult.error("生成OAUTH_TOKEN异常，请联系管理员");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return HttpResult.error("密码错误，请重试");
        }
        return HttpResult.success(token);
    }
}
