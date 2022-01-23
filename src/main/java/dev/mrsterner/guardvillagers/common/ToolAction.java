package dev.mrsterner.guardvillagers.common;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record ToolAction(String name) {
    private static final Map<String, ToolAction> actions = new ConcurrentHashMap<>();

    public static Collection<ToolAction> getActions() {
        return Collections.unmodifiableCollection(actions.values());
    }

    public static ToolAction get(String name) {
        return actions.computeIfAbsent(name, ToolAction::new);
    }

    @Override
    public String toString() {
        return "ToolAction[" + name + "]";
    }

}