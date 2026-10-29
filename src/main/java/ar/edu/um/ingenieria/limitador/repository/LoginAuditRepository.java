package ar.edu.um.ingenieria.limitador.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.um.ingenieria.limitador.domain.LoginAudit;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    Page<LoginAudit> findByUsername(String username, Pageable pageable);

    Page<LoginAudit> findBySuccess(boolean success, Pageable pageable);
}
