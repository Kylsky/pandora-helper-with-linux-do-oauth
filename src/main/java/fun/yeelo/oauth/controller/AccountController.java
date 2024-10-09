package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jndi.ldap.Ber;
import com.sun.org.apache.xpath.internal.operations.Bool;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REFRESH_URL = "https://token.oaifree.com/api/auth/refresh";
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
    private CarService carService;
    @Autowired
    private GptConfigService gptConfigService;
    @Autowired
    private ClaudeConfigService claudeConfigService;

    @Autowired
    private CacheManager cacheManager;

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

    @GetMapping("/share")
    public HttpResult<String> share(HttpServletRequest request,
                                    @RequestParam(required = false) Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        boolean b = checkIdWithinFiveMinutes(id,true);
        if (b){
            return HttpResult.error("当前账号使用繁忙，请稍后再试");
        }
        Account account = accountService.getById(id);
        String addr = "";
        switch (account.getAccountType()) {
            case 1:
                break;
            case 2:
                addr = claudeConfigService.generateAutoToken(account, user, 3600);
                break;
        }

        return HttpResult.success(addr);
    }


    @GetMapping("/statistic")
    public HttpResult<List<InfoVO>> statistic(HttpServletRequest request, Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account byId = accountService.getById(id);
        List<ShareGptConfig> gptShares = gptConfigService.list().stream().filter(e -> e.getAccountId().equals(id)).collect(Collectors.toList());
        String chatUrl = "https://chat.oaifree.com/token/info/";
        List<InfoVO> info = new ArrayList<>();
        Map<Integer, Share> shareMap = shareService.list().stream().collect(Collectors.toMap(Share::getId, Function.identity()));
        gptShares.parallelStream().forEach(e -> {
            InfoVO infoVO = new InfoVO();
            infoVO.setUniqueName(shareMap.get(e.getShareId()).getUniqueName());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Bearer " + byId.getAccessToken());
            Map map = null;

            try {
                UsageVO usageVO = new UsageVO();
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(chatUrl + e.getShareToken(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
                map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                Map<String, String> usage = (Map<String, String>) map.get("usage");
                usage.entrySet().stream().forEach(entry -> {
                    switch (entry.getKey()) {
                        case "gpt-4o":
                            usageVO.setGpt4o(Integer.valueOf(entry.getValue()));
                            break;
                        case "gpt-4":
                            usageVO.setGpt4(Integer.valueOf(entry.getValue()));
                            break;
                        case "gpt-4o-mini":
                            usageVO.setGpt4omini(Integer.valueOf(entry.getValue()));
                            break;
                        case "o1-preview":
                            usageVO.setO1Preview(Integer.valueOf(entry.getValue()));
                            break;
                        case "o1-mini":
                            usageVO.setO1Mini(Integer.valueOf(entry.getValue()));
                            break;
                    }
                    infoVO.setUsage(usageVO);
                });
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            ;
            info.add(infoVO);
        });
        return HttpResult.success(info);
    }

    @GetMapping("/list")
    public HttpResult<PageVO<AccountVO>> list(HttpServletRequest request,
                                              @RequestParam(required = false) String emailAddr,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) Integer type) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        List<Account> accountList = type != null ? accountService.list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType,type)) : accountService.findByUserId(user.getId());
        if (StringUtils.hasText(emailAddr)) {
            accountList = accountList.stream().filter(e -> e.getEmail().contains(emailAddr)).collect(Collectors.toList());
        }
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        Map<Integer, List<CarApply>> accountIdMap = carService.list().stream().collect(Collectors.groupingBy(CarApply::getAccountId));
        accountVOS.forEach(e -> {
            //e.setEmail("车辆"+(num.getAndIncrement()));
            e.setType(e.getAccountType().equals(1) ? "ChatGPT" : "Claude");
            e.setCount(accountIdMap.getOrDefault(e.getId(), new ArrayList<>()).size());
        });
        accountVOS = accountVOS.stream().filter(e -> type == null || (type.equals(e.getAccountType())&&e.getShared().equals(1)&&e.getAuto().equals(1))).sorted(Comparator.comparing(AccountVO::getType)).collect(Collectors.toList());
        for (AccountVO accountVO : accountVOS) {
            Integer id = accountVO.getId();
            accountVO.setSessionToken(checkIdWithinFiveMinutes(id,false) ?"1":"");
        }
        PageVO<AccountVO> pageVO = new PageVO<>();
        pageVO.setTotal(accountVOS.size());
        pageVO.setData(page == null ? accountVOS : accountVOS.subList(10 * (page - 1), Math.min(10 * (page - 1) + size, accountVOS.size())));
        return HttpResult.success(pageVO);
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
        Account account = accountService.findById(id);
        if (account != null && account.getUserId().equals(user.getId())) {
            accountService.delete(id);
            Integer accountType = account.getAccountType();
            switch (accountType) {
                case 1:
                    List<ShareGptConfig> shareList = gptConfigService.list(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getAccountId, id));
                    CompletableFuture.runAsync(() -> {
                        shareList.parallelStream().forEach(e -> {
                            ShareVO shareVO = new ShareVO();
                            shareVO.setAccountId(-1);
                            shareVO.setId(e.getId());
                            shareService.distribute(shareVO);
                        });
                    });
                    break;
                case 2:
                    claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getAccountId, id));
                    break;
            }
        } else {
            return HttpResult.error("您无权删除该账号");
        }

        return HttpResult.success(true);
    }

    @GetMapping("/getById")
    public HttpResult<Account> getById(HttpServletRequest request, @RequestParam Integer id) {
        Account byId = accountService.getById(id);
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (!byId.getUserId().equals(user.getId()) || user.getId() != 1) {
            return HttpResult.error("你无权访问该账号");
        }
        return HttpResult.success(byId);
    }


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request, @RequestBody AccountVO dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        dto.setUserId(user.getId());
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        accountService.saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    @PatchMapping("/update")
    public HttpResult<Boolean> update(HttpServletRequest request, @RequestBody Account dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (!StringUtils.hasText(dto.getName())) {
            dto.setName(dto.getEmail());
        }
        dto.setUpdateTime(LocalDateTime.now());
        dto.setUserId(user.getId());
        accountService.saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    @PostMapping("/refresh")
    public HttpResult<Boolean> refresh(HttpServletRequest request, @RequestParam Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = accountService.getById(id);
        if (account == null) {
            return HttpResult.error("账号不存在");
        }
        if (!StringUtils.hasText(account.getRefreshToken())) {
            return HttpResult.error("账号未配置refreshToken");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
            personJsonObject.add("refresh_token", account.getRefreshToken());
            ResponseEntity<String> stringResponseEntity = restTemplate.exchange(REFRESH_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("access_token")) {
                log.info("refresh success");
                String newToken = map.get("access_token").toString();
                Account updateDTO = new Account();
                updateDTO.setId(id);
                updateDTO.setAccessToken(newToken);
                accountService.saveOrUpdate(updateDTO);
                return HttpResult.success(true);
            }
        } catch (Exception e) {
            log.error("Check user error:", e);
            return HttpResult.error("删除用户异常");
        }


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


    @GetMapping("/options")
    public HttpResult<List<LabelDTO>> emailOptions(HttpServletRequest request,
                                                   @RequestParam Integer type) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String s = jwtTokenUtil.extractUsername(token);
        Share byUserName = shareService.getByUserName(s);
        List<LabelDTO> emails = accountService.list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType, type))
                                        .stream()
                                        .filter(e -> e.getUserId().equals(byUserName.getId()))
                                        .map(e -> new LabelDTO(e.getId().toString(), e.getName(), e.getName()))
                                        .sorted(Comparator.comparing(LabelDTO::getLabel))
                                        .collect(Collectors.toList());
        List<LabelDTO> res = new ArrayList<>();
        LabelDTO labelDTO = new LabelDTO(type.equals(1) ? "-1" : "-2", "----默认选项：下车----", "----默认选项：下车----");
        res.add(labelDTO);
        res.addAll(emails);
        return HttpResult.success(res);
    }

}
