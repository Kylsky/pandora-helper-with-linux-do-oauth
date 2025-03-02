package fun.yeelo.oauth.domain.account;

import lombok.Data;

@Data
public class AccountVO extends Account{
    private String type;

    private String username;

    private String usernameDesc;

    private Integer count;

    private String countDesc;

    private Integer applyNum;

    private boolean authorized;

}
