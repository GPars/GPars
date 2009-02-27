package org.gparallelizer.actors.pooledActors

import static org.gparallelizer.actors.pooledActors.PooledActors.getPool

/**
 *
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public class SingleThreadLoopTest extends LoopTest {

    protected void setUp() {
        super.setUp();
        getPool().initialize(1)
    }
}