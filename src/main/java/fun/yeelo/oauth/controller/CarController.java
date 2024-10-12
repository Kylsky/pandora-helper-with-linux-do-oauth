package fun.yeelo.oauth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.One;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/car")
@Slf4j
public class CarController {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CarService carService;
    @Autowired
    private GptConfigService gptConfigService;
    @Autowired
    private ClaudeConfigService claudeConfigService;
    @Autowired
    private ApiConfigService apiConfigService;

    @GetMapping("/list")
    public HttpResult<PageVO<AccountVO>> list(HttpServletRequest request, @RequestParam(required = false) String owner,@RequestParam Integer page,@RequestParam Integer size) {
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
    public HttpResult<Boolean> refresh(HttpServletRequest request, @RequestBody CarApplyVO dto) {
        return carService.audit(request,dto);
    }


}
