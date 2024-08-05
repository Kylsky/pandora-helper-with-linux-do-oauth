package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.GptConfigService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownServiceException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
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

    @Autowired
    private ShareService shareService;

    @Autowired
    private GptConfigService gptConfigService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/checkUser")
    public HttpEntity<ShareVO> checkLinuxDoUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
        String jmcFromSession = request.getSession().getAttribute("jmc") == null ? "" : request.getSession().getAttribute("jmc").toString();
        if (!StringUtils.hasText(jmc) || !jmc.equals(jmcFromSession)) {
            return new ResponseEntity<>(new ShareVO(), HttpStatus.UNAUTHORIZED);
        }
        Share user = shareService.getByUserName(username);
        if (Objects.isNull(user)) {
            // 新建默认share
            ShareVO share = new ShareVO();
            share.setUniqueName(username);
            share.setIsShared(false);
            share.setPassword(passwordEncoder.encode("123456"));
            share.setComment("unassigned");
            shareService.save(share);
            return new ResponseEntity<>(share, HttpStatus.OK);
        }
        // 获取share的gpt配置
        ShareGptConfig byShareId = gptConfigService.getByShareId(user.getId());
        // 判断是否有share token
        ShareVO res = new ShareVO();
        res.setIsShared(byShareId!=null && byShareId.getShareToken() != null);
        if (!res.getIsShared()) {
            return new ResponseEntity<>(res, HttpStatus.OK);
        }
        BeanUtils.copyProperties(user, res);

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
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @PostMapping("/reset")
    public HttpEntity<String> reset(@RequestBody ResetDTO resetDTO) {
        String username = resetDTO.getUsername();
        String password = resetDTO.getOldPassword();
        String newPassword = resetDTO.getNewPassword();
        String confirmPassword = resetDTO.getConfirmPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return new ResponseEntity<>("用户名或密码不能为空", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            return new ResponseEntity<>("新密码为空", HttpStatus.BAD_REQUEST);
        }
        if (!newPassword.equals(confirmPassword)) {
            return new ResponseEntity<>("两次密码不一致", HttpStatus.BAD_REQUEST);
        }
        if (newPassword.length() < 8) {
            return new ResponseEntity<>("密码长度必须超过大于等于8位，请重新输入。", HttpStatus.BAD_REQUEST);
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return new ResponseEntity<>("用户不存在，请重试", HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(password,user.getPassword())){
            return new ResponseEntity<>("密码错误，请重试", HttpStatus.BAD_REQUEST);
        }
        Share update = new ShareVO();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        boolean res = shareService.updateById(update);
        return res ? new ResponseEntity<>("重置成功", HttpStatus.OK) : new ResponseEntity<>("重置失败", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/login")
    public HttpEntity<String> login(@RequestBody LoginDTO resetDTO) {
        String username = resetDTO.getUsername();
        String password = resetDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return new ResponseEntity<>("用户名或密码不能为空", HttpStatus.BAD_REQUEST);
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return new ResponseEntity<>("用户不存在，请重试", HttpStatus.BAD_REQUEST);
        }
        ShareGptConfig gptShare = gptConfigService.getByShareId(user.getId());
        if (gptShare==null || !StringUtils.hasText(gptShare.getShareToken())) {
            return new ResponseEntity<>("用户未激活", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(password,user.getPassword())){
            return new ResponseEntity<>("密码错误，请重试", HttpStatus.UNAUTHORIZED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

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
            return new ResponseEntity<>("系统内部异常", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(res.getAddress(), HttpStatus.OK);

    }
}
