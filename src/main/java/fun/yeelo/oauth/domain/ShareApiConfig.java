package fun.yeelo.oauth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("share_api_config")
@Data
public class ShareApiConfig {
    @TableId(type= IdType.AUTO)
    private Integer id;

    @TableField("share_id")
    private Integer shareId;

    @TableField("account_id")
    private Integer accountId;

    @TableField("api_proxy")
    private String apiProxy;

    @TableField("api_key")
    private String apiKey;

}
