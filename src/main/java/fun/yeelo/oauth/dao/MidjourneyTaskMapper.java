package fun.yeelo.oauth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.yeelo.oauth.domain.car.CarApply;
import fun.yeelo.oauth.domain.midjourney.MidjourneyTask;
import org.springframework.stereotype.Repository;

@Repository
public interface MidjourneyTaskMapper extends BaseMapper<MidjourneyTask> {

}
