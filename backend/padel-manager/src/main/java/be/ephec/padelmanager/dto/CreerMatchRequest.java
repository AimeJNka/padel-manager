package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreerMatchRequest {
    @NotNull
    private Integer dispoId;
}
