package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.annotation.RequireLogin;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.account.AccountVO;
import fun.yeelo.oauth.domain.car.CarApply;
import fun.yeelo.oauth.domain.car.CarApplyVO;
import fun.yeelo.oauth.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/car")
@Slf4j
public class CarController {
    @Autowired
    private CarService carService;

    @RequireLogin
    @GetMapping("/list")
    public HttpResult<PageVO<AccountVO>> list(@RequestParam(required = false) String owner, @RequestParam Integer page, @RequestParam Integer size) {
        return carService.listCars(owner,page,size);
    }

    @RequireLogin
    @GetMapping("/fetchApplies")
    public HttpResult<List<LabelDTO>> fetchApplies(@RequestParam Integer accountId) {
        return carService.fetchApplies(accountId);
    }

    @RequireLogin
    @PostMapping("/apply")
    public HttpResult<Boolean> carApply(@RequestBody CarApply dto) {
        return carService.carApply(dto);
    }

    @RequireLogin
    @PostMapping("/audit")
    public HttpResult<Boolean> refresh(@RequestBody CarApplyVO dto) {
        return carService.audit(dto);
    }


}
