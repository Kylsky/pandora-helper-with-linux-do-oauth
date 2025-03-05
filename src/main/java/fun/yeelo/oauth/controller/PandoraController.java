package fun.yeelo.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.config.MirrorConfig;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.share.ResetDTO;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareGptConfig;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.GptConfigService;
import fun.yeelo.oauth.service.MidjourneyService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/pandora")
public class PandoraController {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private static final Logger log = LoggerFactory.getLogger(PandoraController.class);
    @Autowired
    private RestTemplate restTemplate;

    @Value("${linux-do.oaifree.auth-api}")
    private String authUrl;

    private final static String DEFAULT_AUTH_URL = "https://new.oaifree.com";

    @Value("${linux-do.oaifree.token-api}")
    private String tokenUrl;

    @Value("${mirror.enable}")
    private Boolean mirrorEnable;

    @Value("${mirror.host}")
    private String mirrorHost;

    @Value("${mirror.password}")
    private String mirrorPwd;

    @Autowired
    private ShareService shareService;

    @Autowired
    private GptConfigService gptConfigService;

    @Autowired
    private MidjourneyService midjourneyService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MirrorConfig mirrorConfig;

    @GetMapping("/checkUser")
    public HttpResult<ShareVO> checkLinuxDoUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
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
        ShareGptConfig byShareId = gptConfigService.getByShareId(user.getId());
        // 判断是否有share token
        ShareVO res = new ShareVO();
        if (!mirrorEnable) {
            res.setIsShared(byShareId!=null && byShareId.getShareToken() != null);
            if (!res.getIsShared()) {
                return HttpResult.success(res);
            }
        }else {
            res.setIsShared(true);
        }

        BeanUtils.copyProperties(user, res);
        if (StringUtils.hasText(user.getChatGptUrl())) {
            return mirrorConfig.getMirrorUrl(user.getUniqueName(), byShareId.getAccountId(),user.getChatGptUrl(),user.getChatGptPassword());
        }
        else if (mirrorEnable) {
            return mirrorConfig.getMirrorUrl(user.getUniqueName(), byShareId.getAccountId());
        }else {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                ObjectNode personJsonObject = objectMapper.createObjectNode();
                personJsonObject.put("share_token", byShareId.getShareToken());
                ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(personJsonObject, headers), String.class);
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                if (map.containsKey("login_url")) {
                    String loginUrl = map.get("login_url").toString();
                    loginUrl = loginUrl.replace(DEFAULT_AUTH_URL, authUrl);
                    res.setAddress(loginUrl);

                    // 打印user信息，用json
                    log.info("Check user:{}", res);
                }
            } catch (IOException e) {
                log.error("Check user error:", e);
            }
            return HttpResult.success(res);
        }
    }

    @PostMapping("/reset")
    public HttpResult<String> reset(@RequestBody ResetDTO resetDTO) {
        String username = resetDTO.getUsername();
        String password = resetDTO.getOldPassword();
        String newPassword = resetDTO.getNewPassword();
        String confirmPassword = resetDTO.getConfirmPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return HttpResult.error("用户名或密码不能为空");
        }
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            return HttpResult.error("新密码为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            return HttpResult.error("两次密码不一致");
        }
        if (newPassword.length() < 8) {
            return HttpResult.error("密码长度必须超过大于等于8位，请重新输入。");
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请重试");
        }
        if (!passwordEncoder.matches(password,user.getPassword())){
            return HttpResult.error("密码错误，请重试");
        }
        Share update = new ShareVO();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        boolean res = shareService.updateById(update);
        midjourneyService.updateUser(update, null);
        return res ? HttpResult.success("重置成功") : HttpResult.error("重置失败");
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
        ShareGptConfig gptShare = gptConfigService.getByShareId(user.getId());
        if (gptShare==null || !StringUtils.hasText(gptShare.getShareToken())) {
            return HttpResult.error("当前用户未激活ChatGPT");
        }
        if (!passwordEncoder.matches(password,user.getPassword())){
            return HttpResult.error("密码错误，请重试");
        }

        if (StringUtils.hasText(user.getChatGptUrl())) {
            return mirrorConfig.getSimpleMirrorUrl(user.getUniqueName(), gptShare.getAccountId(),user.getChatGptUrl(),user.getChatGptPassword());
        }
        else if (mirrorEnable) {
            return mirrorConfig.getSimpleMirrorUrl(user.getUniqueName(), gptShare.getAccountId());
        }else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");

            ObjectNode personJsonObject = objectMapper.createObjectNode();
            personJsonObject.put("share_token", gptShare.getShareToken());
            ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(personJsonObject, headers), String.class);
            ShareVO res = new ShareVO();
            BeanUtils.copyProperties(user, res);
            try {
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                if (map.containsKey("login_url")) {
                    String loginUrl = map.get("login_url").toString();
                    loginUrl = loginUrl.replace(DEFAULT_AUTH_URL, authUrl);
                    res.setAddress(loginUrl);
                    // 打印user信息，用json
                    log.info("Check user:{}", res);
                }
            } catch (IOException e) {
                log.error("Check user error:", e);
                return HttpResult.error("系统内部异常");
            }
            return HttpResult.success(res.getAddress());
        }
    }
}
