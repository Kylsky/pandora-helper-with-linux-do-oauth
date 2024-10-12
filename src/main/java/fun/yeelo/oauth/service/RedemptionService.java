package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.RedemptionMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    public HttpResult<PageVO<RedemptionVO>> listRedemptions(HttpServletRequest request, String email, Integer page, Integer size) {
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
                    case 3:
                        accType = "API";
                        break;
                    default:
                        accType = "ChatGPT";
                }
                red.setAccountType(accType);
            }
        });
        redemptionVOS = redemptionVOS.stream().filter(e->StringUtils.hasText(e.getEmail()) && (!StringUtils.hasText(email)||(StringUtils.hasText(email) && e.getEmail().contains(email)))).collect(Collectors.toList());
        PageVO<RedemptionVO> pageVO = new PageVO<>();
        pageVO.setData(page==null ? redemptionVOS : redemptionVOS.subList(10*(page-1),Math.min(10*(page-1)+size,redemptionVOS.size())));
        pageVO.setTotal(redemptionVOS.size());
        return HttpResult.success(pageVO);
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
        shareVO.setDuration(one.getDuration().equals(-1) || user.getId().equals(1) ? null : one.getDuration());
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

    public HttpResult<Redemption> getRedemptionById(HttpServletRequest request, Integer id) {
        Redemption byId = getById(id);
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (!byId.getUserId().equals(user.getId())) {
            return HttpResult.error("你无权访问该内容");
        }
        return HttpResult.success(byId);
    }

    public HttpResult<Boolean> deleteRedemption(HttpServletRequest request, Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Redemption redemption = getById(id);
        if (redemption!=null && redemption.getUserId().equals(user.getId())) {
            removeById(id);
        }else {
            return HttpResult.error("您无权删除该兑换码");
        }

        return HttpResult.success(true);
    }

    public HttpResult<Boolean> addRedemption(HttpServletRequest request, RedemptionVO dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (dto.getCount()==null || dto.getCount()<=0){
            dto.setCount(1);
        }
        if (dto.getAccountId()==null){
            return HttpResult.error("尚未选择账号，请重试");
        }
        if (dto.getCount() > 4) {
            return HttpResult.error("最多支持一次性生成4个兑换码");
        }
        if (dto.getDuration() > 30){
            return HttpResult.error("最多支持30天");
        }
        for (int i = 0; i < dto.getCount(); i++) {
            dto.setId(null);
            dto.setUserId(user.getId());
            dto.setCreateTime(LocalDateTime.now());
            dto.setCode(UUID.randomUUID().toString().replace("-","").substring(0,10));
            save(dto);
        }

        return HttpResult.success(true);
    }

    public HttpResult<Boolean> updateRedemption(HttpServletRequest request, Redemption dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Redemption updatePO = new Redemption();
        if (!StringUtils.hasText(dto.getTargetUserName())) {
            updatePO.setTargetUserName(dto.getTargetUserName());
        }
        updatePO.setDuration(dto.getDuration());
        updatePO.setTimeUnit(dto.getTimeUnit());
        updatePO.setId(dto.getId());
        if (updatePO.getId()!=null){
            updateById(updatePO);
        }

        return HttpResult.success(true);
    }
}
