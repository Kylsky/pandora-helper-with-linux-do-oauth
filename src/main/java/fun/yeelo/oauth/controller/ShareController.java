package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareController {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${linux-do.fuclaude}")
    private String fuclaudeUrl;
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
    @Autowired
    private UserDetailsService userDetailsService;
    @Value("${linux-do.oaifree.token-api}")
    private String tokenUrl;
    private final static String DEFAULT_AUTH_URL = "https://new.oaifree.com";
    @Value("${linux-do.oaifree.auth-api}")
    private String authUrl;

    @GetMapping("/list")
    public HttpResult<List<ShareVO>> list(HttpServletRequest request,
                                          @RequestParam(required = false) String emailAddr,
                                          @RequestParam(required = false) Integer accountType) {
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
                                         .filter(e -> user.getId().equals(1) || e.getUserId().equals(user.getId())
                                                              && (!StringUtils.hasText(emailAddr) || e.getEmail().contains(emailAddr))
                                                              && (accountType==null || e.getAccountType().equals(accountType))).collect(Collectors.toList()
                                    );
        Map<Integer, Account> accountIdMap = accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));
        // 筛选accountId在账号map内的
        List<Share> shareList = shareService.findAll();
        List<ShareVO> shareVOS = ConvertUtil.convertList(shareList, ShareVO.class);

        // 获取gpt config
        Map<Integer, ShareGptConfig> gptMap = gptConfigService.list()
                                                      .stream().filter(e -> e.getShareId().equals(user.getId()) || accountIdMap.containsKey(e.getAccountId()))
                                                      .collect(Collectors.toMap(ShareGptConfig::getShareId, Function.identity()));
        Map<Integer, List<ShareGptConfig>> gptAccountMap = gptConfigService.list().stream().collect(Collectors.groupingBy(ShareGptConfig::getAccountId));
        Map<Integer, List<ShareClaudeConfig>> claudeAccountMap = claudeConfigService.list().stream().collect(Collectors.groupingBy(ShareClaudeConfig::getAccountId));

        // 获取claude config
        Map<Integer, ShareClaudeConfig> claudeMap = claudeConfigService.list()
                                                            .stream().filter(e -> e.getShareId().equals(user.getId()) || accountIdMap.containsKey(e.getAccountId()))
                                                            .collect(Collectors.toMap(ShareClaudeConfig::getShareId, Function.identity()));

        // 设置邮箱
        shareVOS = shareVOS.stream().filter(e -> e.getId().equals(user.getId()) || (claudeMap.containsKey(e.getId()) || gptMap.containsKey(e.getId()))).collect(Collectors.toList());
        for (ShareVO share : shareVOS) {
            ShareGptConfig gptConfig = gptMap.get(share.getId());
            ShareClaudeConfig claudeConfig = claudeMap.get(share.getId());
            //if (gptConfig == null && claudeConfig == null) {
            //    continue;
            //}
            if (share.getId().equals(user.getId())) {
                share.setUniqueName(share.getUniqueName()+"(我)");
            }
            share.setCurAdminId(user.getId());
            if (gptConfig != null) {
                int size = gptAccountMap.getOrDefault(gptConfig.getAccountId(),new ArrayList<>()).size();
                share.setGptEmail(accountService.getById(gptConfig.getAccountId()).getEmail());
                share.setGptCarName(accountService.getById(gptConfig.getAccountId()).getName()+" ("+size+"人)");;
                share.setGptConfigId(gptConfig.getId());
            } else {
                share.setGptEmail("-");
                share.setGptCarName("-");
            }

            if (claudeConfig != null) {
                int size = claudeAccountMap.getOrDefault(claudeConfig.getAccountId(),new ArrayList<>()).size();
                share.setClaudeEmail(accountService.getById(claudeConfig.getAccountId()).getEmail());
                share.setClaudeCarName(accountService.getById(claudeConfig.getAccountId()).getName()+" ("+size+"人)");
                share.setClaudeConfigId(claudeConfig.getId());
            } else {
                share.setClaudeEmail("-");
                share.setClaudeCarName("-");

            }

            if (!StringUtils.hasText(share.getExpiresAt())) {
                share.setExpiresAt("-");
            }
            share.setPassword(null);
        }
        if (StringUtils.hasText(emailAddr)) {
            if (accountType!=null && accountType.equals(1)) {
                shareVOS = shareVOS.stream().filter(e-> e.getGptEmail().contains(emailAddr)).collect(Collectors.toList());
            }
            else if (accountType!=null && accountType.equals(2)) {
                shareVOS = shareVOS.stream().filter(e-> e.getClaudeEmail().contains(emailAddr)).collect(Collectors.toList());
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
        Account gptAccount = null;
        if (share != null && user.getId().equals(1)) {
            shareService.removeById(id);
            ShareGptConfig one = gptConfigService.getOne(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, id));
            if (one!=null) {
                gptAccount = accountService.getById(one.getAccountId());
            }
            gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, id));
            claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, id));
        } else {
            return HttpResult.error("您无权删除该账号");
        }

        // 删除oaifree的share token
        if (gptAccount!=null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("access_token", gptAccount.getAccessToken());
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

        String expiresAt = dto.getExpiresAt();

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        dto.setParentId(user.getId());
        shareService.getBaseMapper().insert(dto);
        int shareId = dto.getId();

        Account account = accountService.getById(dto.getAccountId());
        switch (account.getAccountType()) {
            case 1:
                return gptConfigService.addShare(account, dto.getUniqueName(), shareId, expiresAt);
            case 2:
                return claudeConfigService.addShare(account, shareId);
            default:
                return HttpResult.success(false);

        }

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

        return gptConfigService.addShare(accountService.getById(accountId), share.getUniqueName(), share.getId(), user.getExpiresAt());

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
        } else if (StringUtils.hasText(dto.getPassword())){
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
        if (share.getAccountId()!=null && share.getAccountId().equals(-1)) {
            gptConfigService.deleteShare(share.getId());
            return HttpResult.success();
        }else if (share.getAccountId()!=null && share.getAccountId().equals(-2)) {
            claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId,share.getId()));
            return HttpResult.success();
        }
        else if (account == null) {
            return HttpResult.error("账号不存在");
        }

        switch (account.getAccountType()) {
            case 1:
                return gptConfigService.addShare(account, byId.getUniqueName(), byId.getId(), byId.getExpiresAt());
            case 2:
                return claudeConfigService.addShare(account, byId.getId());
            default:
                return HttpResult.error("激活出现异常");
        }

        //return HttpResult.success(true);
    }

    @GetMapping("/checkUser")
    public HttpResult<String> checkLinuxDoUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
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
            share.setComment("unassigned");
            shareService.save(share);
            //return HttpResult.error("当前用户不支持登录面板,请联系管理员");
        }
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String jwt = jwtTokenUtil.generateToken(userDetails);
        return HttpResult.success(jwt);
    }

    @GetMapping("/updateParent")
    public HttpResult<Boolean> updateParent(@RequestParam Integer shareId, HttpServletRequest request) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user==null){
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Share byId = shareService.getById(shareId);
        if (byId!=null && byId.getParentId().equals(user.getId())){
            byId.setParentId(user.getParentId());
            Share update = new Share();
            update.setId(shareId);
            update.setParentId(shareId);
            shareService.updateById(update);
        }else {
            return HttpResult.error("您无权进行该操作");
        }

        return HttpResult.success(true);
    }

    @GetMapping("/getGptShare")
    public HttpResult<String> getGptShare(@RequestParam Integer gptConfigId) {
        ShareGptConfig gptShare = gptConfigService.getById(gptConfigId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("share_token", gptShare.getShareToken());
        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("login_url")) {
                String loginUrl = map.get("login_url").toString();
                loginUrl = loginUrl.replace(DEFAULT_AUTH_URL, authUrl);
                log.info("获取login url成功:{}", loginUrl);
                return HttpResult.success(loginUrl);
                // 打印user信息，用json
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("获取登录信息异常");
        }
        return HttpResult.error("获取登录信息失败");
    }

    @GetMapping("/getClaudeShare")
    public HttpResult<String> getClaudeShare(@RequestParam Integer claudeConfigId) {
        ShareClaudeConfig claudeShare = claudeConfigService.getById(claudeConfigId);
        Account account = accountService.getById(claudeShare.getAccountId());
        Share share = shareService.getById(claudeShare.getShareId());
        String token = claudeConfigService.generateAutoToken(account, share);

        if (token==null){
            return HttpResult.error("获取登录信息失败");
        }else {
            return HttpResult.success(token);
        }
    }
}
