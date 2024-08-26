package fun.yeelo.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("redemption")
@Data
public class Redemption {
    @TableId(type= IdType.AUTO)
    private Integer id;

    @TableField("target_user_name")
    private String targetUserName;

    @TableField("code")
    private String code;

    @TableField("account_id")
    private Integer accountId;

    @TableField("user_id")
    private Integer userId;

    @TableField("duration")
    private Integer duration;

    @TableField("time_unit")
    private Integer timeUnit;

    @TableField("create_time")
    private LocalDateTime createTime;
}
