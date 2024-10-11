package fun.yeelo.oauth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.yeelo.oauth.domain.ShareApiConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiConfigMapper extends BaseMapper<ShareApiConfig> {
}
