package com.supercraftmc.spadefall.setup;

/**
 * The evaluated state of one setup step.
 *
 * The level drives the glass colour in the GUI and the glyph in chat:
 *   DONE     green   ✔
 *   WARNING  yellow  ⚠   complete, but with something worth knowing
 *   ERROR    red     ✘   missing or broken - the arena cannot run
 *   OPTIONAL grey    ─   not set, and that's fine
 */
public final class StepStatus {

    public enum Level { DONE, WARNING, ERROR, OPTIONAL }

    private final SetupStep step;
    private final Level level;
    private final String summary;
    private final String detail;

    public StepStatus(SetupStep step, Level level, String summary, String detail) {
        this.step = step;
        this.level = level;
        this.summary = summary;
        this.detail = detail;
    }

    public SetupStep getStep() { return step; }
    public Level getLevel() { return level; }
    public String getSummary() { return summary; }
    public String getDetail() { return detail; }

    public boolean blocksRegister() {
        return level == Level.ERROR;
    }

    public boolean isIncomplete() {
        return level == Level.ERROR || level == Level.WARNING;
    }

    public String glyph() {
        return switch (level) {
            case DONE -> "\u00A7a\u2714";
            case WARNING -> "\u00A7e\u26A0";
            case ERROR -> "\u00A7c\u2718";
            case OPTIONAL -> "\u00A78\u2500";
        };
    }

    public String colour() {
        return switch (level) {
            case DONE -> "\u00A7a";
            case WARNING -> "\u00A7e";
            case ERROR -> "\u00A7c";
            case OPTIONAL -> "\u00A77";
        };
    }
}
