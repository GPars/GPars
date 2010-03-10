// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
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

package c14

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.awt.*
import org.jcsp.util.*
import phw.util.*

def delay = Ask.Int("Target visible period (2000 to 3500)?  ", 2000, 3500)

def targets = 16
def targetOrigins = [[10, 10], [120, 10], [230, 10], [340, 10],
        [10, 120], [120, 120], [230, 120], [340, 120],
        [10, 230], [120, 230], [230, 230], [340, 230],
        [10, 340], [120, 340], [230, 340], [340, 340]]

def setUpBarrier = new Barrier(targets + 5)
def initBarrier = new Barrier()
def goBarrier = new Barrier(3)
AltingBarrier[] timeAndHitBarrier = AltingBarrier.create(targets + 2)
AltingBarrier[] finalBarrier = AltingBarrier.create(2)
def buckets = Bucket.create(targets)

def mouseEvent = Channel.createOne2One(new OverWriteOldestBuffer(20))
def requestPoint = Channel.createOne2One()
def receivePoint = Channel.createOne2One()
def pointToTC = Channel.createOne2One(new OverWriteOldestBuffer(1))

def targetsFlushed = Channel.createOne2One()
def flushNextBucket = Channel.createOne2One()

def targetsActivated = Channel.createOne2One()
def targetsActivatedToDC = Channel.createOne2One()
def getActiveTargets = Channel.createOne2One()

def hitsToGallery = Channel.createOne2One()
def possiblesToGallery = Channel.createOne2One()

def targetIdToManager = Channel.createAny2One()
def targetStateToDC = Channel.createAny2One()

One2OneChannel[] mousePointToTP = Channel.createOne2One(targets)
def mousePoints = new ChannelOutputList(mousePointToTP)

def imageList = new DisplayList()
def targetCanvas = new ActiveCanvas()
targetCanvas.setPaintable(imageList)

def targetList = (0..<targets).collect {i ->
    return new TargetProcess(
            targetRunning: targetIdToManager.out(),
            stateToDC: targetStateToDC.out(),
            mousePoint: mousePointToTP[i].in(),
            setUpBarrier: setUpBarrier,
            initBarrier: initBarrier,
            goBarrier: goBarrier,
            timeAndHitBarrier: timeAndHitBarrier[i],
            buckets: buckets,
            targetId: i,
            x: targetOrigins[i][0],
            y: targetOrigins[i][1],
            delay: delay
    )
}

def barrierManager = new BarrierManager(
        timeAndHitBarrier: timeAndHitBarrier[targets],
        finalBarrier: finalBarrier[0],
        goBarrier: goBarrier,
        setUpBarrier: setUpBarrier
)

def targetController = new TargetController(
        getActiveTargets: getActiveTargets.out(),
        activatedTargets: targetsActivated.in(),
        receivePoint: pointToTC.in(),
        sendPoint: mousePoints,
        setUpBarrier: setUpBarrier,
        goBarrier: goBarrier,
        timeAndHitBarrier: timeAndHitBarrier[targets + 1]
)

def galleryProcess = new Gallery(
        targetCanvas: targetCanvas,
        hitsFromGallery: hitsToGallery.in(),
        possiblesFromGallery: possiblesToGallery.in(),
        mouseEvent: mouseEvent.out()
)

def flusher = new TargetFlusher(
        buckets: buckets,
        targetsFlushed: targetsFlushed.out(),
        flushNextBucket: flushNextBucket.in(),
        initBarrier: initBarrier
)

def mouseBuffer = new MouseBufferPreCon(
        mouseEvent: mouseEvent.in(),
        getClick: requestPoint.in(),
        sendPoint: receivePoint.out()
)

def mouseBufferPrompt = new MouseBufferPrompt(
        returnPoint: pointToTC.out(),
        getPoint: requestPoint.out(),
        receivePoint: receivePoint.in(),
        setUpBarrier: setUpBarrier
)

def targetManager = new TargetManager(
        targetIdFromTarget: targetIdToManager.in(),
        getActiveTargets: getActiveTargets.in(),
        activatedTargets: targetsActivated.out(),
        activatedTargetsToDC: targetsActivatedToDC.out(),
        targetsFlushed: targetsFlushed.in(),
        flushNextBucket: flushNextBucket.out(),
        setUpBarrier: setUpBarrier
)

def displayControl = new DisplayController(
        stateChange: targetStateToDC.in(),
        activeTargets: targetsActivatedToDC.in(),
        displayList: imageList,
        hitsToGallery: hitsToGallery.out(),
        possiblesToGallery: possiblesToGallery.out(),
        setUpBarrier: setUpBarrier,
        goBarrier: goBarrier,
        finalBarrier: finalBarrier[1]
)

def procList = targetList +
        mouseBuffer +
        mouseBufferPrompt +
        targetManager +
        galleryProcess +
        displayControl +
        flusher +
        targetController +
        barrierManager

new PAR(procList).run()

