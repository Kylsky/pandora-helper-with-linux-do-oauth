package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountService extends ServiceImpl<AccountMapper, Account> implements IService<Account> {
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
}

// Similar implementation for ShareService

