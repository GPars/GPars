package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*

final df = new DataFlows()

start { df.result = df.x + df.y }

start { df.x = 10 }

start { df.y = 5 }

assert 15 == df.result

start { df [0] = df[2] + df[1] }

start { df [1] = 10 }

start { df [2] = 5 }

assert 15 == df[0]
