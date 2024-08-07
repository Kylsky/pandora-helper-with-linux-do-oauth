package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class LabelDTO {
    private String value;

    private String label;

    public LabelDTO(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
