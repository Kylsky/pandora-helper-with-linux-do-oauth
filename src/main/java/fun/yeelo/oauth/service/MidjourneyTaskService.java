package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.MidjourneyTaskMapper;
import fun.yeelo.oauth.dao.RedemptionMapper;
import fun.yeelo.oauth.domain.PageVO;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.midjourney.MidjourneyTask;
import fun.yeelo.oauth.domain.redemption.Redemption;
import fun.yeelo.oauth.domain.redemption.RedemptionVO;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.utils.ConvertUtil;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MidjourneyTaskService extends ServiceImpl<MidjourneyTaskMapper, MidjourneyTask> implements IService<MidjourneyTask> {

}
