package fun.yeelo.oauth.domain;

import lombok.Data;

import java.util.List;

@Data
public class PageVO <T>{
    private List<T> data;
    private Integer total;
}
