package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.dao.CarMapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@Service
public class CarService extends ServiceImpl<CarMapper, CarApply> implements IService<CarApply> {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ShareService shareService;

    public HttpResult<Boolean> audit(HttpServletRequest request, CarApplyVO dto) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        if (dto.getIds()==null && dto.getShareId()!=null ) {
            dto.setIds(Collections.singletonList(dto.getShareId()));
        }
        if (!dto.getAllowApply().equals(1)) {
            dto.getIds().forEach(id -> {
                this.remove(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId, dto.getId()).eq(CarApply::getShareId, id));
            });
            return HttpResult.success();
        }

        dto.getIds().forEach(id -> {
            ShareVO shareVO = new ShareVO();
            shareVO.setId(id);
            shareVO.setAccountId(dto.getAccountId());
            this.remove(new LambdaQueryWrapper<CarApply>().eq(CarApply::getAccountId, dto.getAccountId()).eq(CarApply::getShareId, id));
            shareService.distribute(shareVO);
        });
        return HttpResult.success();
    }
}

