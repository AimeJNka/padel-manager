package be.ephec.padelmanager.service.Impl;

import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.service.IMembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembreService implements IMembreService {

    private final MembreRepo membreRepo;

}