package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.ClaudeConfigService;
import fun.yeelo.oauth.service.GptConfigService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareController {
    private final ObjectMapper objectMapper = new ObjectMapper();
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
    @Autowired
    private GptConfigService gptConfigService;
    @Autowired
    private ClaudeConfigService claudeConfigService;

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
        shareList = shareList.stream().filter(e -> e.getParentId().equals(user.getId())).collect(Collectors.toList());
        List<ShareVO> shareVOS = ConvertUtil.convertList(shareList, ShareVO.class);

        // 获取gpt config
        Map<Integer, ShareGptConfig> gptMap = gptConfigService.list().stream().collect(Collectors.toMap(ShareGptConfig::getShareId, Function.identity()));

        // 获取claude config
        Map<Integer, ShareClaudeConfig> claudeMap = claudeConfigService.list().stream().collect(Collectors.toMap(ShareClaudeConfig::getShareId, Function.identity()));

        // 设置邮箱
        for (ShareVO share : shareVOS) {
            ShareGptConfig gptConfig = gptMap.get(share.getId());
            if (gptConfig!=null){
                share.setGptEmail(accountIdMap.get(gptConfig.getAccountId()).getEmail());
            }else {
                share.setGptEmail("-");
            }

            ShareClaudeConfig claudeConfig = claudeMap.get(share.getId());
            if (claudeConfig!=null){
                share.setClaudeEmail(accountIdMap.get(claudeConfig.getAccountId()).getEmail());
            }else {
                share.setClaudeEmail("-");
            }

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


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request, @RequestBody ShareVO dto) {
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
        dto.setParentId(user.getId());
        int shareId = shareService.getBaseMapper().insert(dto);


        Account account = accountService.getById(dto.getAccountId());
        switch (dto.getType()) {
            case 1:
                gptConfigService.addShare(account,dto.getUniqueName(), shareId);
                break;
            case 2:
                return claudeConfigService.addShare(account, shareId);
        }

        return HttpResult.success(true);
    }

    @PatchMapping("/activate")
    public HttpResult<Boolean> activate(HttpServletRequest request, @RequestParam Integer id, @RequestParam Integer accountId) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Share share = shareService.getById(id);

        return gptConfigService.addShare(accountService.getById(accountId), share.getUniqueName(), share.getId());

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

    @PostMapping("/distribute")
    public HttpResult<Boolean> distribute(HttpServletRequest request, @RequestBody ShareVO share) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = accountService.getById(share.getAccountId());
        Share byId = shareService.getById(share.getId());
        if (account == null) {
            return HttpResult.error("账号不存在");
        }

        switch (account.getAccountType()) {
            case 1:
                return gptConfigService.addShare(account, byId.getUniqueName(), byId.getId());
            case 2:
                return claudeConfigService.addShare(account, byId.getId());
            default:
                return HttpResult.error("激活出现异常");
        }

        //return HttpResult.success(true);
    }

}
