package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class EmailDto {
    private String value;

    private String label;

    public EmailDto(String value, String label) {
        this.value = value;
        this.label = label;
    }
}
