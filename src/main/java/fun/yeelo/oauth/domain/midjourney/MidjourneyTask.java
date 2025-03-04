package fun.yeelo.oauth.domain.midjourney;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("midjourney_task")
public class MidjourneyTask {
    @TableId(type= IdType.AUTO)
    private Long id;

    @TableField("user_name")
    private String username;

    @TableField("task_id")
    private String taskId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
