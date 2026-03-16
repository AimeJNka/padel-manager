package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.SiteDTO;

import java.util.List;

public interface ISiteService {
    List<SiteDTO> findAll();
    SiteDTO create(SiteDTO dto);
}
