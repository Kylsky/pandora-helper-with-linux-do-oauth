package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareController {
    @Autowired
    private ShareService shareService;

    @GetMapping("/getById")
    public HttpResult<Share> getById(HttpServletRequest request, @RequestParam Integer id){
        return shareService.getShareById(request,id);
    }

    @GetMapping("/list")
    public HttpResult<PageVO<ShareVO>> list(HttpServletRequest request,
                                          @RequestParam(required = false) String emailAddr,
                                          @RequestParam(required = false) Integer accountType,
                                          @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return shareService.listShares(request,emailAddr,accountType,page,size);
    }

    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(HttpServletRequest request, @RequestParam Integer id) {
        return shareService.deleteShare(request,id);
    }


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request, @RequestBody ShareVO dto) {
        return shareService.addShare(request,dto);
    }

    @PatchMapping("/update")
    public HttpResult<Boolean> update(HttpServletRequest request, @RequestBody Share dto) {
        return shareService.updateShare(request,dto);
    }

    @PostMapping("/distribute")
    public HttpResult<Boolean> distribute(HttpServletRequest request, @RequestBody ShareVO share) {
        return shareService.distributeShare(request,share);
    }

    @GetMapping("/checkUser")
    public HttpResult<String> checkLinuxDoUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
        return shareService.checkLinuxDoUser(username,jmc,request);
    }

    @GetMapping("/updateParent")
    public HttpResult<Boolean> updateParent(@RequestParam Integer shareId, HttpServletRequest request) {
        return shareService.updateParent(shareId,request);
    }

    @GetMapping("/getGptShare")
    public HttpResult<String> getGptShare(@RequestParam Integer gptConfigId) {
        return shareService.getGptShare(gptConfigId);
    }

    @GetMapping("/getClaudeShare")
    public HttpResult<String> getClaudeShare(@RequestParam Integer claudeConfigId) {
        return shareService.getClaudeShare(claudeConfigId);
    }

    @GetMapping("/getApiShare")
    public HttpResult<String> getApiShare(@RequestParam Integer apiConfigId) {
        return shareService.getApiShare(apiConfigId);
    }
}
