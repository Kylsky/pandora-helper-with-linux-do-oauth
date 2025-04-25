package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.annotation.RequireLogin;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareController {
    @Autowired
    private ShareService shareService;

    @RequireLogin
    @GetMapping("/getById")
    public HttpResult<Share> getById(@RequestParam Integer id){
        return shareService.getShareById(id);
    }

    @RequireLogin
    @GetMapping("/list")
    public HttpResult<PageVO<ShareVO>> list(@RequestParam(required = false) String emailAddr,
                                            @RequestParam(required = false) Integer accountType,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return shareService.listShares(emailAddr, accountType, page, size);
    }

    @RequireLogin
    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(@RequestParam Integer id) {
        return shareService.deleteShare(id);
    }

    @RequireLogin
    @PostMapping("/add")
    public HttpResult<Boolean> add(@RequestBody ShareVO dto) {
        return shareService.addShare(dto);
    }

    @RequireLogin
    @PatchMapping("/update")
    public HttpResult<Boolean> update(@RequestBody ShareVO dto) {
        return shareService.updateShare(dto);
    }

    @RequireLogin
    @PostMapping("/distribute")
    public HttpResult<Boolean> distribute(@RequestBody ShareVO share) {
        return shareService.distributeShare(share);
    }

    @GetMapping("/checkUser")
    public HttpResult<String> checkLinuxDoUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
        return shareService.checkLinuxDoUser(username, jmc, request);
    }

    @RequireLogin
    @GetMapping("/updateParent")
    public HttpResult<Boolean> updateParent(@RequestParam Integer shareId) {
        return shareService.updateParent(shareId);
    }

    @RequireLogin
    @GetMapping("/getGptShare")
    public HttpResult<String> getGptShare(@RequestParam Integer gptConfigId) {
        return shareService.getGptShare(gptConfigId);
    }

    @RequireLogin
    @GetMapping("/getClaudeShare")
    public HttpResult<String> getClaudeShare(@RequestParam Integer claudeConfigId) {
        return shareService.getClaudeShare(claudeConfigId);
    }

    @RequireLogin
    @GetMapping("/getApiShare")
    public HttpResult<String> getApiShare(@RequestParam Integer apiConfigId) {
        return shareService.getApiShare(apiConfigId);
    }

    @RequireLogin
    @GetMapping("/getGrokShare")
    public HttpResult<String> getGrokShare(@RequestParam Integer grokConfigId) {
        return shareService.getGrokShare(grokConfigId);
    }

    @GetMapping("/autoRenewal")
    public HttpResult<String> autoRenewal(@RequestParam String uniqueName, @RequestParam String code, @RequestParam Integer type) {
        return shareService.autoRenewal(uniqueName, code, type);
    }

    @RequireLogin
    @PostMapping("/updateUserConfig")
    public HttpResult<String> updateUserConfig(@RequestBody UserConfigVO config) {
        return shareService.updateUserConfig(config);
    }

    @RequireLogin
    @GetMapping("/getUserConfig")
    public HttpResult<UserConfigVO> getUserConfig() {
        return shareService.getUserConfig();
    }
}
