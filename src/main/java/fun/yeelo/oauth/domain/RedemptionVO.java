package fun.yeelo.oauth.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedemptionVO extends Redemption{
    private String email;

    private String accountType;

    private Integer count;
}
