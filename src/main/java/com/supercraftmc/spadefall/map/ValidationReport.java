package com.supercraftmc.spadefall.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of validating a map.
 *
 * The distinction matters. ERRORS are structural - the map cannot function and
 * is refused. WARNINGS are judgement calls, and the owner is always allowed to
 * override them: the design brief was explicitly "let the user continue with
 * whatever they want".
 */
public final class ValidationReport {

    private final String mapName;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public ValidationReport(String mapName) {
        this.mapName = mapName;
    }

    public void error(String message) {
        errors.add(message);
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public String getMapName() { return mapName; }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public boolean isClean() { return errors.isEmpty() && warnings.isEmpty(); }

    /** True when the owner should be asked to confirm before continuing. */
    public boolean needsConfirmation() {
        return !hasErrors() && hasWarnings();
    }
}
