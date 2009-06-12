package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.pooledActors.PooledActors

/**
 *
 * @author Vaclav Pech
 * Date: Feb 20, 2009
 */
public class SingleThreadedLifecycleTest extends LifecycleTest {

    protected void setUp() {
        super.setUp();
        PooledActors.defaultPooledActorGroup.resize(1)
    }


    protected void tearDown() {
        super.tearDown();
        PooledActors.defaultPooledActorGroup.resize(5)
    }
}