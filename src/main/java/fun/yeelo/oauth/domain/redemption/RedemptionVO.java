package fun.yeelo.oauth.domain.redemption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RedemptionVO extends Redemption{
    private String email;

    private String accountType;

    private Integer count;
}
