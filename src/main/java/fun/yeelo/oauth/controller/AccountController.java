package fun.yeelo.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.account.AccountVO;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/share")
    public HttpResult<String> share(HttpServletRequest request,
                                    @RequestParam(required = false) Integer id) {
        return accountService.share(request,id);
    }

    @GetMapping("/statistic")
    public HttpResult<List<InfoVO>> statistic(HttpServletRequest request, Integer id) {
        return accountService.statistic(request,id);
    }

    @GetMapping("/list")
    public HttpResult<PageVO<AccountVO>> list(HttpServletRequest request,
                                              @RequestParam(required = false) String emailAddr,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) Integer type) {
        return accountService.listAccount(request,emailAddr,page,size,type);
    }

    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(HttpServletRequest request, @RequestParam Integer id) {
        return accountService.deleteAccount(request,id);
    }

    @GetMapping("/getById")
    public HttpResult<Account> getById(HttpServletRequest request, @RequestParam Integer id) {
        return accountService.getAccountById(request,id);
    }


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request, @RequestBody AccountVO dto) {
        return accountService.addAccount(request,dto);
    }

    @PatchMapping("/update")
    public HttpResult<Boolean> update(HttpServletRequest request, @RequestBody Account dto) {
        return accountService.saveOrUpdateAccount(request,dto);
    }

    @PostMapping("/refresh")
    public HttpResult<Boolean> refresh(HttpServletRequest request, @RequestParam Integer id) {
        return accountService.refresh(request,id);
    }

    @GetMapping("/getAccount")
    public HttpResult<Account> getAccount(HttpServletRequest request, @RequestParam Integer accountId) {
        return accountService.getAccount(request,accountId);
    }


    @GetMapping("/options")
    public HttpResult<List<LabelDTO>> emailOptions(HttpServletRequest request,
                                                   @RequestParam Integer type) {
        return accountService.emailOptions(request,type);
    }

}
