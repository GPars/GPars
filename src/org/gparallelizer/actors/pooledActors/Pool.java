package org.gparallelizer.actors.pooledActors;

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public interface Pool {
    void initialize(int size);

    void execute(Object task);

    void shutdown();
}
