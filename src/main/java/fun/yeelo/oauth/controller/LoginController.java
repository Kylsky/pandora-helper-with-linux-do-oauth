package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.redemption.Redemption;
import fun.yeelo.oauth.domain.share.ResetDTO;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.MidjourneyService;
import fun.yeelo.oauth.service.RedemptionService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.timer.UpdateTimer;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MidjourneyService midjourneyService;

    @Value("${midjourney.key}")
    private String mjKey;

    @Autowired
    private UpdateTimer updateTimer;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;
    @Value("${admin-name:admin}")
    private String adminName;
    @Autowired
    private RedemptionService redemptionService;

    @PostConstruct
    public void initiate() {
        List<Share> list = shareService.list();
        if (CollectionUtils.isEmpty(list)) {
            Share user = new Share();
            user.setId(1);
            user.setUniqueName(adminName);
            user.setParentId(1);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setComment("admin");
            shareService.save(user);
            log.info("初始化成功");
        }
    }

    @PostMapping("/login")
    public HttpResult<ShareVO> panelLogin(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return HttpResult.error("用户名或密码不能为空", HttpStatus.BAD_REQUEST);
        }
        Share user = shareService.getByUserName(username);
        List<Redemption> list = redemptionService.list(new LambdaQueryWrapper<Redemption>().eq(Redemption::getCode, password));
        if (!CollectionUtils.isEmpty(list)) {
            ShareVO userToAdd = new ShareVO();
            userToAdd.setUniqueName(username);
            userToAdd.setTrustLevel(-1);
            userToAdd.setPassword(passwordEncoder.encode("123456"));
            userToAdd.setComment("");
            Share share = ConvertUtil.convert(userToAdd, Share.class);
            shareService.save(share);

            redemptionService.activate(share.getId(), password);
            password = "123456";
            user = share;
            midjourneyService.addUser(share, "DISABLED");
        }
        if (user == null) {
            return HttpResult.error("用户不存在，请重试");
        }
        //ShareGptConfig gptConfig = gptConfigService.getByShareId(user.getId());
        //if (gptConfig == null || !StringUtils.hasText(gptConfig.getShareToken())) {
        //    return HttpResult.error("用户未激活");
        //}
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return HttpResult.error("密码错误,请重试");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.getUsername());
        ShareVO shareVO = new ShareVO();
        shareVO.setAvatarUrl(user.getAvatarUrl());
        shareVO.setUsername(user.getUniqueName());
        shareVO.setJwt(jwtTokenUtil.generateToken(userDetails));
        shareVO.setTrustLevel(user.getTrustLevel());

        return HttpResult.success(shareVO);
    }

    @GetMapping("/info")
    public HttpResult<ShareVO> userInfo(HttpServletRequest request) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {

            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        ShareVO shareVO = new ShareVO();
        // 根据user计算hash值
        shareVO.setApiKey(user.getId() + "+" + user.getUniqueName() + "+" + user.getPassword().substring(0, 10));
        shareVO.setAvatarUrl(user.getAvatarUrl());
        shareVO.setUsername(user.getUniqueName());
        shareVO.setTrustLevel(user.getTrustLevel());
        shareVO.setId(user.getId());
        shareVO.setMjProxyUrl(user.getMjProxyUrl());
        return HttpResult.success(shareVO);
    }


    @GetMapping("/checkToken")
    public HttpResult<Boolean> checkToken(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader("Authorization");

        String username;
        String jwt;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtTokenUtil.extractUsername(jwt);
        } else {
            return HttpResult.success(false, "用户未登录");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() != null) {
            return HttpResult.success(true);
        } else {
            return HttpResult.success(false, "登录状态已失效，请重新登录");
        }
    }

    @GetMapping("/refreshAll")
    public HttpResult<Boolean> refreshAll(@RequestParam String password) {
        Share admin = shareService.findById(1);
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            return HttpResult.error("密码错误");
        }
        updateTimer.refreshAccessToken();
        return HttpResult.success();
    }

    @PostMapping("/reset")
    public HttpResult<String> reset(@RequestBody ResetDTO resetDTO, HttpServletRequest request) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }

        String password = resetDTO.getOldPassword();
        String newPassword = resetDTO.getNewPassword();
        String confirmPassword = resetDTO.getConfirmPassword();
        if (!StringUtils.hasText(password)) {
            return HttpResult.error("旧密码为空");
        }
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            return HttpResult.error("新密码为空");
        }
        if (!newPassword.equals(confirmPassword)) {
            return HttpResult.error("两次密码不一致");
        }
        if (newPassword.length() < 8) {
            return HttpResult.error("密码长度必须超过大于等于8位，请重新输入。");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return HttpResult.error("密码错误，请重试");
        }
        Share update = new ShareVO();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        boolean res = shareService.updateById(update);
        midjourneyService.updateUser(update, null);
        return res ? HttpResult.success("重置成功") : HttpResult.error("重置失败");
    }

}
