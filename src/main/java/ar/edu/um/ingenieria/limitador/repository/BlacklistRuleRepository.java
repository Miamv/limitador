package ar.edu.um.ingenieria.limitador.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;

@Repository
public interface BlacklistRuleRepository extends JpaRepository<BlacklistRule, Long> {

    @Query("""
            SELECT r FROM BlacklistRule r
            WHERE r.active = true
              AND (r.expiresAt IS NULL OR r.expiresAt > :now)
              AND (
                    (:username IS NOT NULL AND LOWER(r.username) = LOWER(:username))
                    OR r.ipOrCidr IS NOT NULL
              )
            """)
    List<BlacklistRule> findActiveRulesCandidates(@Param("username") String username, @Param("now") LocalDateTime now);

    List<BlacklistRule> findByActiveTrue();

    List<BlacklistRule> findByUsername(String username);

    @Query("SELECT r FROM BlacklistRule r WHERE r.ipOrCidr = :ipOrCidr")
    List<BlacklistRule> findByIpOrCidr(@Param("ipOrCidr") String ipOrCidr);
}
