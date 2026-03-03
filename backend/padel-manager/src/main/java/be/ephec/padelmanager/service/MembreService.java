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
}