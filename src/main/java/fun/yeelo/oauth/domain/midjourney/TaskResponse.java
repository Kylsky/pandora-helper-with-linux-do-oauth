package fun.yeelo.oauth.domain.midjourney;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import org.json.JSONObject;

import java.util.List;

@Data
public class TaskResponse {
    private JSONArray list;
    private Pagination pagination;
    private Boolean customMjConfig;

}