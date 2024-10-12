package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
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

    public List<Account> findAll() {
        return accountMapper.selectList(null);
    }

    public HttpResult<String> share(HttpServletRequest request, Integer id) {
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
        Account account = getById(id);
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
        Account byId = getById(id);
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
            Map map;

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

    public HttpResult<PageVO<AccountVO>> listAccount(HttpServletRequest request, String emailAddr, Integer page, Integer size, Integer type) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        List<Account> accountList = type != null ? list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType,type)) : findByUserId(user.getId());
        if (StringUtils.hasText(emailAddr)) {
            accountList = accountList.stream().filter(e -> e.getEmail().contains(emailAddr)).collect(Collectors.toList());
        }
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        Map<Integer, List<CarApply>> accountIdMap = carService.list().stream().collect(Collectors.groupingBy(CarApply::getAccountId));
        accountVOS.forEach(e -> {
            //e.setEmail("车辆"+(num.getAndIncrement()));
            e.setType(e.getAccountType().equals(1) ? "ChatGPT" : e.getAccountType().equals(2)?"Claude":"API");
            e.setCount(accountIdMap.getOrDefault(e.getId(), new ArrayList<>()).size());
        });
        accountVOS = accountVOS.stream()
                             .filter(e -> type == null || (type.equals(e.getAccountType())&&e.getShared().equals(1)&&e.getAuto().equals(1)))
                             .sorted(Comparator.comparing(AccountVO::getType)).collect(Collectors.toList());
        for (AccountVO accountVO : accountVOS) {
            Integer id = accountVO.getId();
            accountVO.setSessionToken(checkIdWithinFiveMinutes(id,false) ?"1":"");
        }
        PageVO<AccountVO> pageVO = new PageVO<>();
        pageVO.setTotal(accountVOS.size());
        pageVO.setData(page == null ? accountVOS : accountVOS.subList(10 * (page - 1), Math.min(10 * (page - 1) + size, accountVOS.size())));
        return HttpResult.success(pageVO);
    }

    public HttpResult<Boolean> deleteAccount(HttpServletRequest request, Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = findById(id);
        if (account != null && account.getUserId().equals(user.getId())) {
            delete(id);
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

    public HttpResult<Account> getAccountById(HttpServletRequest request, Integer id) {
        Account byId = getById(id);
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

    public HttpResult<Boolean> saveOrUpdateAccount(HttpServletRequest request,Account dto) {
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
        saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    public HttpResult<Boolean> refresh(HttpServletRequest request, Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = getById(id);
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
            ResponseEntity<String> stringResponseEntity = restTemplate.exchange(CommonConst.REFRESH_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("access_token")) {
                log.info("refresh success");
                String newToken = map.get("access_token").toString();
                Account updateDTO = new Account();
                updateDTO.setId(id);
                updateDTO.setAccessToken(newToken);
                saveOrUpdate(updateDTO);
                return HttpResult.success(true);
            }
        } catch (Exception e) {
            log.error("Check user error:", e);
            return HttpResult.error("删除用户异常");
        }


        return HttpResult.success(true);
    }

    public HttpResult<Boolean> addAccount(HttpServletRequest request, AccountVO dto) {
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
        saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    public HttpResult<Account> getAccount(HttpServletRequest request, Integer accountId) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = getById(accountId);
        if (account == null) {
            return HttpResult.error("账号不存在");
        }

        return HttpResult.success(account);
    }

    public HttpResult<List<LabelDTO>> emailOptions(HttpServletRequest request, Integer type) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String s = jwtTokenUtil.extractUsername(token);
        Share byUserName = shareService.getByUserName(s);
        List<LabelDTO> emails = list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType, type))
                                        .stream()
                                        .filter(e -> e.getUserId().equals(byUserName.getId()))
                                        .map(e -> new LabelDTO(e.getId().toString(), e.getName(), e.getName()))
                                        .sorted(Comparator.comparing(LabelDTO::getLabel))
                                        .collect(Collectors.toList());
        List<LabelDTO> res = new ArrayList<>();
        LabelDTO labelDTO = new LabelDTO(type.equals(1) ? "-1" : type.equals(2)?"-2":"-3", "----默认选项：下车----", "----默认选项：下车----");
        res.add(labelDTO);
        res.addAll(emails);
        return HttpResult.success(res);
    }
}
