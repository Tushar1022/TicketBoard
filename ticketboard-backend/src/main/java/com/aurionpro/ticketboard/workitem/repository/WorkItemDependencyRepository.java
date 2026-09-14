package com.aurionpro.ticketboard.workitem.repository;

import com.aurionpro.ticketboard.workitem.entity.WorkItemDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkItemDependencyRepository extends JpaRepository<WorkItemDependency, Long> {
    List<WorkItemDependency> findBySourceItemId(Long sourceItemId);
    List<WorkItemDependency> findByTargetItemId(Long targetItemId);
}
