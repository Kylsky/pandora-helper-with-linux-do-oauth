package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.config.MirrorConfig;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.account.AccountVO;
import fun.yeelo.oauth.domain.car.CarApply;
import fun.yeelo.oauth.domain.share.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.OpenAIUtil;
import fun.yeelo.oauth.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

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
    private GrokConfigService grokConfigService;

    @Autowired
    private MirrorConfig mirrorConfig;
    @Autowired
    private OpenAIUtil openAIUtil;

    public List<Account> findAll() {
        return accountMapper.selectList(null);
    }

    public HttpResult<String> share(Integer id) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

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

    public HttpResult<List<InfoVO>> statistic(Integer id) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        Account byId = getById(id);
        List<ShareGptConfig> gptShares = gptConfigService.list().stream().filter(e -> e.getAccountId().equals(id)).collect(Collectors.toList());
        //String chatUrl = "https://chat.oaifree.com/token/info/";
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

    public HttpResult<PageVO<AccountVO>> listAccount(String emailAddr, Integer page, Integer size, Integer type) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        List<Account> accountList = type != null ? list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType, type)) : findByUserId(user.getId());
        if (StringUtils.hasText(emailAddr)) {
            accountList = accountList.stream().filter(e -> e.getEmail().contains(emailAddr)).collect(Collectors.toList());
        }
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        Map<Integer, List<CarApply>> accountIdMap = carService.list().stream().collect(Collectors.groupingBy(CarApply::getAccountId));
        accountVOS.forEach(e -> {
            //e.setEmail("车辆"+(num.getAndIncrement()));
            switch (e.getAccountType()) {
                case 1 -> e.setType("ChatGPT");
                case 2 -> e.setType("Claude");
                case 3 -> e.setType("API");
                case 4 -> e.setType("Grok");
            }
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

    public HttpResult<Boolean> deleteAccount(Integer id) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        Account account = findById(id);
        if (account != null && account.getUserId().equals(user.getId())) {
            delete(id);
            Integer accountType = account.getAccountType();
            switch (accountType) {
                case 1:
                    gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getAccountId, account.getId()));
                    break;
                case 2:
                    claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getAccountId, id));
                    break;
                case 3:
                    apiConfigService.remove(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getAccountId, id));
                    break;
                case 4:
                    grokConfigService.remove(new LambdaQueryWrapper<ShareGrokConfig>().eq(ShareGrokConfig::getAccountId, id));
                    break;
            }
        } else {
            return HttpResult.error("您无权删除该账号");
        }

        return HttpResult.success(true);
    }

    public HttpResult<Account> getAccountById(Integer id) {
        Account byId = getById(id);
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        if (!byId.getUserId().equals(user.getId()) && user.getId() != 1) {
            return HttpResult.error("你无权访问该账号");
        }
        return HttpResult.success(byId);
    }

    public HttpResult<Boolean> saveOrUpdateAccount(Account dto) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

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
                case 1:
                    gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getAccountId, account.getId()));
                    break;
                case 2:
                    claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getAccountId, account.getId()));
                    break;
                case 3:
                    apiConfigService.remove(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getAccountId, account.getId()));
                    break;
                case 4:
                    grokConfigService.remove(new LambdaQueryWrapper<ShareGrokConfig>().eq(ShareGrokConfig::getAccountId, account.getId()));
                    break;
            }
        }
        if (dto.getAccountType().equals(1) && StringUtils.hasText(dto.getAccessToken())) {
            CompletableFuture.runAsync(() -> openAIUtil.checkAccount(dto.getAccessToken(), dto.getEmail(), dto.getId()));
        }
        if (dto.getAccountType().equals(4) && StringUtils.hasText(dto.getAccessToken())) {
            String md5 = mirrorConfig.getGrokMirrorMd5(dto.getAccessToken());
            dto.setRefreshToken(md5);
        }
        saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    public HttpResult<Boolean> refresh(Integer id) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        Account account = getById(id);
        if (account == null) {
            return HttpResult.error("账号不存在");
        }
        if (!StringUtils.hasText(account.getRefreshToken())) {
            return HttpResult.error("账号未配置refreshToken");
        }

        try {
            Integer accountId = account.getId();

            Boolean result = openAIUtil.refresh(accountId, account.getRefreshToken(), account.getEmail());
            if (!result) {
                return HttpResult.error("刷新 access_token异常 , 请检查 refresh_token 是否有效");
            }
        } catch (Exception e) {
            log.error("刷新access_token异常,异常账号:{}", account.getEmail(), e);
            return HttpResult.error("刷新 access_token异常 , 请检查 refresh_token 是否有效");
        }


        return HttpResult.success(true);
    }

    public HttpResult<Boolean> addAccount(AccountVO dto) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        dto.setUserId(user.getId());
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());

        if (dto.getAccountType().equals(1) && StringUtils.hasText(dto.getAccessToken())) {
            CompletableFuture.runAsync(() -> openAIUtil.checkAccount(dto.getAccessToken(), dto.getEmail(), dto.getId()));
        }

        if (dto.getAccountType().equals(4) && StringUtils.hasText(dto.getAccessToken())) {
            String md5 = mirrorConfig.getGrokMirrorMd5(dto.getAccessToken());
            dto.setRefreshToken(md5);
        }

        saveOrUpdate(dto);
        return HttpResult.success(true);
    }

    public HttpResult<Account> getAccount(Integer accountId) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        Account account = getById(accountId);
        if (account == null) {
            return HttpResult.error("账号不存在");
        }

        return HttpResult.success(account);
    }

    public HttpResult<List<LabelDTO>> emailOptions(Integer type) {
        // 使用 UserContextUtil 获取当前用户
        Share user = UserContextUtil.getCurrentUser();

        List<LabelDTO> emails = list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType, type))
                                        .stream()
                                        .filter(e -> e.getUserId().equals(user.getId()))
                                        .map(e -> new LabelDTO(e.getId().toString(), e.getName(), e.getName()))
                                        .sorted(Comparator.comparing(LabelDTO::getLabel))
                                        .toList();
        List<LabelDTO> res = new ArrayList<>();
        LabelDTO labelDTO;
        switch (type) {
            case 2 -> labelDTO = new LabelDTO("-2", "----默认选项：下车----", "----默认选项：下车----");
            case 3 -> labelDTO = new LabelDTO("-3", "----默认选项：下车----", "----默认选项：下车----");
            case 4 -> labelDTO = new LabelDTO("-4", "----默认选项：下车----", "----默认选项：下车----");
            default -> labelDTO = new LabelDTO("-1", "----默认选项：下车----", "----默认选项：下车----");
        }
        res.add(labelDTO);
        res.addAll(emails);
        return HttpResult.success(res);
    }
}
