package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class ShareVO extends Share{
    private Boolean isShared;

    private String address;

    private String jmc;

    private String gptEmail;

    private String claudeEmail;

    private String token;

    // 共享类型，1gpt，2claude
    private Integer type;

    private Integer accountId;
}
