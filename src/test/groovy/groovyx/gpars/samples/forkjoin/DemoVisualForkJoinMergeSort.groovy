// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.forkjoin

import groovy.swing.SwingBuilder

import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import java.awt.*
import java.awt.BorderLayout as BL
import java.util.List

import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

/**
 * Shows use of the ForkJoin mechanics to implement merge sort.
 *
 * Author: Vaclav Pech, Lukas Krecan, Pavel Jetensky
 */

/**
 Fork/Join operations can be safely run with small number of threads thanks to using the TaskBarrier class to synchronize the threads.
 Although the algorithm creates tasks for all fractions of the original list and tasks wait for the sub-fractions' tasks to complete,
 as few as one thread is enough to keep the computation going.
 */
ROW_HEIGHT = 40
COL_WIDTH = 35
COLOR_WAIT = new Color(212, 146, 52)
COLOR_SCHEDULED = new Color(134, 219, 52)
COLOR_FINISHED = Color.GRAY

swing = new SwingBuilder()

threadColors = [Color.YELLOW, new Color(76, 160, 255), Color.CYAN, Color.MAGENTA, Color.GREEN, Color.PINK, Color.ORANGE, Color.WHITE]

panel = new JPanel();
panel.setLayout(null)

//Creates Number of threads slider
numThreads = swing.slider(value: 3, minimum: 1, maximum: 8, paintTicks: true, paintLabels: true, minorTickSpacing: 1)
numThreads.border = swing.titledBorder("""Number of threads ${numThreads.value}""")
numThreads.addChangeListener(new ChangeListener() {
    void stateChanged(ChangeEvent event) {
        event.source.border.title = """Number of threads ${event.source.value}"""
    }
})

//Creates Problem Size slider
problemSize = swing.slider(value: 32, minimum: 4, maximum: 64, paintTicks: true, paintLabels: true, minorTickSpacing: 1)
problemSize.border = BorderFactory.createTitledBorder("""Problem size ${problemSize.value}""")
problemSize.addChangeListener(new ChangeListener() {
    void stateChanged(ChangeEvent event) {
        event.source.border.title = """Problem size ${event.source.value}"""
    }
})

//Creates Speed slider
speed = swing.slider(minimum: 0, maximum: 1000, value: 700, border: BorderFactory.createTitledBorder("Speed"))

//Creates Start Button
startButton = swing.button(text: 'Start', actionPerformed: { event ->
    numThreads.enabled = false
    problemSize.enabled = false
    def ps = problemSize.value
    event.source.enabled = false;
    panel.preferredSize = [ps * COL_WIDTH, 7 * ROW_HEIGHT]
    panel.removeAll();
    Thread.start { runDemo() }
})
//Creates frame
swing.edt {
    frame(title: "Visualisation of merge sort using fork join",
            defaultCloseOperation: JFrame.EXIT_ON_CLOSE,
            size: [1024, 640],
            show: true) {
        borderLayout()
        vbox(constraints: BL.NORTH) {
            hbox()
                    {
                        label("<html>\
					 	<ol>\
							 <li>Thread takes a task from the queue. If the tasks is too big (longer than two elements) it is split to two smaller tasks</li>\
							 <li>Sub-tasks are placed to queue to be processed</li>\
						 	 <li>While the task waits for its sub-tasks to finish the thread goes to 1.</li>\
						     <li>When the sub-tasks are finished their results are merged.</li>\
						 </ol>\
					 </html>")
                    }
            panel()
                    {
                        label(text: "Waiting in queue", background: COLOR_SCHEDULED, opaque: true, border: emptyBorder(3, 3, 3, 3));
                        label(text: "Waiting for sub-tasks", background: COLOR_WAIT, opaque: true, border: emptyBorder(3, 3, 3, 3));
                        label(text: "Finished", background: COLOR_FINISHED, opaque: true, border: emptyBorder(3, 3, 3, 3));
                        threadColors.eachWithIndex { color, idx ->
                            label(text: "Thread ${idx + 1}", background: color, opaque: true, border: emptyBorder(3, 3, 3, 3));
                        }
                    }
            hbox()
                    {
                        widget(speed)
                        widget(numThreads)
                        widget(problemSize)
                    }
            hbox()
                    {
                        widget(startButton)
                    }

        }
        scrollPane(viewportView: panel, constraints: BL.CENTER)
    }.setExtendedState(JFrame.MAXIMIZED_BOTH)
}

