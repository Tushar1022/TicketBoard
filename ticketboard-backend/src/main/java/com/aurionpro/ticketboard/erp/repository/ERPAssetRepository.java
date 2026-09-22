package com.aurionpro.ticketboard.erp.repository;

import com.aurionpro.ticketboard.erp.entity.ERPAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ERPAssetRepository extends JpaRepository<ERPAsset, Long> {
    List<ERPAsset> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
