package be.ephec.padelmanager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateHoraireRequest {
    @NotNull
    private LocalTime heureOuverture;

    @NotNull
    private LocalTime heureFermeture;
}
