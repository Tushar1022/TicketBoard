package com.aurionpro.ticketboard.client.repository;

import com.aurionpro.ticketboard.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByClientCode(String clientCode);
    boolean existsByClientCode(String clientCode);
}
