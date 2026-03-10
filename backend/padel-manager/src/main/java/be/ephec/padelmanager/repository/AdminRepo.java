package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepo extends JpaRepository<Admin,Integer> {
}
