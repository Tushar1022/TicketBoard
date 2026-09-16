package com.aurionpro.ticketboard.timetracking.repository;

import com.aurionpro.ticketboard.timetracking.entity.TimeEntry;
import com.aurionpro.ticketboard.timetracking.enums.TimeEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByUserIdOrderByWorkDateDesc(Long userId);

    List<TimeEntry> findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    List<TimeEntry> findByProjectId(Long projectId);

    List<TimeEntry> findByRequirementId(Long requirementId);

    List<TimeEntry> findByWorkItemId(Long workItemId);

    List<TimeEntry> findByTimesheetId(Long timesheetId);

    List<TimeEntry> findByStatus(TimeEntryStatus status);

    @Query("SELECT SUM(t.totalHours) FROM TimeEntry t WHERE t.user.id = :userId AND t.workDate BETWEEN :startDate AND :endDate")
    Double sumHoursByUserAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(t.totalHours) FROM TimeEntry t WHERE t.project.id = :projectId")
    Double sumHoursByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT SUM(t.totalHours) FROM TimeEntry t WHERE t.requirement.id = :reqId")
    Double sumHoursByRequirementId(@Param("reqId") Long reqId);

    @Query("SELECT SUM(t.totalHours) FROM TimeEntry t WHERE t.workItem.id = :workItemId")
    Double sumHoursByWorkItemId(@Param("workItemId") Long workItemId);

    @Query("SELECT SUM(t.totalHours) FROM TimeEntry t")
    Double sumTotalHoursLogged();

    @Query("SELECT t.workDate, SUM(t.totalHours) FROM TimeEntry t GROUP BY t.workDate ORDER BY t.workDate")
    List<Object[]> hoursGroupedByDate();

    @Query("SELECT t.workDate, SUM(t.totalHours) FROM TimeEntry t WHERE t.project.id = :projectId GROUP BY t.workDate ORDER BY t.workDate")
    List<Object[]> hoursGroupedByDateForProject(@Param("projectId") Long projectId);

    @Query("SELECT t.status, COUNT(t) FROM TimeEntry t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT t.status, COUNT(t) FROM TimeEntry t WHERE t.project.id = :projectId GROUP BY t.status")
    List<Object[]> countByStatusGroupedForProject(@Param("projectId") Long projectId);

    @Query("SELECT t.user.id, SUM(t.totalHours) FROM TimeEntry t WHERE t.user IS NOT NULL GROUP BY t.user.id")
    List<Object[]> hoursGroupedByUser();

    @Query("SELECT t.user.id, SUM(t.totalHours) FROM TimeEntry t WHERE t.project.id = :projectId AND t.user IS NOT NULL GROUP BY t.user.id")
    List<Object[]> hoursGroupedByUserForProject(@Param("projectId") Long projectId);
}
