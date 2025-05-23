package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.annotation.RequireLogin;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.redemption.Redemption;
import fun.yeelo.oauth.domain.redemption.RedemptionVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.CarService;
import fun.yeelo.oauth.service.RedemptionService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/redemption")
@Slf4j
public class RedemptionController {
    @Autowired
    private RedemptionService redemptionService;

    @RequireLogin
    @GetMapping("/getById")
    public HttpResult<Redemption> getById(@RequestParam Integer id){
        return redemptionService.getRedemptionById(id);
    }

    @RequireLogin
    @GetMapping("/list")
    public HttpResult<PageVO<RedemptionVO>> list(@RequestParam(required = false) String emailAddr, @RequestParam Integer page, @RequestParam Integer size) {
        return redemptionService.listRedemptions(emailAddr,page,size);
    }

    @RequireLogin
    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(@RequestParam Integer id) {
        return redemptionService.deleteRedemption(id);
    }

    @RequireLogin
    @PostMapping("/add")
    public HttpResult<Boolean> add(@RequestBody RedemptionVO dto) {
        return redemptionService.addRedemption(null, dto);
    }

    @RequireLogin
    @PatchMapping("/update")
    public HttpResult<Boolean> update(@RequestBody Redemption dto) {
        return redemptionService.updateRedemption(dto);
    }

    @RequireLogin
    @GetMapping("/activate")
    public HttpResult<Boolean> activate(@RequestParam String code) {
        return redemptionService.activate(code);
    }

}
