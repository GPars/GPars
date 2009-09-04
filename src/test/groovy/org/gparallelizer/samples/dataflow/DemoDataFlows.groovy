package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*

final df = new DataFlows()

thread { df.result = df.x + df.y }

thread { df.x = 10 }

thread { df.y = 5 }

assert 15 == df.result