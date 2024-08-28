package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.dao.RedemptionMapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RedemptionService extends ServiceImpl<RedemptionMapper, Redemption> implements IService<Redemption> {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;

    public HttpResult<List<RedemptionVO>> listRedemptions(HttpServletRequest request, String email) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        List<Redemption> list = list(new LambdaQueryWrapper<Redemption>().eq(Redemption::getUserId, user.getId()));
        List<RedemptionVO> redemptionVOS = ConvertUtil.convertList(list, RedemptionVO.class);
        Map<Integer, Account> accountMap = accountService.list().stream().collect(Collectors.toMap(Account::getId, Function.identity()));
        redemptionVOS.stream().forEach(red -> {
            Account account = accountMap.get(red.getAccountId());
            if (account == null) {
                red.setEmail("");
            }else {
                red.setEmail(account.getEmail());
                String accType;
                switch (account.getAccountType()){
                    case 1:
                        accType = "ChatGPT";
                        break;
                    case 2:
                        accType = "Claude";
                        break;
                    default:
                        accType = "ChatGPT";
                }
                red.setAccountType(accType);
            }
        });
        redemptionVOS = redemptionVOS.stream().filter(e->StringUtils.hasText(e.getEmail()) && (!StringUtils.hasText(email)||(StringUtils.hasText(email) && e.getEmail().contains(email)))).collect(Collectors.toList());
        return HttpResult.success(redemptionVOS);
    }

    public HttpResult<Boolean> activate(HttpServletRequest request, String code) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Redemption one = getOne(new LambdaQueryWrapper<Redemption>().eq(Redemption::getCode, code));
        if (one == null) {
            return HttpResult.error("兑换码不存在");
        }
        if (StringUtils.hasText(one.getTargetUserName()) && !one.getTargetUserName().equals(username)) {
            return HttpResult.error("您无法使用此兑换码");
        }
        ShareVO shareVO = new ShareVO();
        shareVO.setId(user.getId());
        shareVO.setAccountId(one.getAccountId());
        shareVO.setDuration(one.getDuration().equals(-1) ? null : one.getDuration());
        HttpResult<Boolean> distribute = shareService.distribute(shareVO);
        if (distribute.isStatus()){
            removeById(one.getId());
            return distribute;
        }
        return distribute;
    }

    public HttpResult<Boolean> activate(Integer shareId, String code) {
        Redemption one = getOne(new LambdaQueryWrapper<Redemption>().eq(Redemption::getCode, code));
        if (one == null) {
            return HttpResult.error("兑换码不存在");
        }
        ShareVO shareVO = new ShareVO();
        shareVO.setId(shareId);
        shareVO.setAccountId(one.getAccountId());
        shareVO.setDuration(one.getDuration().equals(-1) ? null : one.getDuration());
        HttpResult<Boolean> distribute = shareService.distribute(shareVO);
        if (distribute.isStatus()){
            removeById(one.getId());
            return distribute;
        }
        return distribute;
    }

}
