package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.prism.shape.ShapeRep;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.domain.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareController {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SHARE_TOKEN_URL = "https://chat.oaifree.com/token/register";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public HttpResult<List<ShareVO>> list(HttpServletRequest request, @RequestParam(required = false) String emailAddr) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        // 根据邮箱和用户id获取账号
        List<Account> accounts = accountService.findAll().stream()
                                         .filter(e -> e.getUserId().equals(user.getId()) && (!StringUtils.hasText(emailAddr) || e.getEmail().contains(emailAddr))).collect(Collectors.toList());
        Map<Integer, Account> accountIdMap = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));
        // 筛选accountId在账号map内的
        List<Share> shareList = shareService.findAll();
        shareList = shareList.stream().filter(e -> accountIdMap.containsKey(e.getAccountId())).collect(Collectors.toList());
        List<ShareVO> shareVOS = ConvertUtil.convertList(shareList, ShareVO.class);
        for (ShareVO share : shareVOS) {
            Account account = accountIdMap.get(share.getAccountId());
            share.setEmail(account.getEmail());
            if (!StringUtils.hasText(share.getExpiresAt())) {
                share.setExpiresAt("-");
            }
        }
        return HttpResult.success(shareVOS);
    }

    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(HttpServletRequest request, @RequestParam Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Share share = shareService.findById(id);
        Integer accountId = share.getAccountId();
        Account account = accountService.findById(accountId);
        if (share != null && account.getUserId().equals(user.getId())) {
            shareService.removeById(id);
        } else {
            return HttpResult.error("您无权删除该账号");
        }

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
            ResponseEntity<String> stringResponseEntity = restTemplate.exchange(SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("detail") && map.get("detail").equals("revoke token key successfully")) {
                log.info("delete success");
                return HttpResult.success(true);
            }
            //else {
            //    return HttpResult.error("已删除");
            //}
        } catch (Exception e) {
            log.error("Check user error:", e);
            return HttpResult.error("删除用户异常");
        }
        return HttpResult.success();
    }


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request, @RequestBody Share dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        LambdaQueryWrapper<Share> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Share::getUniqueName, dto.getUniqueName());
        List<Share> shareList = shareService.list(lambdaQueryWrapper);
        if (!CollectionUtils.isEmpty(shareList)) {
            return HttpResult.error("用户名已存在");
        }
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        dto.setExpiresIn(0);
        dto.setGpt4Limit(-1);
        dto.setGpt35Limit(-1);
        dto.setShowUserinfo(false);
        dto.setTemporaryChat(false);
        dto.setRefreshEveryday(true);
        dto.setShowConversations(true);

        HttpHeaders headers = new HttpHeaders();
        //headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        //ObjectNode personJsonObject = objectMapper.createObjectNode();
        MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();

        personJsonObject.add("access_token", accountService.getById(dto.getAccountId()).getAccessToken());
        personJsonObject.add("unique_name", dto.getUniqueName());
        personJsonObject.add("expires_in", 0);
        personJsonObject.add("gpt35_limit", -1);
        personJsonObject.add("gpt4_limit", -1);
        personJsonObject.add("site_limit", "");
        personJsonObject.add("show_userinfo", false);
        personJsonObject.add("show_conversations", false);
        personJsonObject.add("reset_limit", true);
        personJsonObject.add("temporary_chat", false);
        ResponseEntity<String> stringResponseEntity = restTemplate.exchange(SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            dto.setShareToken(map.get("token_key").toString());
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("系统内部异常");
        }
        shareService.save(dto);

        return HttpResult.success(true);
    }

    @PatchMapping("/update")
    public HttpResult<Boolean> update(HttpServletRequest request, @RequestBody Share dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Share byId = shareService.getById(dto.getId());
        if (byId.getPassword().equals(dto.getPassword())) {
            dto.setPassword(null);
        } else {
            dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        dto.setExpiresAt(StringUtils.hasText(dto.getExpiresAt()) ? dto.getExpiresAt() : "-");
        dto.setComment(dto.getComment());
        shareService.saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    @GetMapping("/getAccount")
    public HttpResult<Account> getAccount(HttpServletRequest request, @RequestParam Integer accountId) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = accountService.getById(accountId);
        if (account == null) {
            return HttpResult.error("账号不存在");
        }

        return HttpResult.success(account);
    }

}
