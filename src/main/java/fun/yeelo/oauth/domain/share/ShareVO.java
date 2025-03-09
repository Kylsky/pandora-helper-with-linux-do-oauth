package fun.yeelo.oauth.domain.share;

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

    private Integer grokConfigId;

    private String grokEmail;

    private String token;

    // 共享类型，1gpt，2claude
    private Integer type;

    private Integer accountId;

    private Integer curAdminId;

    private String gptCarName;

    private String claudeCarName;

    private String grokCarName;

    private String apiCarName;

    private String username;

    private Integer duration;

    private Integer gptUserCount;

    private Integer claudeUserCount;

    private Integer grokUserCount;

    private Integer apiUserCount;

    private Boolean self = false;

    private String claudeExpiresAt;

    private String grokExpiresAt;

    private String apiExpiresAt;

    private String gptExpiresAt;

    private String apiKey;

    private Boolean mjEnable;

    private Boolean customMjConfig;

}
