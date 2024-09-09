package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class InfoVO {
    private String email;

    private UsageVO usage;

    private String userId;

    private String uniqueName;
}
