package fun.yeelo.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@TableName("share")
@Data
public class Share {
    @TableId(type= IdType.AUTO)
    private Integer id;

    @TableField("account_id")
    private Integer accountId;

    @TableField("unique_name")
    private String uniqueName;

    @TableField("password")
    private String password;

    @TableField("share_token")
    private String shareToken;

    @TableField("comment")
    private String comment;

    @TableField("expires_at")
    private String expiresAt;

    @TableField("parent_id")
    private Integer parentId;
}
