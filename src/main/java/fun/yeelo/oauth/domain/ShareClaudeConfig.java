package fun.yeelo.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("share_claude_config")
@Data
public class ShareClaudeConfig {
    @TableId(type= IdType.AUTO)
    private Integer id;

    @TableField("share_id")
    private Integer shareId;

    @TableField("account_id")
    private Integer accountId;

    @TableField("oauth_token")
    private String oauthToken;

    @TableField("expires_at")
    private String expiresAt;

}
