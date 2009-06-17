package org.gparallelizer.actors

import org.gparallelizer.actors.pooledActors.Pool

/**
 * The common functionality for all actor groups.
 *
 * @author Vaclav Pech
 */
public abstract class AbstractActorGroup {

    /**
     * Stored the group actors' thread pool
     */
    protected @Delegate Pool threadPool

    /**
     * Indicates whether actors in this group use ForkJoin thread pool
     */
    private final boolean usedForkJoin

    boolean isUsedForkJoin() {usedForkJoin}

    def AbstractActorGroup() { this.usedForkJoin = useFJPool() }

    def AbstractActorGroup(final boolean usedForkJoin) { this.usedForkJoin = usedForkJoin }

    /**
     * Checks the gparallelizer.useFJPool system property and returns, whether or not to use a fork join pool
     */
    private boolean useFJPool() {
        return 'true' == System.getProperty("gparallelizer.useFJPool")?.trim()?.toLowerCase()
    }
}