package fun.yeelo.oauth.domain.midjourney;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class Pagination {
    private Integer current;
    private Integer pageSize;
    private Integer total;

}