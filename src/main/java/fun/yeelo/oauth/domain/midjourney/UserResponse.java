package fun.yeelo.oauth.domain.midjourney;

import lombok.Data;

import java.util.List;

@Data
public class UserResponse {
    private List<User> list;
    private Pagination pagination;
    private Boolean customMjConfig;

}