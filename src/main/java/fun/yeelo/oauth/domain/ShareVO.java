package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class ShareVO extends Share{
    private Boolean isShared;

    private String address;

    private String jmc;
    private String jwt;

    private String gptEmail;

    private Integer gptConfigId;

    private String claudeEmail;

    private Integer claudeConfigId;

    private Integer apiConfigId;

    private String token;

    // 共享类型，1gpt，2claude
    private Integer type;

    private Integer accountId;

    private Integer curAdminId;

    private String gptCarName;

    private String claudeCarName;

    private String apiCarName;

    private String username;

    private Integer duration;

    private Boolean self = false;

}
