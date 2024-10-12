package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.CarService;
import fun.yeelo.oauth.service.RedemptionService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

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

    @GetMapping("/getById")
    public HttpResult<Redemption> getById(HttpServletRequest request, @RequestParam Integer id){
        return redemptionService.getRedemptionById(request,id);
    }

    @GetMapping("/list")
    public HttpResult<PageVO<RedemptionVO>> list(HttpServletRequest request, @RequestParam(required = false) String emailAddr, @RequestParam Integer page, @RequestParam Integer size) {
        return redemptionService.listRedemptions(request,emailAddr,page,size);
    }

    @DeleteMapping("/delete")
    public HttpResult<Boolean> delete(HttpServletRequest request, @RequestParam Integer id) {
        return redemptionService.deleteRedemption(request,id);
    }


    @PostMapping("/add")
    public HttpResult<Boolean> add(HttpServletRequest request,@RequestBody RedemptionVO dto) {
        return redemptionService.addRedemption(request,dto);
    }

    @PatchMapping("/update")
    public HttpResult<Boolean> update(HttpServletRequest request,@RequestBody Redemption dto) {
        return redemptionService.updateRedemption(request,dto);
    }

    @GetMapping("/activate")
    public HttpResult<Boolean> activate(HttpServletRequest request, @RequestParam String code) {
        return redemptionService.activate(request,code);
    }

}
