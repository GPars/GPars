//Copyright 2008 Zan Thrash
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//
//Modified by Hyuk Don Kwon
//Added few graphing options

package groovyx.gpars.benchmark.caliper.chart

@SuppressWarnings("SpellCheckingInspection")
class GoogleChartBuilder extends BuilderSupport {
    def result = 'http://chart.apis.google.com/chart?' << ''
    def static AMP = '&'
    def static PIPE = '|'
    def static COLON = ':'
    def static FIRST_CHAR = 0
    def encodingKey
    def separator = AMP

    def AZ = ('A'..'Z').toList()
    def az = ('a'..'z').toList()
    def zeroNine = (0..9).toList()
    def c = ['-', '.']
    def all = [AZ, az, zeroNine, c].flatten()
    def extended = createExtendedList(all)
    def simple = all - c

    def createExtendedList(all) {
        def extendedList = []
        all.each { a ->
            all.each { b ->
                extendedList.add(a + b)
            }
        }
        extendedList
    }

    def simpleTranslator = { numberList ->
        def outputString = '' << ''
        numberList.each {
            outputString << simple[round(it)].toString()
        }
        outputString
    }

    def extendedTranslator = { numberList ->
        def outputString = '' << ''
        numberList.each {
            outputString << extended[round(it)].toString()
        }
        outputString
    }

    def textTranslator = { listToString(it) }

    def round(number) { Math.round(number).toInteger() }


    def translators = ['s': simpleTranslator, 'e': extendedTranslator, 't': textTranslator]
    def encodingTranslator

    def size = { args ->
        'chs=' << args.w << 'x' << args.h
    }

    def type = { args ->
        'cht=' << args
    }

    def barType = { args ->
        def style = 'b' << ''
        args.each {
            style << it[FIRST_CHAR]
        }
        return 'cht=' << style
    }

    def rowClosure = { text ->
        replaceSpaces('+', text.toString())
    }

    def setClosure = { args ->
        encodingTranslator.call(args)
    }

    def colorClosure = { color ->
        color
    }

    def toAxisLabelList = { list ->
        def t = '' << ''
        list.each { t << PIPE << it }
        t << PIPE
    }


    def row = getCounter(0).curry(rowClosure)

    def textSet = getCounter(0).curry(setClosure)
    def extendedSet = getCounter(0, ',').curry(setClosure)

    def label = getCounter(0).curry(rowClosure)

    def color = getCounter(0, ',').curry(colorClosure)

    def data = { args ->
        encodingKey = args.encoding[FIRST_CHAR].toLowerCase()
        encodingTranslator = translators[encodingKey]
        'chd=' << encodingKey << args.numLines << COLON
    }


    def title = { colorSizeMap ->
        def t = '' << ''
        if (colorSizeMap) {
            //noinspection SpellCheckingInspection
            t << 'chts=' << colorSizeMap.color << ',' << colorSizeMap.size << AMP
        }
        //noinspection SpellCheckingInspection
        t << 'chtt='
    }

    def axis = { labelMap ->
        def output = axisTypeString(labelMap)
        def labels = extractLabelsFrom(labelMap)
        output << axisLabelString(labels)
    }

    def line_style = { styleMap ->
        def output = "chls=" << ''
        styleMap.each { key, value -> output << listToString(value) << PIPE }
        removeLastPipe(output)
    }

    def axis_optionClosure = { type, mapOfPoints ->
        def s = '' << ''
        mapOfPoints.each { index, listOfPoints ->
            s << index << ',' << listToString(listOfPoints) << PIPE
        }
        return removeLastPipe(s)
    }

    def axis_option = getAxisCounter(0).curry(axis_optionClosure)
    def axis_position = getAxisCounter(0).curry(axis_optionClosure, 'chxp')
    def axis_range = getAxisCounter(0).curry(axis_optionClosure, 'chxr')

    def axis_style = getAxisCounter(0).curry(axis_optionClosure, 'chxs')

    def data_range = {
        def s = '' << ''
        s << "chds=" << it
    }

    def removeLastPipe(stringWithPipe) {
        def result
        stringWithPipe.size() > 0 ? result = stringWithPipe[0..-2] : (result = stringWithPipe)
        result
    }

    def markerTypes = ['ar': 'a', 'cr': 'c', 'di': 'd', 'ci': 'o', 'sq': 's', 'vp': 'v', 'vt': 'V', 'ho': 'h', 'xs': 'x']


    def shapeClosure = { map ->
        def output = '' << ''
        map.type ? output << markerTypes[map.type[0..1].toLowerCase()] << ',' : output
        map.color ? output << map.color << ',' : output
        map.set >= 0 ? output << map.set << ',' : output
        map.point >= 0.0 ? output << map.point << ',' : output
        map.size ? output << map.size : output
        output
    }
    def shape = getCounter(0).curry(shapeClosure)

    def rangeMarkerClosure = { map ->
        def types = ['h': 'r', 'v': 'R']
        def output = '' << ''
        map.type ? output << types[map.type[0].toLowerCase()] << ',' : output
        map.color ? output << map.color << ',0,' : output
        map.start >= 0 ? output << map.start << ',' : output << '0.00' << ','
        map.end >= 0 ? output << map.end : output << '1.00'
        output
    }

