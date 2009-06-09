package org.gparallelizer.actors.pooledActors

/**
 *
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public class SingleThreadLoopTest extends LoopTest {

    protected void setUp() {
        super.setUp();
        PooledActors.defaultPooledActorGroup.resize(1)
    }
}