package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.One;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/car")
@Slf4j
public class CarController {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CarService carService;
    @Autowired
    private GptConfigService gptConfigService;
    @Autowired
    private ClaudeConfigService claudeConfigService;

    @GetMapping("/list")
    public HttpResult<List<AccountVO>> list(HttpServletRequest request, @RequestParam(required = false) String owner) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        Map<Integer, Share> userMap = shareService.list().stream().collect(Collectors.toMap(Share::getId, Function.identity()));
        Map<Integer, List<ShareGptConfig>> gptMap = gptConfigService.list().stream().collect(Collectors.groupingBy(ShareGptConfig::getAccountId));
        Map<Integer, List<ShareClaudeConfig>> claudeMap = claudeConfigService.list().stream().collect(Collectors.groupingBy(ShareClaudeConfig::getAccountId));
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        List<Account> accountList = accountService.list(new LambdaQueryWrapper<Account>().eq(Account::getShared, true)).stream().filter(e -> !e.getUserId().equals(user.getId())).collect(Collectors.toList());
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        //AtomicInteger num = new AtomicInteger(1);
        accountVOS.forEach(e -> {
            //e.setEmail("车辆"+(num.getAndIncrement()));
            e.setType(e.getAccountType().equals(1) ? "ChatGPT" : "Claude");
            e.setUsername(userMap.get(e.getUserId()).getUniqueName());
            e.setEmail(e.getName());
            Integer count;

            switch (e.getAccountType()) {
                case 1:
                    count = gptMap.getOrDefault(e.getId(), new ArrayList<>()).size();
                    break;
                default:
                    count = claudeMap.getOrDefault(e.getId(), new ArrayList<>()).size();
                    break;
            }
            e.setCount(count);
        });
        accountVOS = accountVOS.stream()
                             .filter(e-> !StringUtils.hasText(owner) || e.getUsername().contains(owner))
                             .sorted(Comparator.comparing(AccountVO::getType))
                             .collect(Collectors.toList());
        return HttpResult.success(accountVOS);
    }

    @GetMapping("/fetchApplies")
    public HttpResult<List<LabelDTO>> fetchApplies(HttpServletRequest request,
                                                   @RequestParam Integer accountId) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Map<Integer, Share> userMap = shareService.list().stream().collect(Collectors.toMap(Share::getId, Function.identity()));
        List<LabelDTO> labels = carService.list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId, accountId))
                                      .stream()
                                      .map(e -> {
                                          return new LabelDTO(e.getShareId() + "", userMap.get(e.getShareId()).getUniqueName() + "");
                                      })
                                      .collect(Collectors.toList());


        return HttpResult.success(labels);
    }

    @PostMapping("/apply")
    public HttpResult<Boolean> carApply(HttpServletRequest request, @RequestBody CarApply dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        dto.setShareId(user.getId());
        Integer accountType = accountService.getById(dto.getAccountId()).getAccountType();
        switch (accountType) {
            case 1:
                List<ShareGptConfig> list = gptConfigService.list(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, dto.getShareId()).eq(ShareGptConfig::getAccountId, dto.getAccountId()));
                if (!CollectionUtils.isEmpty(list)) {
                    return HttpResult.error("您已在车上，请勿重复申请");
                }
            case 2:
                List<ShareClaudeConfig> cladueList = claudeConfigService.list(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, dto.getShareId()).eq(ShareClaudeConfig::getAccountId, dto.getAccountId()));
                if (!CollectionUtils.isEmpty(cladueList)) {
                    return HttpResult.error("您已在车上，请勿重复申请");
                }
                break;
        }

        List<CarApply> applies = carService.list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getShareId, dto.getShareId()).eq(CarApply::getAccountId, dto.getShareId()));
        if (!CollectionUtils.isEmpty(applies)) {
            return HttpResult.error("您已申请加入该车，请勿重复申请");
        }
        List<CarApply> myApplies = carService.list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getShareId, dto.getShareId()));
        if (!CollectionUtils.isEmpty(myApplies) && myApplies.size() > 5) {
            return HttpResult.error("您已经申请了5辆车，请给他人一点机会");
        }
        if (applies.size() > 10) {
            return HttpResult.error("该车申请人数过多，请考虑申请其他车辆");
        }
        CarApply apply = new CarApply();
        apply.setAccountId(dto.getAccountId());
        apply.setShareId(dto.getShareId());
        carService.save(apply);
        return HttpResult.success();
    }

    @PostMapping("/audit")
    public HttpResult<Boolean> refresh(HttpServletRequest request, @RequestBody CarApplyVO dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (!dto.getAllowApply().equals(1)) {
            carService.remove(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId,dto.getAccountId()).eq(CarApply::getShareId,dto.getShareId()));
            return HttpResult.success();
        }

        ShareVO shareVO = new ShareVO();
        shareVO.setId(dto.getShareId());
        shareVO.setAccountId(dto.getAccountId());
        carService.remove(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId,dto.getAccountId()).eq(CarApply::getShareId,dto.getShareId()));
        return shareService.distribute(shareVO);
    }


}
