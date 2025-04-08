package fun.yeelo.oauth.controller;

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
    public HttpResult<Boolean> update(HttpServletRequest request, @RequestBody ShareVO dto) {
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
    public HttpResult<String> getGptShare(@RequestParam Integer gptConfigId, HttpServletRequest request) {
        return shareService.getGptShare(gptConfigId,request);
    }

    @GetMapping("/getClaudeShare")
    public HttpResult<String> getClaudeShare(@RequestParam Integer claudeConfigId) {
        return shareService.getClaudeShare(claudeConfigId);
    }

    @GetMapping("/getApiShare")
    public HttpResult<String> getApiShare(@RequestParam Integer apiConfigId) {
        return shareService.getApiShare(apiConfigId);
    }

    @GetMapping("/getGrokShare")
    public HttpResult<String> getGrokShare(@RequestParam Integer grokConfigId) {
        return shareService.getGrokShare(grokConfigId);
    }

    @GetMapping("/autoRenewal")
    public HttpResult<String> autoRenewal(@RequestParam String uniqueName,@RequestParam String code,@RequestParam Integer type) {
        return shareService.autoRenewal(uniqueName,code,type);
    }

    @PostMapping("/updateUserConfig")
    public HttpResult<String> updateUserConfig(@RequestBody UserConfigVO config, HttpServletRequest request) {
        return shareService.updateUserConfig(config, request);
    }


    @GetMapping("/getUserConfig")
    public HttpResult<UserConfigVO> getUserConfig(HttpServletRequest request) {
        return shareService.getUserConfig(request);
    }
}
