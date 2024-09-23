package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class UsageVO {
    private Integer gpt4o = 0;

    private Integer gpt4 = 0;

    private Integer gpt4omini = 0;

    private Integer o1Preview = 0;

    private Integer o1Mini = 0;
}
