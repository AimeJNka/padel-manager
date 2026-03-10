package be.ephec.padelmanager.service.Impl;


import be.ephec.padelmanager.repository.TypeMembreRepo;
import be.ephec.padelmanager.service.ITypeMembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TypeMembreService implements ITypeMembreService {
    private final TypeMembreRepo typeMembreRepo;
}
