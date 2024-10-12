package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShareService extends ServiceImpl<ShareMapper, Share> implements IService<Share> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UserDetailsService userDetailsService;
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
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${linux-do.oaifree.token-api}")
    private String tokenUrl;
    @Value("${linux-do.oaifree.auth-api}")
    private String authUrl;
    @Value("${chat_site:https://next.yeelo.top}")
    private String chatSite;


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

    public HttpResult<Share> getShareById(HttpServletRequest request, Integer id) {
        Share byId = getById(id);
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (user.getId()!=1 && (!byId.getId().equals(user.getId()) && !byId.getParentId().equals(user.getId()))) {
            return HttpResult.error("你无权访问该内容");
        }
        return HttpResult.success(byId);
    }

    public HttpResult<PageVO<ShareVO>> listShares(HttpServletRequest request, String emailAddr, Integer accountType, Integer page, Integer size) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {

            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = getByUserName(username);
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
        List<Share> shareList = findAll();
        List<ShareVO> shareVOS = ConvertUtil.convertList(shareList, ShareVO.class);

        // 获取gpt config
        Map<Integer, ShareGptConfig> gptMap = gptConfigService.list()
                                                      .stream().filter(e -> e.getShareId().equals(user.getId()) || accountIdMap.containsKey(e.getAccountId()))
                                                      .collect(Collectors.toMap(ShareGptConfig::getShareId, Function.identity()));
        Map<Integer, List<ShareGptConfig>> gptAccountMap = gptConfigService.list().stream().collect(Collectors.groupingBy(ShareGptConfig::getAccountId));
        Map<Integer, List<ShareClaudeConfig>> claudeAccountMap = claudeConfigService.list().stream().collect(Collectors.groupingBy(ShareClaudeConfig::getAccountId));
        Map<Integer, List<ShareApiConfig>> apiAccountMap = apiConfigService.list().stream().collect(Collectors.groupingBy(ShareApiConfig::getAccountId));

        // 获取claude config
        Map<Integer, ShareClaudeConfig> claudeMap = claudeConfigService.list()
                                                            .stream().filter(e -> e.getShareId().equals(user.getId()) || accountIdMap.containsKey(e.getAccountId()))
                                                            .collect(Collectors.toMap(ShareClaudeConfig::getShareId, Function.identity()));
        Map<Integer, ShareApiConfig> apiMap = apiConfigService.list()
                                                      .stream().filter(e -> e.getShareId().equals(user.getId()) || accountIdMap.containsKey(e.getAccountId()))
                                                      .collect(Collectors.toMap(ShareApiConfig::getShareId, Function.identity()));

        // 设置邮箱
        shareVOS = shareVOS.stream().filter(e -> user.getId().equals(1) || e.getId().equals(user.getId()) || (claudeMap.containsKey(e.getId()) || gptMap.containsKey(e.getId()))).collect(Collectors.toList());
        for (ShareVO share : shareVOS) {
            ShareGptConfig gptConfig = gptMap.get(share.getId());
            ShareClaudeConfig claudeConfig = claudeMap.get(share.getId());
            ShareApiConfig apiConfig = apiMap.get(share.getId());
            //if (gptConfig == null && claudeConfig == null) {
            //    continue;
            //}
            if (share.getId().equals(user.getId())) {
                share.setUniqueName(share.getUniqueName()+"(我)");
                share.setSelf(true);
            }
            share.setCurAdminId(user.getId());
            if (gptConfig != null) {
                int total = gptAccountMap.getOrDefault(gptConfig.getAccountId(),new ArrayList<>()).size();
                share.setGptEmail(accountService.getById(gptConfig.getAccountId()).getEmail());
                share.setGptCarName(accountService.getById(gptConfig.getAccountId()).getName()+" ("+total+"人)");;
                share.setGptConfigId(gptConfig.getId());
            } else {
                share.setGptEmail("-");
                share.setGptCarName("-");
            }

            if (claudeConfig != null) {
                int total = claudeAccountMap.getOrDefault(claudeConfig.getAccountId(),new ArrayList<>()).size();
                share.setClaudeEmail(accountService.getById(claudeConfig.getAccountId()).getEmail());
                share.setClaudeCarName(accountService.getById(claudeConfig.getAccountId()).getName()+" ("+total+"人)");
                share.setClaudeConfigId(claudeConfig.getId());
            } else {
                share.setClaudeEmail("-");
                share.setClaudeCarName("-");

            }

            if (apiConfig != null) {
                int total = apiAccountMap.getOrDefault(apiConfig.getAccountId(),new ArrayList<>()).size();
                share.setApiCarName(accountService.getById(apiConfig.getAccountId()).getName()+" ("+total+"人)");
                share.setApiConfigId(apiConfig.getId());
            } else {
                share.setApiCarName("-");
            }

            if (!StringUtils.hasText(share.getExpiresAt())) {
                share.setExpiresAt("-");
            }
            share.setPassword(null);
        }
        if (StringUtils.hasText(emailAddr)) {
            if (accountType==null){
                shareVOS = shareVOS.stream().filter(e-> (e.getGptEmail()!=null && e.getGptEmail().contains(emailAddr))|| (e.getClaudeEmail()!=null && e.getClaudeEmail().contains(emailAddr))||e.getUniqueName().contains(emailAddr)).collect(Collectors.toList());
            }
            else if (accountType.equals(1)) {
                shareVOS = shareVOS.stream().filter(e-> (e.getGptEmail()!=null && e.getGptEmail().contains(emailAddr))||e.getUniqueName().contains(emailAddr)).collect(Collectors.toList());
            }
            else if (accountType.equals(2)) {
                shareVOS = shareVOS.stream().filter(e-> (e.getClaudeEmail()!=null && e.getClaudeEmail().contains(emailAddr))||e.getUniqueName().contains(emailAddr)).collect(Collectors.toList());
            }
        }
        PageVO<ShareVO> pageVO = new PageVO<>();
        pageVO.setData(page==null?shareVOS:shareVOS.subList(Math.min(10*(page-1),shareVOS.size()-1),Math.min(10*(page-1)+size,shareVOS.size())));
        pageVO.setTotal(shareVOS.size());
        return HttpResult.success(pageVO);
    }

    public HttpResult<Boolean> deleteShare(HttpServletRequest request, Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Share share = findById(id);
        Account gptAccount = null;
        if (share != null && user.getId().equals(1)) {
            removeById(id);
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

    public HttpResult<Boolean> addShare(HttpServletRequest request, ShareVO dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        LambdaQueryWrapper<Share> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Share::getUniqueName, dto.getUniqueName());
        List<Share> shareList = list(lambdaQueryWrapper);
        if (!CollectionUtils.isEmpty(shareList)) {
            return HttpResult.error("用户名已存在");
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        dto.setParentId(user.getId());
        getBaseMapper().insert(dto);
        int shareId = dto.getId();

        Account account = accountService.getById(dto.getAccountId());
        switch (account.getAccountType()) {
            case 1:
                return gptConfigService.addShare(account, dto.getUniqueName(), shareId, null);
            case 2:
                return claudeConfigService.addShare(account, shareId,null);
            case 3:
                return apiConfigService.addShare(account, shareId, null);
            default:
                return HttpResult.success(false);

        }
    }

    public HttpResult<Boolean> updateShare(HttpServletRequest request, Share dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (dto.getId()==null){
            log.error("更新用户出错，用户id为空");
            return HttpResult.error("更新用户异常，用户id为空");
        }

        Share share = getById(dto.getId());

        Share updateVo = new Share();
        if (user.getId().equals(share.getParentId())) {
            updateVo.setExpiresAt(StringUtils.hasText(dto.getExpiresAt())  ? dto.getExpiresAt() : "-");
        }
        updateVo.setId(dto.getId());

        updateVo.setComment(dto.getComment()==null ? "" : dto.getComment());
        updateById(updateVo);
        return HttpResult.success(true);
    }

    public HttpResult<Boolean> distributeShare(HttpServletRequest request, ShareVO share) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = accountService.getById(share.getAccountId());
        Share byId = getById(share.getId());
        if (share.getAccountId()!=null && share.getAccountId().equals(-1)) {
            return gptConfigService.deleteShare(share.getId());
        }
        else if (share.getAccountId()!=null && share.getAccountId().equals(-2)) {
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
                return gptConfigService.addShare(account, byId.getUniqueName(), byId.getId(), null);
            case 2:
                return claudeConfigService.addShare(account, byId.getId(),null);
            case 3:
                return apiConfigService.addShare(account, byId.getId(), null);
            default:
                return HttpResult.error("激活出现异常");
        }
    }

    public HttpResult<String> checkLinuxDoUser(String username, String jmc, HttpServletRequest request) {
        String jmcFromSession = request.getSession().getAttribute("jmc") == null ? "" : request.getSession().getAttribute("jmc").toString();
        if (!StringUtils.hasText(jmc) || !jmc.equals(jmcFromSession)) {
            return HttpResult.error("请遵守登录规范！");
        }
        Share user = getByUserName(username);
        if (Objects.isNull(user)) {
            // 新建默认share
            ShareVO share = new ShareVO();
            share.setUniqueName(username);
            share.setIsShared(false);
            share.setPassword(passwordEncoder.encode("123456"));
            share.setComment("");
            save(share);
            //return HttpResult.error("当前用户不支持登录面板,请联系管理员");
        }
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String jwt = jwtTokenUtil.generateToken(userDetails);
        return HttpResult.success(jwt);
    }

    public HttpResult<String> getGptShare(Integer gptConfigId) {
        ShareGptConfig gptShare = gptConfigService.getById(gptConfigId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");

        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("share_token", gptShare.getShareToken());
        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("login_url")) {
                String loginUrl = map.get("login_url").toString();
                loginUrl = loginUrl.replace(CommonConst.DEFAULT_AUTH_URL, authUrl);
                log.info("获取login url成功:{}", loginUrl);
                return HttpResult.success(loginUrl);
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("获取登录信息异常");
        }
        return HttpResult.error("获取登录信息失败");
    }

    public HttpResult<Boolean> updateParent(Integer shareId, HttpServletRequest request) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = getByUserName(username);
        if (user==null){
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Share byId = getById(shareId);
        if (byId!=null && byId.getParentId().equals(user.getId())){
            byId.setParentId(user.getParentId());
            Share update = new Share();
            update.setId(shareId);
            update.setParentId(shareId);
            updateById(update);
        }else {
            return HttpResult.error("您无权进行该操作");
        }

        return HttpResult.success(true);
    }

    public HttpResult<String> getClaudeShare(Integer claudeConfigId) {
        ShareClaudeConfig claudeShare = claudeConfigService.getById(claudeConfigId);
        Account account = accountService.getById(claudeShare.getAccountId());
        Share share = getById(claudeShare.getShareId());
        String token = claudeConfigService.generateAutoToken(account, share,null);

        if (token==null){
            return HttpResult.error("获取登录信息失败");
        }else {
            return HttpResult.success(token);
        }
    }

    public HttpResult<String> getApiShare(Integer apiConfigId) {
        ShareApiConfig apiShare = apiConfigService.getById(apiConfigId);
        Account account = accountService.getById(apiShare.getAccountId());
        String token = chatSite +"/#/?settings={%22key%22:%22"+ account.getRefreshToken()+"%22,%22url%22:%22"+account.getAccessToken()+"%22}";

        if (token==null){
            return HttpResult.error("获取登录信息失败");
        }else {
            return HttpResult.success(token);
        }
    }
}
