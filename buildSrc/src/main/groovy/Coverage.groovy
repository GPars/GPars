// GPars - Groovy Parallel Systems
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

/**
 * @author: dierk.koenig at canoo.com
 */
class Coverage {

    final static String saveDir = 'build/classes/plain'
    final static String workDir = 'build/classes/main'

    def ant, test, cp, logger

    Coverage(context) {
        ant = context.ant
        test = context.test
        cp = context.configurations.cover.asPath
        logger = context.logger
        context.test { options.fork() }
    }

    def instrument() {
        ant.taskdef resource: 'tasks.properties', classpath: cp
        log "before instrumenting, save pristine classes"
        ant.delete file: saveDir, failonerror: false
        ant.copy(todir: saveDir) { fileset dir: workDir }
        log "instrument the class files for measuring code coverage"
        ant.delete file: 'cobertura.ser'
        ant.'cobertura-instrument' {
            fileset dir: 'build/classes/main'
        }
        log "done."
    }

    def coverageReport() {
        log "generate the code coverage report"
        ant.'cobertura-report'(destdir: 'build/reports/coverage') {
            fileset dir: 'src/main/groovy'
        }
        log "after generating coverage report, restore pristine classes"
        ant.delete file: workDir, failonerror: false
        ant.mkdir dir: workDir
        ant.move(todir: workDir) { fileset dir: saveDir }
    }

    def setup() {
        log "preparing for test coverage"
        test.doFirst { instrument() }
        test.doLast  { coverageReport() }
    }

    void log(message) {
        logger.info org.gradle.api.logging.Logging.QUIET, "[coverage] $message"
    }
}
