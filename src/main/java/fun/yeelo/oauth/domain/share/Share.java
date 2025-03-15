package fun.yeelo.oauth.domain.share;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.lang.ref.PhantomReference;

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

    @TableField("comment")
    private String comment;

    @TableField("expires_at")
    private String expiresAt;

    @TableField("parent_id")
    private Integer parentId;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("trust_level")
    private Integer trustLevel;

    @TableField("mj_user_id")
    private String mjUserId;

    @TableField("mj_enable")
    private Boolean mjEnable;

    @TableField("mj_proxy_url")
    private String mjProxyUrl;

    @TableField("mj_proxy_key")
    private String mjProxyKey;

    @TableField("chat_gpt_url")
    private String chatGptUrl;

    @TableField("chat_gpt_password")
    private String chatGptPassword;

    @TableField("proxy_url")
    private String proxyUrl;
}
