package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.MembreDTO;
import be.ephec.padelmanager.dto.MembreProfilDTO;
import be.ephec.padelmanager.dto.MembreSearchDTO;
import be.ephec.padelmanager.dto.UpdateMembreRequest;
import be.ephec.padelmanager.integration.TestAuth;
import be.ephec.padelmanager.exception.ForbiddenException;
import be.ephec.padelmanager.exception.NotFoundException;
import be.ephec.padelmanager.model.Membre;
import be.ephec.padelmanager.model.Personne;
import be.ephec.padelmanager.model.Site;
import be.ephec.padelmanager.model.TypeMembre;
import be.ephec.padelmanager.repository.MembreRepo;
import be.ephec.padelmanager.repository.PersonneRepo;
import be.ephec.padelmanager.service.impl.MembreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembreServiceTest {

    @Mock MembreRepo membreRepo;
    @Mock PersonneRepo personneRepo;

    MembreService service;

    @BeforeEach
    void setUp() {
        service = new MembreService(membreRepo, personneRepo);
    }

    private Membre buildMembre(String matricule, String email, String nom, String prenom) {
        Personne personne = new Personne();
        personne.setEmail(email);
        personne.setNom(nom);
        personne.setPrenom(prenom);

        Membre membre = new Membre();
        membre.setMatricule(matricule);
        membre.setPersonne(personne);
        membre.setSoldeDu(BigDecimal.ZERO);
        membre.setDateInscription(LocalDate.now());
        return membre;
    }

    private TestingAuthenticationToken authAdmin() {
        return new TestingAuthenticationToken("admin-user", null, "ROLE_ADMIN_GLOBAL");
    }

    private TestingAuthenticationToken authAdminSite(int idSite) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("admin-site-user", null, "ROLE_ADMIN_SITE");
        auth.setDetails(idSite);
        return auth;
    }

    private TestingAuthenticationToken authMembre(String matricule) {
        return new TestingAuthenticationToken(matricule, null);
    }

    // ── getProfil ────────────────────────────────────────────────────────────

    @Test
    void getProfil_existingMembre_returnsFullProfilDTO() {
        Membre membre = buildMembre("G0001", "jean@test.be", "Dupont", "Jean");
        membre.setSoldeDu(new BigDecimal("10.00"));

        TypeMembre type = new TypeMembre();
        type.setLibelle("Global");
        membre.setTypeMembre(type);

        Site site = new Site();
        site.setNom("Padel Brussels");
        membre.setSite(site);

        when(membreRepo.findById("G0001")).thenReturn(Optional.of(membre));

        MembreProfilDTO dto = service.getProfil("G0001");

        assertThat(dto.getMatricule()).isEqualTo("G0001");
        assertThat(dto.getNom()).isEqualTo("Dupont");
        assertThat(dto.getEmail()).isEqualTo("jean@test.be");
        assertThat(dto.getTypeMembre()).isEqualTo("Global");
        assertThat(dto.getSiteNom()).isEqualTo("Padel Brussels");
        assertThat(dto.getSoldeDu()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void getProfil_membreWithNullPersonne_returnsDTOWithNullPersonneFields() {
        Membre membre = new Membre();
        membre.setMatricule("G0001");
        membre.setPersonne(null);
        membre.setTypeMembre(null);
        membre.setSite(null);
        membre.setSoldeDu(BigDecimal.ZERO);

        when(membreRepo.findById("G0001")).thenReturn(Optional.of(membre));

        MembreProfilDTO dto = service.getProfil("G0001");

        assertThat(dto.getMatricule()).isEqualTo("G0001");
        assertThat(dto.getNom()).isNull();
        assertThat(dto.getEmail()).isNull();
        assertThat(dto.getTypeMembre()).isNull();
        assertThat(dto.getSiteNom()).isNull();
        assertThat(dto.getSoldeDu()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getProfil_unknownMatricule_throwsNotFoundException() {
        when(membreRepo.findById("X9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfil("X9999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("X9999");
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    void findAll_adminGlobal_emptyRepo_returnsEmptyList() {
        when(membreRepo.findAll()).thenReturn(Collections.emptyList());

        List<MembreDTO> result = service.findAll(authAdmin());

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void findAll_adminGlobal_twoMembers_returnsTwoMappedDTOs() {
        Membre m1 = buildMembre("G0001", "g1@test.be", "Dupont", "Jean");
        Membre m2 = buildMembre("S0001", "s1@test.be", "Martin", "Sophie");
        when(membreRepo.findAll()).thenReturn(List.of(m1, m2));

        List<MembreDTO> result = service.findAll(authAdmin());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMatricule()).isEqualTo("G0001");
        assertThat(result.get(1).getMatricule()).isEqualTo("S0001");
    }

    @Test
    void findAll_adminSite_filtersByAdminSiteId() {
        Membre m1 = buildMembre("S0001", "s1@test.be", "Martin", "Sophie");
        Site site1 = new Site();
        site1.setIdSite(1);
        m1.setSite(site1);

        Membre m2 = buildMembre("S0099", "s99@test.be", "Other", "Site");
        Site site2 = new Site();
        site2.setIdSite(2);
        m2.setSite(site2);

        Membre m3 = buildMembre("G0001", "g1@test.be", "Dupont", "Jean");
        // No site on m3 (Global)

        when(membreRepo.findAll()).thenReturn(List.of(m1, m2, m3));

        List<MembreDTO> result = service.findAll(authAdminSite(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMatricule()).isEqualTo("S0001");
    }

    // ── getOne ───────────────────────────────────────────────────────────────

    @Test
    void getOne_existingMembre_returnsSameResultAsGetProfil() {
        Membre membre = buildMembre("G0001", "jean@test.be", "Dupont", "Jean");
        when(membreRepo.findById("G0001")).thenReturn(Optional.of(membre));

        MembreProfilDTO dto = service.getOne("G0001");

        assertThat(dto.getMatricule()).isEqualTo("G0001");
    }

    @Test
    void getOne_unknownMatricule_throwsNotFoundException() {
        when(membreRepo.findById("X9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOne("X9999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("X9999");
    }

    // ── updateMembre ─────────────────────────────────────────────────────────

    @Test
    void updateMembre_adminGlobal_canUpdateAnyMembre() {
        Membre membre = buildMembre("G0001", "old@test.be", "Dupont", "Jean");
        when(membreRepo.findById("G0001")).thenReturn(Optional.of(membre));

        UpdateMembreRequest request = new UpdateMembreRequest();
        request.setEmail("new@test.be");

        service.updateMembre("G0001", request, authAdmin());

        assertThat(membre.getPersonne().getEmail()).isEqualTo("new@test.be");
        verify(personneRepo, times(1)).save(membre.getPersonne());
    }

    @Test
    void updateMembre_memberUpdatesSelf_succeeds() {
        Membre membre = buildMembre("G0001", "old@test.be", "Dupont", "Jean");
        when(membreRepo.findById("G0001")).thenReturn(Optional.of(membre));

        UpdateMembreRequest request = new UpdateMembreRequest();
        request.setEmail("updated@test.be");
        request.setTelephone("0471223344");

        service.updateMembre("G0001", request, authMembre("G0001"));

        assertThat(membre.getPersonne().getEmail()).isEqualTo("updated@test.be");
        assertThat(membre.getPersonne().getTelephone()).isEqualTo("0471223344");
        verify(personneRepo, times(1)).save(membre.getPersonne());
    }

    @Test
    void updateMembre_crossMemberUpdate_throwsForbidden() {
        // "Blocage croisé G+S": member G0001 attempts to update member S0001.
        // Guard fires before any DB call: !isAdminGlobal AND !"S0001".equals("G0001").
        UpdateMembreRequest request = new UpdateMembreRequest();
        request.setEmail("hacked@test.be");

        assertThatThrownBy(() -> service.updateMembre("S0001", request, authMembre("G0001")))
                .isInstanceOf(ForbiddenException.class);

        verify(membreRepo, never()).findById(any());
    }

    @Test
    void updateMembre_memberNotFound_throwsNotFoundException() {
        when(membreRepo.findById("X9999")).thenReturn(Optional.empty());

        UpdateMembreRequest request = new UpdateMembreRequest();
        request.setEmail("ghost@test.be");

        assertThatThrownBy(() -> service.updateMembre("X9999", request, authAdmin()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("X9999");

        verify(personneRepo, never()).save(any());
    }

    @Test
    void updateMembre_allRequestFieldsNull_personneUnchangedButStillSaved() {
        Membre membre = buildMembre("G0001", "orig@test.be", "Dupont", "Jean");
        membre.getPersonne().setTelephone("0470");
        when(membreRepo.findById("G0001")).thenReturn(Optional.of(membre));

        UpdateMembreRequest request = new UpdateMembreRequest();
        // both email and telephone left null — patch semantics, neither field updated

        service.updateMembre("G0001", request, authMembre("G0001"));

        assertThat(membre.getPersonne().getEmail()).isEqualTo("orig@test.be");
        assertThat(membre.getPersonne().getTelephone()).isEqualTo("0470");
        verify(personneRepo, times(1)).save(membre.getPersonne());
    }

    /**
     * Documents the current behavior when a Membre has no associated Personne:
     * the service silently no-ops on personne data updates and returns a DTO with
     * null personne fields. This is not necessarily desirable behavior — see
     * Sprint T-B1 mini-gate Insight section A. Test exists to lock the current
     * behavior; future refactor may turn this into a NotFoundException.
     */
    @Test
    void updateMembre_nullPersonne_personneRepoNeverCalled() {
        Membre membre = new Membre();
        membre.setMatricule("G0001");
        membre.setPersonne(null);
        membre.setSoldeDu(BigDecimal.ZERO);
        when(membreRepo.findById("G0001")).thenReturn(Optional.of(membre));

        UpdateMembreRequest request = new UpdateMembreRequest();
        request.setEmail("any@test.be");

        MembreProfilDTO dto = service.updateMembre("G0001", request, authAdmin());

        assertThat(dto).isNotNull();
        assertThat(dto.getEmail()).isNull();
        verify(personneRepo, never()).save(any());
    }

    // ── search ───────────────────────────────────────────────────────────────

    @Test
    void search_emptyQuery_returnsEmpty() {
        var auth = new TestingAuthenticationToken("G0001", null, "ROLE_GLOBAL");

        assertThat(service.search("", null, auth)).isEmpty();
        assertThat(service.search("a", null, auth)).isEmpty();
        assertThat(service.search(null, null, auth)).isEmpty();

        verify(membreRepo, never()).searchByPattern(any(), any(), any());
        verify(membreRepo, never()).searchByPatternAndSite(any(), any(), any(), any());
    }

    @Test
    void search_returnsLightweightDTOs_noSensitiveFields() {
        Membre membre = buildMembre("G0001", "private@test.be", "Dupont", "Jean");
        membre.setSoldeDu(BigDecimal.valueOf(50));
        Site site = new Site();
        site.setNom("Brussels");
        membre.setSite(site);
        var auth = new TestingAuthenticationToken("G0001", null, "ROLE_GLOBAL");
        when(membreRepo.searchByPattern(any(), any(), any())).thenReturn(List.of(membre));

        List<MembreSearchDTO> results = service.search("jean", null, auth);

        assertThat(results).hasSize(1);
        MembreSearchDTO dto = results.get(0);
        assertThat(dto.getMatricule()).isEqualTo("G0001");
        assertThat(dto.getPrenom()).isEqualTo("Jean");
        assertThat(dto.getNom()).isEqualTo("Dupont");
        assertThat(dto.getSiteNom()).isEqualTo("Brussels");
    }

    @Test
    void search_filtersBySiteForSiteRole() {
        var auth = TestAuth.membreSite("S0002", 1);
        when(membreRepo.searchByPatternAndSite(any(), any(), any(), any())).thenReturn(List.of());

        service.search("jean", null, auth);

        verify(membreRepo).searchByPatternAndSite(any(), any(), any(), any());
        verify(membreRepo, never()).searchByPattern(any(), any(), any());
    }

    @Test
    void search_passesMatchSiteIdToRepo() {
        Membre s0005 = buildMembreWithSite("S0005", 2, "Site");
        var auth = new TestingAuthenticationToken("G0001", null, "ROLE_GLOBAL");
        when(membreRepo.searchByPattern(any(), org.mockito.ArgumentMatchers.eq(2), any()))
                .thenReturn(List.of(s0005));

        List<MembreSearchDTO> results = service.search("test", 2, auth);

        verify(membreRepo).searchByPattern(any(), org.mockito.ArgumentMatchers.eq(2), any());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatricule()).isEqualTo("S0005");
    }

    @Test
    void search_includesGlobalAndLibreWhenMatchSiteIdProvided() {
        Membre g0002 = buildMembreWithType("G0002", "Global");
        Membre l0001 = buildMembreWithType("L0001", "Libre");
        var auth = new TestingAuthenticationToken("G0001", null, "ROLE_GLOBAL");
        when(membreRepo.searchByPattern(any(), org.mockito.ArgumentMatchers.eq(1), any()))
                .thenReturn(List.of(g0002, l0001));

        List<MembreSearchDTO> results = service.search("test", 1, auth);

        assertThat(results).extracting(MembreSearchDTO::getMatricule)
                .containsExactlyInAnyOrder("G0002", "L0001");
    }

    private Membre buildMembreWithSite(String matricule, Integer siteId, String libelleType) {
        Membre m = buildMembre(matricule, matricule + "@test.be",
                               "Nom-" + matricule, "Prenom-" + matricule);
        Site site = new Site();
        site.setIdSite(siteId);
        m.setSite(site);
        TypeMembre tm = new TypeMembre();
        tm.setLibelle(libelleType);
        m.setTypeMembre(tm);
        return m;
    }

    private Membre buildMembreWithType(String matricule, String libelleType) {
        Membre m = buildMembre(matricule, matricule + "@test.be",
                               "Nom-" + matricule, "Prenom-" + matricule);
        TypeMembre tm = new TypeMembre();
        tm.setLibelle(libelleType);
        m.setTypeMembre(tm);
        return m;
    }
}
