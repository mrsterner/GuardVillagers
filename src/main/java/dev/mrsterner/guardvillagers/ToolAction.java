package dev.mrsterner.guardvillagers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToolAction {
    private static final Map<String, ToolAction> actions = new ConcurrentHashMap<>();

    public static Collection<ToolAction> getActions() {
        return Collections.unmodifiableCollection(actions.values());
    }

    /**
     * Gets or creates a new ToolAction for the given name.
     */
    public static ToolAction get(String name) {
        return actions.computeIfAbsent(name, ToolAction::new);
    }

    /**
     * Returns the name of this tool action
     */
    public String name()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "ToolAction[" + name + "]";
    }

    private final String name;

    /**
     * Use {@link #get(String)} to get or create a ToolAction
     */
    private ToolAction(String name)
    {
        this.name = name;
    }
}