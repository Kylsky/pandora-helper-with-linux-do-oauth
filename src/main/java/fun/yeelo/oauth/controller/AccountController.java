package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.CarService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REFRESH_URL = "https://token.oaifree.com/api/auth/refresh";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CarService carService;

    @GetMapping("/list")
    public HttpResult<List<AccountVO>> list(HttpServletRequest request,@RequestParam(required = false) String emailAddr) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        List<Account> accountList = accountService.findByUserId(user.getId());
        if (StringUtils.hasText(emailAddr)) {
            accountList = accountList.stream().filter(e->e.getEmail().contains(emailAddr)).collect(Collectors.toList());
        }
        List<AccountVO> accountVOS = ConvertUtil.convertList(accountList, AccountVO.class);
        Map<Integer, List<CarApply>> accountIdMap = carService.list().stream().collect(Collectors.groupingBy(CarApply::getAccountId));
        accountVOS.forEach(e->{
            //e.setEmail("车辆"+(num.getAndIncrement()));
            e.setType(e.getAccountType().equals(1)?"ChatGPT":"Claude");
            e.setCount(accountIdMap.getOrDefault(e.getId(),new ArrayList<>()).size());
        });
        accountVOS = accountVOS.stream().sorted(Comparator.comparing(AccountVO::getType)).collect(Collectors.toList());
        return HttpResult.success(accountVOS);
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
        Account account = accountService.findById(id);
        if (account!=null && account.getUserId().equals(user.getId())) {
            accountService.delete(id);
        }else {
            return HttpResult.error("您无权删除该账号");
        }

        return HttpResult.success(true);
    }


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request,@RequestBody AccountVO dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        dto.setUserId(user.getId());
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        accountService.saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    @PatchMapping("/update")
    public HttpResult<Boolean> update(HttpServletRequest request,@RequestBody Account dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (!StringUtils.hasText(dto.getName())) {
            dto.setName(dto.getEmail());
        }
        dto.setUpdateTime(LocalDateTime.now());
        dto.setUserId(user.getId());
        accountService.saveOrUpdate(dto);

        return HttpResult.success(true);
    }

    @PostMapping("/refresh")
    public HttpResult<Boolean> refresh(HttpServletRequest request,@RequestParam Integer accountId) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = accountService.getById(accountId);
        if (account==null) {
            return HttpResult.error("账号不存在");
        }
        if (!StringUtils.hasText(account.getRefreshToken())) {
            return HttpResult.error("账号未配置refreshToken");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
            personJsonObject.add("refresh_token", account.getRefreshToken());
            ResponseEntity<String> stringResponseEntity = restTemplate.exchange(REFRESH_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("access_token")) {
                log.info("refresh success");
                String newToken = map.get("access_token").toString();
                Account updateDTO = new Account();
                updateDTO.setId(accountId);
                updateDTO.setAccessToken(newToken);
                accountService.saveOrUpdate(updateDTO);
                return HttpResult.success(true);
            }
        } catch (Exception e) {
            log.error("Check user error:", e);
            return HttpResult.error("删除用户异常");
        }



        return HttpResult.success(true);
    }

    @GetMapping("/getAccount")
    public HttpResult<Account> getAccount(HttpServletRequest request,@RequestParam Integer accountId) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        Account account = accountService.getById(accountId);
        if (account==null) {
            return HttpResult.error("账号不存在");
        }

        return HttpResult.success(account);
    }


    @GetMapping("/options")
    public HttpResult<List<LabelDTO>> emailOptions(HttpServletRequest request, @RequestParam Integer type) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String s = jwtTokenUtil.extractUsername(token);
        Share byUserName = shareService.getByUserName(s);
        List<LabelDTO> emails = accountService.list(new LambdaQueryWrapper<Account>().eq(Account::getAccountType,type))
                                        .stream()
                                        .filter(e->e.getUserId().equals(byUserName.getId()))
                                        .map(e -> new LabelDTO(e.getId().toString(), e.getName()))
                                        .sorted(Comparator.comparing(LabelDTO::getLabel))
                                        .collect(Collectors.toList());
        return HttpResult.success(emails);
    }

}
