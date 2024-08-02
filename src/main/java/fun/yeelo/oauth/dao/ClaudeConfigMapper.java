package fun.yeelo.oauth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.yeelo.oauth.domain.ShareClaudeConfig;
import fun.yeelo.oauth.domain.ShareGptConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaudeConfigMapper extends BaseMapper<ShareClaudeConfig> {
}
