package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.dao.CarMapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CarService extends ServiceImpl<CarMapper, CarApply> implements IService<CarApply> {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    @Autowired
    private GptConfigService gptConfigService;
    @Autowired
    private ClaudeConfigService claudeConfigService;
    @Autowired
    private ApiConfigService apiConfigService;
    @Autowired
    private AccountService accountService;

    public HttpResult<Boolean> audit(HttpServletRequest request, CarApplyVO dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (dto.getIds() == null && dto.getShareId() != null) {
            dto.setIds(Collections.singletonList(dto.getShareId()));
        }
        if (!dto.getAllowApply().equals(1)) {
            dto.getIds().forEach(id -> {
                this.remove(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId, dto.getAccountId()).eq(CarApply::getShareId, id));
            });
            return HttpResult.success();
        }

        dto.getIds().forEach(id -> {
            ShareVO shareVO = new ShareVO();
            shareVO.setId(id);
            shareVO.setAccountId(dto.getAccountId());
            this.remove(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId, dto.getAccountId()).eq(CarApply::getShareId, id));
            shareService.distribute(shareVO);
        });
        return HttpResult.success();
    }

    public HttpResult<PageVO<AccountVO>> listCars(HttpServletRequest request, String owner, Integer page, Integer size) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        Map<Integer, Share> userMap = shareService.list().stream().collect(Collectors.toMap(Share::getId, Function.identity()));
        Map<Integer, List<ShareGptConfig>> gptMap = gptConfigService.list().stream().collect(Collectors.groupingBy(ShareGptConfig::getAccountId));
        Map<Integer, List<ShareClaudeConfig>> claudeMap = claudeConfigService.list().stream().collect(Collectors.groupingBy(ShareClaudeConfig::getAccountId));
        Map<Integer, List<ShareApiConfig>> apiMap = apiConfigService.list().stream().collect(Collectors.groupingBy(ShareApiConfig::getAccountId));
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        List<Account> accountList = new ArrayList<>(accountService.list(new LambdaQueryWrapper<Account>().eq(Account::getShared, true)));
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        accountVOS.forEach(e -> {
            Share targetUser = userMap.get(e.getUserId());
            e.setType(e.getAccountType().equals(1) ? "ChatGPT" : e.getAccountType().equals(2) ? "Claude" : "API");
            String levelDesc = userMap.get(e.getUserId()).getTrustLevel() == null
                                       ? ""
                                       : " ( Lv." + userMap.get(e.getUserId()).getTrustLevel() + " )";
            e.setUsername(targetUser.getUniqueName());
            e.setUsernameDesc(userMap.get(e.getUserId()).getUniqueName() + levelDesc);
            e.setEmail(e.getName());

            Integer count;

            switch (e.getAccountType()) {
                case 1:
                    count = gptMap.getOrDefault(e.getId(), new ArrayList<>()).size();
                    break;
                case 2:
                    count = claudeMap.getOrDefault(e.getId(), new ArrayList<>()).size();
                    break;
                case 3:
                    count = apiMap.getOrDefault(e.getId(), new ArrayList<>()).size();
                    break;
                default:
                    count = 0;
            }
            e.setCountDesc(count + " / " + (e.getUserLimit().equals(-1) ? "无限制" : e.getUserLimit()));
            e.setCount(count);
        });
        Map<Integer, List<CarApply>> applys = list().stream().collect(Collectors.groupingBy(e -> e.getAccountId()));
        accountVOS = accountVOS.stream()
                             .filter(e -> !StringUtils.hasText(owner) || e.getUsername().contains(owner))
                             .sorted(Comparator.comparing(AccountVO::getType))
                             .collect(Collectors.toList());
        accountVOS.forEach(e -> {
            Share targetUser = userMap.get(e.getUserId());
            e.setApplyNum(applys.get(e.getId()) == null ? 0 : applys.get(e.getId()).size());
            e.setAuthorized(targetUser.getId().equals(user.getId()) && e.getApplyNum() > 0);
        });
        PageVO pageVO = new PageVO();
        pageVO.setTotal(accountVOS.size());
        pageVO.setData(pageVO == null ? accountVOS : accountVOS.subList(10 * (page - 1), Math.min(10 * (page - 1) + size, accountVOS.size())));
        return HttpResult.success(pageVO);
    }

    public HttpResult<List<LabelDTO>> fetchApplies(HttpServletRequest request, Integer accountId) {
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
        List<LabelDTO> labels = list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId, accountId))
                                        .stream()
                                        .map(e -> {
                                            return new LabelDTO(e.getShareId() + "", userMap.get(e.getShareId()).getUniqueName() + "", userMap.get(e.getShareId()).getUniqueName() + "");
                                        })
                                        .collect(Collectors.toList());


        return HttpResult.success(labels);
    }

    public HttpResult<Boolean> carApply(HttpServletRequest request, CarApply dto) {
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
            case 3:
                curAccountUser = apiConfigService.count(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getAccountId, account.getId()));
                List<ShareApiConfig> apiList = apiConfigService.list(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId, dto.getShareId()).eq(ShareApiConfig::getAccountId, dto.getAccountId()));
                if (!CollectionUtils.isEmpty(apiList)) {
                    return HttpResult.error("您已该在车上，请勿重复申请");
                }
                break;
        }

        if (!account.getUserLimit().equals(-1) && curAccountUser >= account.getUserLimit()) {
            return HttpResult.error("再上车就要超载了，试试其他车吧");
        }

        if (!account.getShared().equals(1)) {
            return HttpResult.error("该车已经开走了，请停止你的申请");
        }

        List<CarApply> applies = list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getShareId, dto.getShareId()).eq(CarApply::getAccountId, dto.getShareId()));
        if (!CollectionUtils.isEmpty(applies)) {
            return HttpResult.error("您已申请上车，请勿重复申请");
        }
        List<CarApply> myApplies = list(new LambdaQueryWrapper<CarApply>().eq(CarApply::getShareId, dto.getShareId()));
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
            audit(request, carApplyVO);
            return HttpResult.success();
        }

        CarApply apply = new CarApply();
        apply.setAccountId(dto.getAccountId());
        apply.setShareId(dto.getShareId());
        save(apply);
        return HttpResult.success();
    }
}

