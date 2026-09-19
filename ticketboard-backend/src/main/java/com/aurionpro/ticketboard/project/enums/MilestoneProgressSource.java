package com.aurionpro.ticketboard.project.enums;

/**
 * Determines how a milestone's completion percentage is derived.
 * AUTO recomputes the percentage from linked work items at read time;
 * MANUAL keeps the user-entered value.
 */
public enum MilestoneProgressSource {
    AUTO,
    MANUAL
}