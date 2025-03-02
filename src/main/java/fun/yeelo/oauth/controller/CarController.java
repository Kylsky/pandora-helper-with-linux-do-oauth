package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.domain.account.AccountVO;
import fun.yeelo.oauth.domain.car.CarApply;
import fun.yeelo.oauth.domain.car.CarApplyVO;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/car")
@Slf4j
public class CarController {
    @Autowired
    private CarService carService;

    @GetMapping("/list")
    public HttpResult<PageVO<AccountVO>> list(HttpServletRequest request, @RequestParam(required = false) String owner, @RequestParam Integer page, @RequestParam Integer size) {
        return carService.listCars(request,owner,page,size);
    }

    @GetMapping("/fetchApplies")
    public HttpResult<List<LabelDTO>> fetchApplies(HttpServletRequest request,
                                                   @RequestParam Integer accountId) {
        return carService.fetchApplies(request,accountId);
    }

    @PostMapping("/apply")
    public HttpResult<Boolean> carApply(HttpServletRequest request, @RequestBody CarApply dto) {
        return carService.carApply(request,dto);
    }

    @PostMapping("/audit")
    public HttpResult<Boolean> refresh(@RequestBody CarApplyVO dto) {
        return carService.audit(dto);
    }


}
