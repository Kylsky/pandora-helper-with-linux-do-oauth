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

    @TableField("expires_in")
    private Integer expiresIn;

    @TableField("expires_at")
    private String expiresAt;

    @TableField("site_limit")
    private String siteLimit;

    @TableField("gpt4_limit")
    private Integer gpt4Limit;

    @TableField("gpt35_limit")
    private Integer gpt35Limit;

    @TableField("show_userinfo")
    private Boolean showUserinfo;

    @TableField("show_conversations")
    private Boolean showConversations;

    @TableField("refresh_everyday")
    private Boolean refreshEveryday;

    @TableField("temporary_chat")
    private Boolean temporaryChat;


    // Getters and setters
}
