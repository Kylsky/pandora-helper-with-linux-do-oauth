package fun.yeelo.oauth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.yeelo.oauth.domain.share.ShareClaudeConfig;
import fun.yeelo.oauth.domain.share.ShareGrokConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface GrokConfigMapper extends BaseMapper<ShareGrokConfig> {
}
