package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.xpath.internal.operations.Bool;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.CarService;
import fun.yeelo.oauth.service.RedemptionService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/redemption")
@Slf4j
public class RedemptionController {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CarService carService;
    @Autowired
    private RedemptionService redemptionService;

    @GetMapping("/list")
    public HttpResult<List<RedemptionVO>> list(HttpServletRequest request,@RequestParam(required = false) String emailAddr) {
        return redemptionService.listRedemptions(request,emailAddr);
    }

    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(HttpServletRequest request, @RequestParam Integer id) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Redemption redemption = redemptionService.getById(id);
        if (redemption!=null && redemption.getUserId().equals(user.getId())) {
            accountService.delete(id);
        }else {
            return HttpResult.error("您无权删除该兑换码");
        }

        return HttpResult.success(true);
    }


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request,@RequestBody RedemptionVO dto) {
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
        for (int i = 0; i < dto.getCount(); i++) {
            dto.setUserId(user.getId());
            dto.setCreateTime(LocalDateTime.now());
            dto.setCode(UUID.randomUUID().toString().replace("-","").substring(0,10));
            redemptionService.saveOrUpdate(dto);
        }

        return HttpResult.success(true);
    }

    @PatchMapping("/update")
    public HttpResult<Boolean> update(HttpServletRequest request,@RequestBody Redemption dto) {
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
            redemptionService.updateById(updatePO);
        }

        return HttpResult.success(true);
    }

    @GetMapping("/activate")
    public HttpResult<Boolean> emailOptions(HttpServletRequest request, @RequestParam String code) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Redemption one = redemptionService.getOne(new LambdaQueryWrapper<Redemption>().eq(Redemption::getCode, code));
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
        return shareService.distribute(shareVO);
    }

}
