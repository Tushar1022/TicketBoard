package com.aurionpro.ticketboard.timetracking.repository;

import com.aurionpro.ticketboard.timetracking.entity.Timesheet;
import com.aurionpro.ticketboard.timetracking.enums.TimesheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    List<Timesheet> findByUserIdOrderByStartDateDesc(Long userId);

    Optional<Timesheet> findByUserIdAndStartDateAndEndDate(Long userId, LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByStatus(TimesheetStatus status);

    List<Timesheet> findByReviewerId(Long reviewerId);
}
