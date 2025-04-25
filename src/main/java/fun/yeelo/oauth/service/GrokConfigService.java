package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.config.MirrorConfig;
import fun.yeelo.oauth.dao.GrokConfigMapper;
import fun.yeelo.oauth.domain.LoginDTO;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareGrokConfig;
import fun.yeelo.oauth.domain.share.ShareVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class GrokConfigService extends ServiceImpl<GrokConfigMapper, ShareGrokConfig> implements IService<ShareGrokConfig> {
    @Autowired
    private GrokConfigMapper grokConfigMapper;
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
    @Autowired
    private MirrorConfig mirrorConfig;

    public ShareGrokConfig getByShareId(Integer shareId) {
        List<ShareGrokConfig> configs = grokConfigMapper.selectList(new LambdaQueryWrapper<ShareGrokConfig>().eq(ShareGrokConfig::getShareId, shareId));
        if (CollectionUtils.isEmpty(configs)) {
            return null;
        }
        return configs.get(0);
    }


    public HttpResult<Boolean> addShare(Account account, int shareId, Integer duration, String expireAt) {
        if (duration != null) {

            ShareGrokConfig byId = this.getByShareId(shareId);
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
        this.baseMapper.delete(new LambdaQueryWrapper<ShareGrokConfig>().eq(ShareGrokConfig::getShareId, shareId));

        ShareGrokConfig ShareGrokConfig = new ShareGrokConfig();
        ShareGrokConfig.setShareId(shareId);
        ShareGrokConfig.setAccountId(account.getId());
        // 根据expireAt设置过期时间，格式是精确到日 的
        if (StringUtils.hasText(expireAt)) {
            ShareGrokConfig.setExpiresAt(LocalDateTime.parse(expireAt + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        save(ShareGrokConfig);

        return HttpResult.success();
    }

    public String generateAutoToken(Account account, Share byId, Integer expire) {
        return mirrorConfig.getGrokMirrorUrl(account, byId).getData();
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
        ShareGrokConfig grokShare = getByShareId(user.getId());
        if (grokShare == null) {
            return HttpResult.error("权限未激活,请联系管理员");
        }
        Account account = accountService.getById(grokShare.getAccountId());
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
        ShareGrokConfig grokShare = getByShareId(user.getId());
        if (grokShare == null) {
            return HttpResult.error("当前用户未激活Grok");
        }
        Account account = accountService.getById(grokShare.getAccountId());
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
