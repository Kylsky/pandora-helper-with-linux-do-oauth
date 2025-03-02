package fun.yeelo.oauth.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.annotation.RequireLogin;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.config.MirrorConfig;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.account.AccountVO;
import fun.yeelo.oauth.domain.car.CarApply;
import fun.yeelo.oauth.domain.share.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import fun.yeelo.oauth.utils.OpenAIUtil;
import fun.yeelo.oauth.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountService extends ServiceImpl<AccountMapper, Account> implements IService<Account> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mirror.host}")
    private String mirrorHost;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ShareService shareService;

    @Autowired
    private ClaudeConfigService claudeConfigService;

    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private GptConfigService gptConfigService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CarService carService;
    @Autowired
    private ApiConfigService apiConfigService;
    @Autowired
    private MirrorConfig mirrorConfig;
    @Autowired
    private OpenAIUtil openAIUtil;

    public List<Account> findAll() {
        return accountMapper.selectList(null);
    }

    @RequireLogin
    public HttpResult<String> share(HttpServletRequest request, Integer id) {
        Share user = UserContext.getCurrentUser();
        boolean b = checkIdWithinFiveMinutes(id, true);
        if (b) {
            return HttpResult.error("当前账号使用繁忙，请稍后再试");
        }
        Account account = getById(id);
        String addr = "";
        switch (account.getAccountType()) {
            case 1 -> {
                //addr = shareService.generateGPTUrl(user,account);
                HttpResult<ShareVO> mirrorRes = mirrorConfig.getMirrorUrl(user.getUniqueName(), account.getId());
                if (mirrorRes.isStatus()) {
                    addr = mirrorRes.getData().getAddress();
                } else {
                    return HttpResult.error("当前账号异常，请选择其他账号");
                }
            }
            case 2 -> addr = claudeConfigService.generateAutoToken(account, user, 3600);
        }

        return HttpResult.success(addr);
    }

    public Account findById(Integer id) {
        return accountMapper.selectById(id);
    }

    public List<Account> findByUserId(Integer userId) {
        return accountMapper.getByUserId(userId);
    }

    public void delete(Integer id) {
        accountMapper.deleteById(id);
    }

    public Account getById(Integer accountId) {
        return accountMapper.selectById(accountId);
    }

    public boolean checkIdWithinFiveMinutes(Integer id, Boolean addFlag) {
        Cache cache = cacheManager.getCache("idCount");
        if (cache == null) {
            throw new IllegalStateException("Cache not configured properly");
        }

        // 获取当前时间
        Instant now = Instant.now();

        // 获取缓存中的时间戳列表
        List<Instant> timestamps = cache.get(id, List.class) == null ? new LinkedList<>() : cache.get(id, List.class);
        // 删除五分钟前的时间戳
        timestamps = timestamps.stream()
                             .filter(timestamp -> timestamp.isAfter(now.minusSeconds(300)))
                             .collect(Collectors.toList());
        if (timestamps == null) {
            timestamps = new LinkedList<>();
        }

        // 过滤出五分钟内的时间戳
        timestamps = timestamps.stream()
                             .filter(timestamp -> timestamp.isAfter(now.minusSeconds(300)))
                             .collect(Collectors.toList());

        // 判断是否达到三次
        if (timestamps.size() >= 3) {
            log.info("ACCOUNT ID: " + id + " 在五分钟内已经出现了三次！");
            return true;
        }

        // 添加当前时间戳到列表
        if (addFlag) {
            log.info("检测到ACCOUNT ID: " + id + " 的使用");
            timestamps.add(now);
            // 更新缓存
            cache.put(id, timestamps);
        }

        return false;
    }

    @RequireLogin
    public HttpResult<List<InfoVO>> statistic(HttpServletRequest request, Integer id) {
        Account byId = getById(id);
        List<ShareGptConfig> gptShares = gptConfigService.list().stream().filter(e -> e.getAccountId().equals(id)).collect(Collectors.toList());
        String chatUrl = mirrorHost + "/api/usage";
        List<InfoVO> info = new ArrayList<>();
        Map<Integer, Share> shareMap = shareService.list().stream().collect(Collectors.toMap(Share::getId, Function.identity()));
        gptShares.parallelStream().forEach(e -> {
            InfoVO infoVO = new InfoVO();
            infoVO.setUniqueName(shareMap.get(e.getShareId()).getUniqueName());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Bearer " + byId.getAccessToken());
            Map map;

            try {
                UsageVO usageVO = new UsageVO();
                //String shareToken = mirrorConfig.getSimpleMirrorUrl(infoVO.getUniqueName(), id).getData().replace(mirrorHost + "/api/not-login?user_gateway_token=", "");
                String shareToken = "fk-921b44473f5b970c";
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(chatUrl + "?share_token=" + shareToken, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                Map<String, Integer> usage = (Map<String, Integer>) map.get("data");
                usage.entrySet().stream().forEach(entry -> {
                    switch (entry.getKey()) {
                        case "gpt-4o" -> usageVO.setGpt_4o(entry.getValue());
                        case "gpt-4" -> usageVO.setGpt_4(entry.getValue());
                        case "gpt-4o-mini" -> usageVO.setGpt_4o_mini(entry.getValue());
                        case "o1" -> usageVO.setO1(entry.getValue());
                        case "o1-mini" -> usageVO.setO1_mini(entry.getValue());
                    }
                });
                infoVO.setUsage(usageVO);
            } catch (Exception ex) {
                log.info("获取使用情况异常", ex);
            }
            info.add(infoVO);
        });
        return HttpResult.success(info);
    }

    @RequireLogin
    public HttpResult<PageVO<AccountVO>> listAccount(HttpServletRequest request, String emailAddr, Integer page, Integer size, Integer type) {
        Share user = UserContext.getCurrentUser();
        List<Account> accountList = type != null ? list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType, type)) : findByUserId(user.getId());
        if (StringUtils.hasText(emailAddr)) {
            accountList = accountList.stream().filter(e -> e.getEmail().contains(emailAddr)).collect(Collectors.toList());
        }
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        Map<Integer, List<CarApply>> accountIdMap = carService.list().stream().collect(Collectors.groupingBy(CarApply::getAccountId));
        accountVOS.forEach(e -> {
            //e.setEmail("车辆"+(num.getAndIncrement()));
            e.setType(e.getAccountType().equals(1) ? "ChatGPT" : (e.getAccountType().equals(2) ? "Claude" : "API"));
            e.setCount(accountIdMap.getOrDefault(e.getId(), new ArrayList<>()).size());
        });
        accountVOS = accountVOS.stream()
                             .filter(e -> type == null || (type.equals(e.getAccountType()) && e.getShared().equals(1) && e.getAuto().equals(1)))
                             .sorted(Comparator.comparing(AccountVO::getType)).collect(Collectors.toList());
        accountVOS.stream().forEach(e -> {
            e.setRefreshToken(null);
            e.setAccessToken(null);
            if (type != null) {
                e.setEmail(null);
            }
        });
        for (AccountVO accountVO : accountVOS) {
            Integer id = accountVO.getId();
            accountVO.setSessionToken(checkIdWithinFiveMinutes(id, false) ? "1" : "");
        }
        PageVO<AccountVO> pageVO = new PageVO<>();
        pageVO.setTotal(accountVOS.size());
        pageVO.setData(page == null ? accountVOS : accountVOS.subList(10 * (page - 1), Math.min(10 * (page - 1) + size, accountVOS.size())));
        return HttpResult.success(pageVO);
    }

    @RequireLogin
    public HttpResult<Boolean> deleteAccount(HttpServletRequest request, Integer id) {
        Share user = UserContext.getCurrentUser();
        Account account = findById(id);
        if (account != null && account.getUserId().equals(user.getId())) {
            delete(id);
            Integer accountType = account.getAccountType();
            switch (accountType) {
                case 1 ->
                        gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getAccountId, account.getId()));
                case 2 ->
                        claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getAccountId, id));
                case 3 ->
                        apiConfigService.remove(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getAccountId, id));
            }
        } else {
            return HttpResult.error("您无权删除该账号");
        }

        return HttpResult.success(true);
    }

    @RequireLogin
    public HttpResult<Account> getAccountById(HttpServletRequest request, Integer id) {
        Share user = UserContext.getCurrentUser();
        Account byId = getById(id);
        if (!byId.getUserId().equals(user.getId()) && user.getId() != 1) {
            return HttpResult.error("你无权访问该账号");
        }
        return HttpResult.success(byId);
    }

    @RequireLogin
    public HttpResult<Boolean> saveOrUpdateAccount(HttpServletRequest request, Account dto) {
        Share user = UserContext.getCurrentUser();
        if (!StringUtils.hasText(dto.getName())) {
            dto.setName(dto.getEmail());
        }
        if (dto.getId() == null) {
            return HttpResult.error("账号ID不存在");

        }
        dto.setUpdateTime(LocalDateTime.now());
        dto.setUserId(user.getId());
        Account account = findById(dto.getId());
        // 假设账号共享从开启到关闭，则删除所有共享的配置
        if (account != null
                    && account.getUserId().equals(user.getId())
                    && account.getShared().equals(1)
                    && dto.getShared().equals(0)) {
            Integer accountType = account.getAccountType();
            switch (accountType) {
                case 1 ->
                        gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getAccountId, account.getId()));
                case 2 ->
                        claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getAccountId, account.getId()));
                case 3 ->
                        apiConfigService.remove(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getAccountId, account.getId()));
            }
        }
        saveOrUpdate(dto);
        if (StringUtils.hasText(dto.getAccessToken())) {
            CompletableFuture.runAsync(() -> openAIUtil.checkAccount(dto.getAccessToken(), dto.getEmail(), dto.getId()));
        }

        return HttpResult.success(true);
    }

    @RequireLogin
    public HttpResult<Boolean> refresh(HttpServletRequest request, Integer id) {
        Share user = UserContext.getCurrentUser();
        Account account = getById(id);
        if (account == null) {
            return HttpResult.error("账号不存在");
        }
        if (!StringUtils.hasText(account.getRefreshToken())) {
            return HttpResult.error("账号未配置refreshToken");
        }

        try {
            Integer accountId = account.getId();

            openAIUtil.refresh(accountId, account.getRefreshToken(), account.getEmail());

            //HttpHeaders headers = new HttpHeaders();
            //headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
            //headers.setContentType(MediaType.APPLICATION_JSON);
            //headers.set(HttpHeaders.ACCEPT, "*/*");
            //headers.set(HttpHeaders.USER_AGENT,"PostmanRuntime/7.43.0");
            //headers.set(HttpHeaders.CONNECTION,"keep-alive");
            //headers.set(HttpHeaders.HOST,"auth0.openai.com");
            //ObjectNode body = objectMapper.createObjectNode();
            //body.put("refresh_token", account.getRefreshToken());
            //body.put("redirect_uri", "com.openai.chat://auth0.openai.com/ios/com.openai.chat/callback");
            //body.put("grant_type", "refresh_token");
            //body.put("client_id", "pdlLIX2Y72MIl2rhLhTE9VV9bN905kBh");
            //headers.set("Accept-Charset", "UTF-8"); // 声明接受的字符集
            //headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.toString().length()));
            //ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity("https://auth0.openai.com/oauth/token", new HttpEntity<>(body, headers), String.class);
            //Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            //if (map.containsKey("access_token")) {
            //    log.info("refresh success");
            //    String newToken = map.get("access_token").toString();
            //    Account updateDTO = new Account();
            //    updateDTO.setId(accountId);
            //    updateDTO.setAccessToken(newToken);
            //    // 刷新后校验账号
            //    openAIUtil.checkAccount(newToken, account.getEmail(), accountId);
            //    updateDTO.setUpdateTime(LocalDateTime.now());
            //    this.saveOrUpdate(updateDTO);
            //    log.info("刷新账号{}成功", account.getEmail());
            //}
        } catch (Exception e) {
            log.error("刷新access_token异常,异常账号:{}", account.getEmail(), e);
            return HttpResult.error("刷新 access_token异常 , 请检查 refresh_token 是否有效");
        }


        return HttpResult.success(true);
    }

    @RequireLogin
    public HttpResult<Boolean> addAccount(HttpServletRequest request, AccountVO dto) {
        Share user = UserContext.getCurrentUser();
        dto.setUserId(user.getId());
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(dto);

        if (StringUtils.hasText(dto.getAccessToken())) {
            CompletableFuture.runAsync(() -> openAIUtil.checkAccount(dto.getAccessToken(), dto.getEmail(), dto.getId()));
        }

        return HttpResult.success(true);
    }

    @RequireLogin
    public HttpResult<Account> getAccount(HttpServletRequest request, Integer accountId) {
        Share user = UserContext.getCurrentUser();
        Account account = getById(accountId);
        if (account == null) {
            return HttpResult.error("账号不存在");
        }

        return HttpResult.success(account);
    }

    @RequireLogin
    public HttpResult<List<LabelDTO>> emailOptions(HttpServletRequest request, Integer type) {
        Share user = UserContext.getCurrentUser();
        List<LabelDTO> emails = list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType, type))
                                        .stream()
                                        .filter(e -> e.getUserId().equals(user.getId()))
                                        .map(e -> new LabelDTO(e.getId().toString(), e.getName(), e.getName()))
                                        .sorted(Comparator.comparing(LabelDTO::getLabel))
                                        .collect(Collectors.toList());
        List<LabelDTO> res = new ArrayList<>();
        LabelDTO labelDTO = new LabelDTO(type.equals(1) ? "-1" : type.equals(2) ? "-2" : "-3", "----默认选项：下车----", "----默认选项：下车----");
        res.add(labelDTO);
        res.addAll(emails);
        return HttpResult.success(res);
    }
}
