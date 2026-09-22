package com.aurionpro.ticketboard.client.repository;

import com.aurionpro.ticketboard.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByClientCode(String clientCode);
    boolean existsByClientCode(String clientCode);

    @Modifying
    @Query("UPDATE Client c SET c.accountManager = null WHERE c.accountManager.id = :userId")
    void detachAccountManager(@Param("userId") Long userId);
}
