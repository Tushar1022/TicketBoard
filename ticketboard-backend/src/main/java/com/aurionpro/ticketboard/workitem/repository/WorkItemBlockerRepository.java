package com.aurionpro.ticketboard.workitem.repository;

import com.aurionpro.ticketboard.workitem.entity.WorkItemBlocker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkItemBlockerRepository extends JpaRepository<WorkItemBlocker, Long> {
    List<WorkItemBlocker> findByWorkItemId(Long workItemId);
    List<WorkItemBlocker> findByResolvedAtIsNull();
}
