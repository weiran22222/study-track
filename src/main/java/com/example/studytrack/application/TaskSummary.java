package com.example.studytrack.application;

/** Immutable counts of all, pending, and completed learning tasks. */
public record TaskSummary(long total, long pending, long completed) {}