/**
 * Creates label that visualizes the task
 * @param row
 * @param col
 * @param nums
 * @return
 */
def createLabel(row, col, nums) {
    final label = swing.label(
            text: " " + nums,
            bounds: [col * COL_WIDTH, row * ROW_HEIGHT + 20, nums.size() * COL_WIDTH, ROW_HEIGHT],
            background: COLOR_SCHEDULED,
            opaque: true,
            toolTipText: nums.toString(),
            border: BorderFactory.createLineBorder(Color.BLACK)
    )
    threadSafe {
        panel.add(label)
    }
    return label
}

/**
 * Sets color of the label
 * @param label
 * @param color
 * @return
 */
def setLabelColor(label, color) {
    threadSafe { label.setBackground(color) }
}

/**
 * Runs closure in Swing Event thread and repaints the panel. Sleeps after the change.
 * @param closure
 * @return
 */
def threadSafe(closure) {
    SwingUtilities.invokeAndWait {
        closure()
        panel.repaint()
    }
    Thread.sleep(1000 - speed.value)
}

/**
 * Splits a list of numbers in half
 */
def split(List<Integer> list) {
    int listSize = list.size()
    int middleIndex = listSize / 2
    def list1 = list[0..<middleIndex]
    def list2 = list[middleIndex..listSize - 1]
    return [list1, list2]
}

/**
 * Merges two sorted lists into one
 */
List<Integer> merge(label, List<Integer> a, List<Integer> b) {
    setLabelColor(label, threadColor())
    int i = 0, j = 0
    final int newSize = a.size() + b.size()
    List<Integer> result = new ArrayList<Integer>(newSize)

    while ((i < a.size()) && (j < b.size())) {
        if (a[i] <= b[j]) result << a[i++]
        else result << b[j++]
    }

    if (i < a.size()) result.addAll(a[i..-1])
    else result.addAll(b[j..-1])
    return result
}

/**
 * Returns color of current thread.
 */
def threadColor() {
    return threadColors[Integer.valueOf(Thread.currentThread().name[-1]) - 1]
}

/**
 * Executes the demo.
 * @return
 */
def runDemo() {
    final def numbers = new ArrayList(problemSize.value..1)
    withPool(numThreads.value) {  //feel free to experiment with the number of fork/join threads in the pool
        def topLabel = createLabel(0, 0, numbers)
        def sorted = runForkJoin(numbers, 0, 0, topLabel) { nums, row, column, label ->
            println "Thread ${Thread.currentThread().name[-1]}: Sorting $nums"
            def colorIndex =
                    setLabelColor(label, threadColor())
            switch (nums.size()) {
                case 0..1:
                    return finishTask(label, nums)                                   //store own result
                case 2:

                    if (nums[0] <= nums[1]) {
                        return finishTask(label, nums)     //store own result
                    } else {
                        return finishTask(label, nums[-1..0])   //store own result
                    }
                default:
                    def splitList = split(nums)
                    def label1 = createLabel(row + 1, column, splitList[0])
                    def label2 = createLabel(row + 1, column + splitList[0].size(), splitList[1])
                    setLabelColor(label, COLOR_WAIT)
                    forkOffChild splitList[0], row + 1, column, label1
                    final def result = runChildDirectly(splitList[1], row + 1, column + splitList[0].size(), label2)
                    return finishTask(label, merge(label, result, * childrenResults));       //use results of children tasks to calculate and store own result

            }
        }
        threadSafe {
            numThreads.enabled = true
            problemSize.enabled = true
            startButton.enabled = true
        }
        println """Sorted numbers: ${sorted}"""
    }
}

/**
 * Finishes the task
 * @param label
 * @param result
 * @return
 */
def finishTask(label, result) {
    threadSafe {
        label.text = result.toString()
        label.background = COLOR_FINISHED
    }
    return result
}

