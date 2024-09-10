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
    public HttpResult<PageVO<AccountVO>> list(HttpServletRequest request, @RequestParam(required = false) String owner,@RequestParam Integer page,@RequestParam Integer size) {
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
        List<Account> accountList = new ArrayList<>(accountService.list(new LambdaQueryWrapper<Account>().eq(Account::getShared, true)));
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        accountVOS.forEach(e -> {
            Share targetUser = userMap.get(e.getUserId());
            e.setType(e.getAccountType().equals(1) ? "ChatGPT" : "Claude");
            String levelDesc = userMap.get(e.getUserId()).getTrustLevel() == null
                                       ? ""
                                       : " ( Lv."+userMap.get(e.getUserId() ).getTrustLevel()+" )";
            e.setUsername(targetUser.getUniqueName());
            e.setUsernameDesc(userMap.get(e.getUserId()).getUniqueName() + levelDesc);
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
            e.setCountDesc(count+" / "+(e.getUserLimit().equals(-1)? "无限制":e.getUserLimit()));
            e.setCount(count);
        });
        Map<Integer, List<CarApply>> applys = carService.list().stream().collect(Collectors.groupingBy(e -> e.getAccountId()));
        accountVOS = accountVOS.stream()
                             .filter(e-> !StringUtils.hasText(owner) || e.getUsername().contains(owner))
                             .sorted(Comparator.comparing(AccountVO::getType))
                             .collect(Collectors.toList());
        accountVOS.forEach(e->{
            Share targetUser = userMap.get(e.getUserId());
            e.setApplyNum(applys.get(e.getId())==null?0:applys.get(e.getId()).size());
            e.setAuthorized(targetUser.getId().equals(user.getId()) && e.getApplyNum()>0);
        });
        PageVO pageVO = new PageVO();
        pageVO.setTotal(accountVOS.size());
        pageVO.setData(pageVO==null ? accountVOS : accountVOS.subList(10*(page-1),Math.min(10*(page-1)+size,accountVOS.size())));
        return HttpResult.success(pageVO);
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
                                          return new LabelDTO(e.getShareId() + "", userMap.get(e.getShareId()).getUniqueName() + "",userMap.get(e.getShareId()).getUniqueName() + "");
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
        Account account = accountService.getById(dto.getAccountId());
        Integer accountType = account.getAccountType();
        Integer curAccountUser = 0;
        switch (accountType) {
            case 1:
                curAccountUser = gptConfigService.count(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getAccountId, account.getId()));
                List<ShareGptConfig> list = gptConfigService.list(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, dto.getShareId()).eq(ShareGptConfig::getAccountId, dto.getAccountId()));
                if (!CollectionUtils.isEmpty(list)) {
                    return HttpResult.error("您已在该车上，请勿重复申请");
                }
                break;
            case 2:
                curAccountUser = claudeConfigService.count(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getAccountId, account.getId()));
                List<ShareClaudeConfig> cladueList = claudeConfigService.list(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, dto.getShareId()).eq(ShareClaudeConfig::getAccountId, dto.getAccountId()));
                if (!CollectionUtils.isEmpty(cladueList)) {
                    return HttpResult.error("您已该在车上，请勿重复申请");
                }
                break;
        }

        if (!account.getUserLimit().equals(-1) &&curAccountUser >= account.getUserLimit()) {
            return HttpResult.error("再上车就要超载了，试试其他车吧");
        }

        if (!account.getShared().equals(1)) {
            return HttpResult.error("该车已经开走了，请停止你的申请");
        }

        List<CarApply> applies = carService.list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getShareId, dto.getShareId()).eq(CarApply::getAccountId, dto.getShareId()));
        if (!CollectionUtils.isEmpty(applies)) {
            return HttpResult.error("您已申请上车，请勿重复申请");
        }
        List<CarApply> myApplies = carService.list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getShareId, dto.getShareId()));
        if (!CollectionUtils.isEmpty(myApplies) && myApplies.size() > 5) {
            return HttpResult.error("您已经申请了5辆车，请给他人一点机会");
        }
        if (applies.size() > 10) {
            return HttpResult.error("该车申请人数过多，请考虑申请其他车辆");
        }

        if (account.getAuto().equals(1)) {
            CarApplyVO carApplyVO = new CarApplyVO();
            carApplyVO.setAllowApply(1);
            carApplyVO.setShareId(user.getId());
            carApplyVO.setAccountId(account.getId());
            carService.audit(request,carApplyVO);
            return HttpResult.success();
        }

        CarApply apply = new CarApply();
        apply.setAccountId(dto.getAccountId());
        apply.setShareId(dto.getShareId());
        carService.save(apply);
        return HttpResult.success();
    }

    @PostMapping("/audit")
    public HttpResult<Boolean> refresh(HttpServletRequest request, @RequestBody CarApplyVO dto) {
        return carService.audit(request,dto);
    }


}
