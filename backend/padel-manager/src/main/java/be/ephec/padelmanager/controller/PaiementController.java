package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.PaiementDTO;
import be.ephec.padelmanager.service.IPaiementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
public class PaiementController {

    private final IPaiementService paiementService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('GLOBAL','SITE','LIBRE')")
    public List<PaiementDTO> mesPaiements(Authentication auth) {
        return paiementService.listerPaiementsMembre(auth);
    }

    @PostMapping("/{id}/payer")
    @PreAuthorize("isAuthenticated()")
    public PaiementDTO payer(@PathVariable Integer id, Authentication auth) {
        return paiementService.payerParMembre(id, auth);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL','ADMIN_SITE')")
    public Page<PaiementDTO> lister(
            @RequestParam(required = false) Integer matchId,
            @RequestParam(required = false) String matricule,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        size = Math.min(size, 100);
        return paiementService.listerPaiementsAdmin(
                matchId, matricule, statut, siteId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "datePaiement")),
                auth);
    }

    @PostMapping("/{id}/rembourser")
    @PreAuthorize("hasAnyRole('ADMIN_GLOBAL','ADMIN_SITE')")
    public PaiementDTO rembourser(@PathVariable Integer id, Authentication auth) {
        return paiementService.rembourserPaiement(id, auth);
    }
}
