package org.gparallelizer.actors.pooledActors
/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Feb 18, 2009
 */

import static org.gparallelizer.actors.pooledActors.PooledActors.getPool

public class SingleThreadLoopTest extends LoopTest {

    protected void setUp() {
        super.setUp();
        getPool().initialize(1)
    }
}