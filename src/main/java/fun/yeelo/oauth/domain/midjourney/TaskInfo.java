package fun.yeelo.oauth.domain.midjourney;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
public class TaskInfo {
    private String id;

    private String parentId;

    private String userId;
}
