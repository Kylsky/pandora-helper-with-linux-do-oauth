package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class LabelDTO {
    private String value;

    private String label;

    private String text;

    public LabelDTO(String value, String label, String text) {
        this.value = value;
        this.label = label;
        this.text = text;
    }

}
