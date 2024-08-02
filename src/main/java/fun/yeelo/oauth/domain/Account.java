package fun.yeelo.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@TableName("account")
@Data
public class Account {
    @TableId(type= IdType.AUTO)
    private Integer id;

    @TableField("email")
    private String email;

    @TableField("password")
    private String password;

    @TableField("session_token")
    private String sessionToken;

    @TableField("access_token")
    private String accessToken;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @TableField("shared")
    private Integer shared;

    @TableField("refresh_token")
    private String refreshToken;

    @TableField("user_id")
    private Integer userId;

    @TableField("account_type")
    private Integer accountType;


}
