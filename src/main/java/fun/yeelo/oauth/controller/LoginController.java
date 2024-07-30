package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.LoginDTO;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.domain.ShareVO;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.security.SecureRandom;

@RestController
@RequestMapping("/user")
public class LoginController {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ShareService shareService;

    @PostMapping("/login")
    public HttpResult<String> panelLogin(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return HttpResult.error("用户名或密码不能为空", HttpStatus.BAD_REQUEST);
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请重试");
        }
        if (user.getAccountId() == null || user.getAccountId() == 99) {
            return HttpResult.error("用户未激活");
        }
        if (passwordEncoder.matches(user.getPassword(),password)) {
            return HttpResult.error("密码错误，请重试");
        }
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.getUsername());

        final String jwt = jwtTokenUtil.generateToken(userDetails);

        return HttpResult.success(jwt);
    }

    @GetMapping("/checkToken")
    public HttpResult<Boolean> checkToken(HttpServletRequest request){
        final String authorizationHeader = request.getHeader("Authorization");

        String username;
        String jwt;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtTokenUtil.extractUsername(jwt);
        }else {
            return HttpResult.success(false,"用户未登录");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() != null) {
            return HttpResult.success(true);
        } else {
            return HttpResult.success(false,"登录状态已失效，请重新登录");
        }
    }

}
