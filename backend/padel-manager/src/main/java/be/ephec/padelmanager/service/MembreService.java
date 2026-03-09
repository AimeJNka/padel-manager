package be.ephec.padelmanager.service;

import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.repository.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembreService {

    private final MembreRepository membreRepository;

    public List<Membre> findAll() {
        return membreRepository.findAll();
    }

    // TODO (DTO milestone): implement TR_Membre_CheckCumul business rule here.
    // When creating/updating a membre, verify that a personne cannot hold an L membership
    // alongside a G or S membership. L is exclusive; G and S can coexist.
    // Throw an appropriate exception with a clear message if the rule is violated.
}