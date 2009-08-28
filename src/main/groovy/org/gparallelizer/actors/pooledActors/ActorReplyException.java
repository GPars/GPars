package org.gparallelizer.actors.pooledActors;

import java.util.Collections;
import java.util.List;

/**
 * Indicates problems sending replies to actors. Holds a list of exceptions that occured during reply dispatch.
 *
 * @author Vaclav Pech
 * Date: Jun 11, 2009
 */
public final class ActorReplyException extends RuntimeException {
    private final List<Exception> issues;

    public ActorReplyException(final String message) {
        super(message);
        this.issues = Collections.emptyList();
    }

    public ActorReplyException(final String message, final List<Exception> issues) {
        super(message);
        this.issues = Collections.unmodifiableList(issues);
    }

    @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
    public List<Exception> getIssues() {
        return issues;
    }

    @Override public String toString() {
        return super.toString() + getIssues();
    }

    @Override public void printStackTrace() {
        super.printStackTrace();
        for(Exception e : issues) e.printStackTrace();
    }
}
