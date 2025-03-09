package fun.yeelo.oauth.domain.share;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@TableName("share_grok_config")
@Data
public class ShareGrokConfig {
    @TableId(type= IdType.AUTO)
    private Integer id;

    @TableField("share_id")
    private Integer shareId;

    @TableField("account_id")
    private Integer accountId;

    @TableField("oauth_token")
    private String oauthToken;

    @TableField("expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

}
