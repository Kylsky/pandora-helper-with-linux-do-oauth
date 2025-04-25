package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.LoginDTO;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareApiConfig;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.ApiConfigService;
import fun.yeelo.oauth.service.MidjourneyService;
import fun.yeelo.oauth.service.ShareService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private ShareService shareService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MidjourneyService midjourneyService;
    @Autowired
    private ApiConfigService apiConfigService;
    @Autowired
    private AccountService accountService;
    @Value("${chat_site}")
    private String chatSite;


    @GetMapping("/checkUser")
    public HttpResult<ShareVO> checkApiUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
        String jmcFromSession = request.getSession().getAttribute("jmc") == null ? "" : request.getSession().getAttribute("jmc").toString();
        if (!StringUtils.hasText(jmc) || !jmc.equals(jmcFromSession)) {
            return HttpResult.error("登录校验码失败，请重试");
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

            return HttpResult.success(share);
        }
        // 获取share的gpt配置
        ShareApiConfig byShareId = apiConfigService.getByShareId(user.getId());
        // 判断是否有share token
        ShareVO res = new ShareVO();
        BeanUtils.copyProperties(user, res);

        if (byShareId == null) {
            res.setIsShared(false);
            return HttpResult.success(res);
        }
        res.setIsShared(true);
        Account account = accountService.getById(byShareId.getAccountId());
        if (account == null) {
            return HttpResult.error("账号不存在");
        }
        String token = chatSite + "/#/?settings={%22key%22:%22" + account.getRefreshToken() + "%22,%22url%22:%22" + account.getAccessToken() + "%22}";
        res.setChatGptUrl(token);
        return HttpResult.success(res);
    }

    @PostMapping("/login")
    public HttpResult<String> login(@RequestBody LoginDTO resetDTO) {
        String username = resetDTO.getUsername();
        String password = resetDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return HttpResult.error("用户名或密码不能为空");
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请重试");
        }
        ShareApiConfig apiShare = apiConfigService.getByShareId(user.getId());
        if (apiShare == null) {
            return HttpResult.error("当前用户未激活API服务");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return HttpResult.error("密码错误，请重试");
        }
        Account account = accountService.getById(apiShare.getAccountId());
        if (account == null) {
            return HttpResult.error("账号不存在");
        }
        String token = chatSite + "/#/?settings={%22key%22:%22" + account.getRefreshToken() + "%22,%22url%22:%22" + account.getAccessToken() + "%22}";
        return HttpResult.success(token);

    }
}
