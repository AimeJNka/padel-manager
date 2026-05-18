package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.TypeMembreDTO;
import be.ephec.padelmanager.model.TypeMembre;
import be.ephec.padelmanager.repository.TypeMembreRepo;
import be.ephec.padelmanager.service.impl.TypeMembreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TypeMembreServiceTest {

    @Mock TypeMembreRepo typeMembreRepo;

    TypeMembreService service;

    @BeforeEach
    void setUp() {
        service = new TypeMembreService(typeMembreRepo);
    }

    @Test
    void findAll_emptyRepo_returnsEmptyList() {
        when(typeMembreRepo.findAll()).thenReturn(Collections.emptyList());

        List<TypeMembreDTO> result = service.findAll();

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void findAll_twoTypes_returnsMappedDTOs() {
        TypeMembre type1 = buildTypeMembre(1, "G", "Global", 14, true);
        TypeMembre type2 = buildTypeMembre(2, "L", "Local", 7, false);
        when(typeMembreRepo.findAll()).thenReturn(List.of(type1, type2));

        List<TypeMembreDTO> result = service.findAll();

        assertThat(result).hasSize(2);
        TypeMembreDTO dto = result.get(0);
        assertThat(dto.getIdType()).isEqualTo(1);
        assertThat(dto.getPrefixe()).isEqualTo("G");
        assertThat(dto.getLibelle()).isEqualTo("Global");
        assertThat(dto.getDelaiReservationJours()).isEqualTo(14);
        assertThat(dto.getPeutCreerMatch()).isTrue();
    }

    // ════ helpers ═════════════════════════════════════════════════

    private static TypeMembre buildTypeMembre(Integer id, String prefixe, String libelle,
                                              Integer delai, Boolean peutCreer) {
        TypeMembre t = new TypeMembre();
        t.setIdType(id);
        t.setPrefixe(prefixe);
        t.setLibelle(libelle);
        t.setDelaiReservationJours(delai);
        t.setPeutCreerMatch(peutCreer);
        return t;
    }
}
