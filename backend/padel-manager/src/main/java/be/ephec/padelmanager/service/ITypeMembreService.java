package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.TypeMembreDTO;

import java.util.List;

public interface ITypeMembreService {
    List<TypeMembreDTO> findAll();
}
