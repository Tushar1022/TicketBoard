package com.aurionpro.ticketboard.common.lookup.repository;

import com.aurionpro.ticketboard.common.lookup.entity.LookupData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LookupDataRepository extends JpaRepository<LookupData, Long> {
    List<LookupData> findByCategoryOrderByDisplayOrderAsc(String category);
    List<LookupData> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(String category);
    Optional<LookupData> findByCategoryAndValue(String category, String value);
    boolean existsByCategoryAndValue(String category, String value);
    @Query("SELECT DISTINCT l.category FROM LookupData l ORDER BY l.category")
    List<String> findDistinctCategories();
}