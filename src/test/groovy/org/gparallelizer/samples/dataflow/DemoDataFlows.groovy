package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*

final df = new DataFlows()

start { df.result = df.x + df.y }

start { df.x = 10 }

start { df.y = 5 }

assert 15 == df.result