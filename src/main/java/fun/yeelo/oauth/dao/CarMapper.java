package fun.yeelo.oauth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.CarApply;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarMapper extends BaseMapper<CarApply> {

}
