package fun.yeelo.oauth.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConvertUtil {

    /**
     * 单个对象转换
     *
     * @param source     source
     * @param targetType 要转换为的目标类型
     * @return {@link R }
     * @date 2020/12/29 23:04
     */
    public static <T, R> R convert(T source, Class<R> targetType) {
        try {
            if (Objects.nonNull(source) && Objects.nonNull(targetType)) {
                R target = targetType.newInstance();
                BeanUtils.copyProperties(source, target);
                return target;
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将 source 分页数据转换为 targetType 类型的分页数据
     *
     * @param source     source
     * @param targetType 要转换为的目标类型
     * @date 2020/12/29 20:11
     */
    public static <T, R> Page<R> convertPage(Page<T> source, Class<R> targetType) {
        Page<R> target = new Page<>();
        if (Objects.nonNull(source) && Objects.nonNull(targetType)) {
            BeanUtils.copyProperties(source, target);
            List<T> sourceRecords = source.getRecords();
            if (!CollectionUtils.isEmpty(sourceRecords)) {
                List<R> records = convertList(sourceRecords, targetType);
                target.setRecords(records);
            }
        }
        return target;
    }


    /**
     * 将 source list数据转换为 targetType 类型的list数据
     *
     * @param source     source
     * @param targetType 要转换为的目标类型
     * @return {@link List<R> }
     * @date 2020/12/29 22:57
     */
    public static <T, R> List<R> convertList(List<T> source, Class<R> targetType) {
        if (!CollectionUtils.isEmpty(source)) {
            return source.stream().map(t -> convert(t, targetType))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
