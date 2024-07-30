package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class ShareVO extends Share{
    private Boolean isShared;

    private String address;

    private String jmc;

    private String email;
}
