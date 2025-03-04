package fun.yeelo.oauth.domain.midjourney;

import fun.yeelo.oauth.domain.midjourney.Search;
import lombok.Data;

import java.util.List;

@Data
public class UserRequest {
    private Pagination pagination;
    private Sort sort;
    private Search search;
    private TaskInfo taskInfo;

    private List<String> ids;

}