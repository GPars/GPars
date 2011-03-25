// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

/**
 * Shows use of the ForkJoin mechanics to implement merge sort.
 *
 * Author: Pavel Jetensky, Lukas Krecan, Vaclav Pech
 */

/**
 Fork/Join operations can be safely run with small number of threads thanks to using the TaskBarrier class to synchronize the threads.
 Although the algorithm creates tasks for all fractions of the original list and tasks wait for the sub-fractions' tasks to complete,
 as few as one thread is enough to keep the computation going.
 */
panel = new JPanel();
panel.setLayout(null)
threadColors = [Color.YELLOW, Color.BLUE, Color.CYAN]

final JFrame frame = new JFrame();
frame.setTitle('Visualisation of merge sort using fork join')
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
frame.add(panel)
frame.visible = true
frame.setSize(1024, 640)

def createLabel(row, col, nums) {
    final JLabel label = new JLabel(nums.toString())
    SwingUtilities.invokeAndWait {
        def ROW_HEIGHT = 40
        def COL_WIDTH = 30

        label.setBounds(col * COL_WIDTH, 100 + row * ROW_HEIGHT, nums.size() * COL_WIDTH, ROW_HEIGHT)
        label.setBackground(Color.GREEN)
        label.opaque = true
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK))

        panel.add(label)
        panel.revalidate()
        panel.repaint()
    }
    println("create: ${label.text}  ${nums}  Thread ${Thread.currentThread().name[-1]}")
    return label


}

def setLabelColor(label, color) {
    println("setLabelColor: ${label.text}  ${color}  Thread ${Thread.currentThread().name[-1]}")
    SwingUtilities.invokeAndWait {
        label.setBackground(color)
        panel.revalidate()
        panel.repaint()
    }
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
List<Integer> merge(List<Integer> a, List<Integer> b) {
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

final def numbers = [32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1]

withPool(1) {  //feel free to experiment with the number of fork/join threads in the pool
    displayColorLegend()
    def topLabel = createLabel(0, 0, numbers)
    println """Sorted numbers: ${
        runForkJoin(numbers, 0, 0, topLabel) {nums, row, column, label ->
            println "Thread ${Thread.currentThread().name[-1]}: Sorting $nums"
            def colorIndex = Integer.valueOf(Thread.currentThread().name[-1])
            setLabelColor(label, threadColors[colorIndex])
            Thread.sleep(3000)
            switch (nums.size()) {
                case 0..1:
                    finishStep(label, nums)
                    return nums                                   //store own result
                case 2:

                    if (nums[0] <= nums[1]) {
                        finishStep(label, nums)
                        return nums     //store own result
                    }
                    else {
                        finishStep(label, nums[-1..0])
                        return nums[-1..0]                       //store own result
                    }
                default:
                    def splitList = split(nums)
                    def label1 = createLabel(row + 1, column, splitList[0])
                    def label2 = createLabel(row + 1, column + splitList[0].size(), splitList[1])
                    setLabelColor(label, Color.MAGENTA)
                    forkOffChild splitList[0], row + 1, column, label1
                    forkOffChild splitList[1], row + 1, column + splitList[0].size(), label2
                    result = merge(* childrenResults)
                    finishStep(label, result)
                    return result;       //use results of children tasks to calculate and store own result

            }
        }
    }"""
}

def finishStep(final label, final result) {
    SwingUtilities.invokeAndWait {
        label.setText(result.toString())
        panel.revalidate()
        panel.repaint()
    }
    setLabelColor(label, Color.GRAY)
}

def displayColorLegend() {

}

