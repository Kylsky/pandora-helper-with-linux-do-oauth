package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class UserConfigVO {
    private String mjProxyUrl;
    private String mjProxyKey;

    private String chatGptUrl;
    private String chatGptPassword;

    private String proxyUrl;
}
