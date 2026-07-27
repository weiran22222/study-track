package com.example.studytrack.domain;

/** A learning task tracked by StudyTrack. */
public record StudyTask(long id, String title, boolean completed) {}
