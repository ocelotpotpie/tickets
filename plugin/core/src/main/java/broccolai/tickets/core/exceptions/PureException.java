package broccolai.tickets.core.exceptions;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class PureException extends RuntimeException {

    private static final long serialVersionUID = -1L;

    private final Component component;

    /**
     * Initialise a localised PureException
     *
     * @param component Component to send
     */
    public PureException(final @NonNull Component component) {
        this.component = component;
    }

    /**
     * @return Get component
     */
    public final @NonNull Component getComponent() {
        return this.component;
    }

}