    def rangeMarker = getCounter(0).curry(rangeMarkerClosure)

    def fill = { map ->
        separator = ''
        def output = '' << ''
        map.single ? output << 'B,' : output << 'b,'
        map.color ? output << map.color << ',' : output
        map.startLine ? output << map.startLine << ',' : output << '0,'
        map.endLine ? output << map.endLine << ',' : output << '0,'
        output << '0|'
    }
    def labelOption = { arg ->
        def p = '' << ''
        p << "chdlp=" << arg
    }
    def solidClosure = { map ->
        def output = "s" << ''
        map.color ? output << ',' << map.color : output
        output
    }

    def solid = getCounter(0).curry(solidClosure)

    def gradientClosure = { map ->
        def output = 'lg' << ''
        map.angle >= 0 ? output << ',' << map.angle : output << ',0'
        map.start ? output << ',' << map.start << ',0' : output << ',ffffff,0'
        map.end ? output << ',' << map.end << ',1' : output << ',efefef,1'
        output
    }

    def gradient = getCounter(0).curry(gradientClosure)

    def stripes = { map ->
        separator = ''
        def output = 'ls' << ''
        map.angle >= 0 ? output << ',' << map.angle : output << ',0'
    }

    def stripe = { map ->
        separator = ''
        def output = '' << ''
        map.color ? output << ',' << map.color : output
        map.width ? output << ',' << map.width : output
        output
    }

    def background = {
        result.toString().contains('chf=a') ? separator = PIPE : (separator = '')
        'bg,'
    }

    def area = {
        result.toString().contains('chf=bg') ? separator = PIPE : (separator = '')
        'c,'
    }

    def barSize = { args ->
        def output = 'chbh='
        output << listToString(args.values())
    }

    def axisTypeString(map) {
        def translate = ['right': 'r', 'bottom': 'x', 'bottom2': 'x', 'top': 't', 'left': 'y', 'left2': 'y']
        def output = 'chxt=' << ''
        def tempList = []
        map.each { k, v ->
            tempList << translate[k]
        }
        output << listToString(tempList)
    }

    def extractLabelsFrom(labelMap) {
        def tempMap = [:]
        labelMap.eachWithIndex { entry, i ->
            if (entry.value) {
                tempMap[i] = entry.value
            }
        }
        tempMap
    }

    def axisLabelString(labels) {
        def output = '' << ""
        if (mapHasAnyLists(labels)) {
            output << AMP << "chxl="
            labels.each { k, v ->
                output << k << ':' << toAxisLabelList.call(v)
            }
        }
        removeLastPipe(output)
    }

    def mapHasAnyLists(map) {
        def result = false
        map.values().each {
            if (it.size() > 0) result = true
        }
        result
    }

    def grid = { map ->
        def output = "chg=" << map.x << "," << map.y
        map.dash ? output << "," << map.dash : output
        map.space ? output << "," << map.space : output
        output
    }

    def listToString(list) {
        replaceSpaces('', list.toString()[1..-2])
    }

    def chartTypes = ['pie': type('p'), 'pie3d': type('p3'), 'line': type('lc'), 'lineXY': type('lxy'),
            'bar': barType, 'venn': type('v'), 'scatter': type('s')]

    def required = ['size': size, 'data': data, 'tSet': textSet, 'eSet': extendedSet, 'sSet': extendedSet]

    def optional = ['title': title, 'label': label, 'row': row, 'legend': 'chdl=', 'labels': 'chl=',
            'axis': axis, 'position': axis_position, 'range': axis_range,
            'style': axis_style, 'lineStyle': line_style, 'grid': grid, 'markers': 'chm=',
            'shape': shape, 'rangeMarker': rangeMarker, 'fill': fill, 'backgrounds': 'chf=',
            'background': background, 'area': area, 'solid': solid, 'gradient': gradient,
            'stripes': stripes, 'stripe': stripe, colors: 'chco=', 'color': color, 'barSize': barSize, 'dataRange': data_range, 'labelOption': labelOption]

    def closureDictionary = chartTypes + required + optional

    def createNode(name) {
        def output = closureDictionary[name]
        if (closureDictionary[name] instanceof Closure) {
            output = closureDictionary[name].call()
        }
        return check(output)
    }

    def createNode(name, value) {
        if (name == 'set') {
            name = "${encodingKey}Set"
        }
        return check(closureDictionary[name] ? closureDictionary[name].call(value) : name)
    }

    def createNode(name, Map attributes) {
        return check(closureDictionary[name].call(attributes))
    }

    def createNode(name, Map attributes, value) {
        return name
    }

    void setParent(parent, child) {
        result << separator << child
        separator = AMP
    }

    void nodeCompleted(parent, node) {
    }

    def check(name) {
        if (!current) {
            return result << name
        }
        return name
    }

    def replaceSpaces(replacement, string) {
        string.replaceAll(/\s/) { replacement }
    }

    def getCounter(count, delimiter = PIPE) {
        return { closure, args ->
            count > 0 ? separator = delimiter : (separator = '')
            count++
            closure(args).toString()
        }
    }

    def getAxisCounter(count) {
        return { closure, type, map ->
            count > 0 ? separator = PIPE : (separator = "&${type}=")
            count++
            closure(type, map).toString()
        }
    }
}


