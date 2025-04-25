package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.dao.MidjourneyTaskMapper;
import fun.yeelo.oauth.domain.midjourney.MidjourneyTask;
import org.springframework.stereotype.Service;

@Service
public class MidjourneyTaskService extends ServiceImpl<MidjourneyTaskMapper, MidjourneyTask> implements IService<MidjourneyTask> {

}
