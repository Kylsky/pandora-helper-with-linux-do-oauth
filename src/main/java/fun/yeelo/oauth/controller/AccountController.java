package fun.yeelo.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.annotation.RequireLogin;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.account.AccountVO;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {
    @Autowired
    private AccountService accountService;

    @RequireLogin
    @GetMapping("/share")
    public HttpResult<String> share(@RequestParam(required = false) Integer id) {
        return accountService.share(id);
    }

    @RequireLogin
    @GetMapping("/statistic")
    public HttpResult<List<InfoVO>> statistic(Integer id) {
        return accountService.statistic(id);
    }

    @RequireLogin
    @GetMapping("/list")
    public HttpResult<PageVO<AccountVO>> list(@RequestParam(required = false) String emailAddr,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size,
                                          @RequestParam(required = false) Integer type) {
        return accountService.listAccount(emailAddr, page, size, type);
    }

    @RequireLogin
    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(@RequestParam Integer id) {
        return accountService.deleteAccount(id);
    }

    @RequireLogin
    @GetMapping("/getById")
    public HttpResult<Account> getById(@RequestParam Integer id) {
        return accountService.getAccountById(id);
    }

    @RequireLogin
    @PostMapping("/add")
    public HttpResult<Boolean> add(@RequestBody AccountVO dto) {
        return accountService.addAccount(dto);
    }

    @RequireLogin
    @PatchMapping("/update")
    public HttpResult<Boolean> update(@RequestBody Account dto) {
        return accountService.saveOrUpdateAccount(dto);
    }

    @RequireLogin
    @PostMapping("/refresh")
    public HttpResult<Boolean> refresh(@RequestParam Integer id) {
        return accountService.refresh(id);
    }

    @RequireLogin
    @GetMapping("/getAccount")
    public HttpResult<Account> getAccount(@RequestParam Integer accountId) {
        return accountService.getAccount(accountId);
    }

    @RequireLogin
    @GetMapping("/options")
    public HttpResult<List<LabelDTO>> emailOptions(@RequestParam Integer type) {
        return accountService.emailOptions(type);
    }
}
