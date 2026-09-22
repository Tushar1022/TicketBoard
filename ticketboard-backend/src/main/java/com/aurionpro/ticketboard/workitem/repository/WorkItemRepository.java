package com.aurionpro.ticketboard.workitem.repository;

import com.aurionpro.ticketboard.workitem.entity.WorkItem;
import com.aurionpro.ticketboard.workitem.enums.WorkItemStatus;
import com.aurionpro.ticketboard.workitem.enums.WorkItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    Optional<WorkItem> findByTicketNumber(String ticketNumber);

    boolean existsByTicketNumber(String ticketNumber);

    List<WorkItem> findByProjectId(Long projectId);

    List<WorkItem> findByRequirementId(Long requirementId);

    List<WorkItem> findByAssigneeId(Long assigneeId);

    List<WorkItem> findByAssigneeIdAndStatus(Long assigneeId, WorkItemStatus status);

    List<WorkItem> findByProjectIdAndType(Long projectId, WorkItemType type);

    List<WorkItem> findByParentTaskId(Long parentTaskId);

    List<WorkItem> findByStatus(WorkItemStatus status);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.assignee.id = :userId AND w.status NOT IN (com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.COMPLETED, com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.CLOSED)")
    long countActiveByAssignee(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.type = com.aurionpro.ticketboard.workitem.enums.WorkItemType.BUG AND w.status NOT IN (com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.COMPLETED, com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.CLOSED)")
    long countOpenBugs();

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.status = com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.BLOCKED")
    long countBlockedItems();

    @Query("SELECT w FROM WorkItem w WHERE w.status NOT IN (com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.COMPLETED, com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.CLOSED) AND w.dueDate < :now")
    List<WorkItem> findOverdueWorkItems(@Param("now") LocalDate now);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId")
    int countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId AND w.type = :type")
    int countByProjectIdAndType(@Param("projectId") Long projectId, @Param("type") WorkItemType type);

    @Query("SELECT SUM(w.estimatedHours) FROM WorkItem w WHERE w.project.id = :projectId")
    Double sumEstimatedHoursByProject(@Param("projectId") Long projectId);

    @Query("SELECT SUM(w.actualHours) FROM WorkItem w WHERE w.project.id = :projectId")
    Double sumActualHoursByProject(@Param("projectId") Long projectId);

    @Query("SELECT SUM(w.estimatedHours) FROM WorkItem w WHERE w.project.id = :projectId AND w.status IN (com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.COMPLETED, com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.CLOSED)")
    Double sumCompletedEstimatedHoursByProject(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId AND w.parentTask IS NULL")
    int countTopLevelByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId AND w.type = com.aurionpro.ticketboard.workitem.enums.WorkItemType.BUG AND w.status NOT IN (com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.COMPLETED, com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.CLOSED)")
    int countOpenBugsByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId AND w.status NOT IN (com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.COMPLETED, com.aurionpro.ticketboard.workitem.enums.WorkItemStatus.CLOSED)")
    int countOpenByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT w.status, COUNT(w) FROM WorkItem w GROUP BY w.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT w.status, COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId GROUP BY w.status")
    List<Object[]> countByStatusGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT w.priority, COUNT(w) FROM WorkItem w GROUP BY w.priority")
    List<Object[]> countByPriorityGrouped();

    @Query("SELECT w.priority, COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId GROUP BY w.priority")
    List<Object[]> countByPriorityGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT w.type, COUNT(w) FROM WorkItem w GROUP BY w.type")
    List<Object[]> countByTypeGrouped();

    @Query("SELECT w.type, COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId GROUP BY w.type")
    List<Object[]> countByTypeGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT w.severity, COUNT(w) FROM WorkItem w GROUP BY w.severity")
    List<Object[]> countBySeverityGrouped();

    @Query("SELECT w.severity, COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId GROUP BY w.severity")
    List<Object[]> countBySeverityGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT w.assignee.id, COUNT(w) FROM WorkItem w WHERE w.assignee IS NOT NULL GROUP BY w.assignee.id")
    List<Object[]> countByAssigneeGrouped();

    @Query("SELECT w.assignee.id, COUNT(w) FROM WorkItem w WHERE w.project.id = :projectId AND w.assignee IS NOT NULL GROUP BY w.assignee.id")
    List<Object[]> countByAssigneeGroupedForProject(@Param("projectId") Long projectId);

    @Modifying
    @Query("UPDATE WorkItem w SET w.assignee = null WHERE w.assignee.id = :userId")
    void detachAssignee(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE WorkItem w SET w.reporter = null WHERE w.reporter.id = :userId")
    void detachReporter(@Param("userId") Long userId);
}
