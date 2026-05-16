package be.ephec.padelmanager.service.impl;


import be.ephec.padelmanager.dto.TypeMembreDTO;
import be.ephec.padelmanager.model.TypeMembre;
import be.ephec.padelmanager.repository.TypeMembreRepo;
import be.ephec.padelmanager.service.ITypeMembreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeMembreService implements ITypeMembreService {
    private final TypeMembreRepo typeMembreRepo;

    @Override
    public List<TypeMembreDTO> findAll() {
        return typeMembreRepo.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private TypeMembreDTO toDTO(TypeMembre typeMembre) {
        TypeMembreDTO dto = new TypeMembreDTO();
        dto.setIdType(typeMembre.getIdType());
        dto.setPrefixe(typeMembre.getPrefixe());
        dto.setLibelle(typeMembre.getLibelle());
        dto.setDelaiReservationJours(typeMembre.getDelaiReservationJours());
        dto.setPeutCreerMatch(typeMembre.getPeutCreerMatch());
        return dto;
    }
}
