/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

ruleset {
    ruleset('rulesets/imports.xml')

    ruleset('rulesets/naming.xml') {
        exclude 'PropertyName'
        exclude 'PackageName'   // TODO is plugAndPlay package correct? 
        'ClassName' {
            regex = '.*'  // '^[A-Z][a-zA-Z0-9]*$'   // TODO: refine correct regex
        }
        'FieldName' {
            finalRegex = '.*' // '^_?[a-z][a-zA-Z0-9]*$'        // TODO: refine correct regex
            staticFinalRegex = '.*' //  '^[A-Z][A-Z_0-9]*$'     // TODO: refine correct regex
        }
        'MethodName' {
            regex = '.*'  // '^[a-z][a-zA-Z0-9_]*$'     // TODO: refine correct regex
        }
        'VariableName' {
            regex = '.*' // '^_?[a-z][a-zA-Z0-9]*$'    // TODO: refine correct regex
            finalRegex = '.*' // '^_?[a-z][a-zA-Z0-9]*$'    // TODO: refine correct regex
        }
    }

    ruleset('rulesets/grails.xml')

    ruleset('rulesets/logging.xml') {
        exclude 'Println'       //TODO: Fix the code and enable rule
        exclude 'PrintStackTrace'    //TODO: Fix the code and enable rule
        exclude 'SystemErrPrint'    //TODO: Fix the code and enable rule
    }

    ruleset('rulesets/braces.xml') {
        exclude 'IfStatementBraces'    // TODO: analyze usage and verify correct code 
        exclude 'WhileStatementBraces'    //TODO: analyze usage and verify correct code 
        exclude 'ElseBlockBraces'    //TODO: analyze usage and verify correct code 
        exclude 'ForStatementBraces'    //TODO: analyze usage and verify correct code 
    }

    ruleset('rulesets/basic.xml') {
        exclude 'StringInstantiation'   //TODO: Fix the code and enable rule
        exclude 'ExplicitHashSetInstantiation'          //TODO: Fix the code and enable rule
        exclude 'ExplicitLinkedListInstantiation'          //TODO: Fix the code and enable rule
        exclude 'ExplicitGarbageCollection'          //TODO: Fix the code and enable rule
        exclude 'ExplicitArrayListInstantiation'          //TODO: Fix the code and enable rule
        exclude 'EmptyCatchBlock'    //TODO: Fix the code and enable rule
        exclude 'EmptyWhileStatement'    //TODO: Fix the code and enable rule
        exclude 'ConfusingTernary'    //TODO: Fix the code and enable rule
        exclude 'ExplicitCallToEqualsMethod'    //TODO: Fix the code and enable rule
        exclude 'ExplicitCallToPlusMethod'    //TODO: Fix the code and enable rule
        exclude 'DeadCode'    //TODO: Fix the code and enable rule
    }

    ruleset('rulesets/size.xml') {
        exclude 'AbcComplexity'    //TODO: Fix the code and enable rule
        exclude 'MethodCount'    //TODO: Fix the code and enable rule
        exclude 'ClassSize'    //TODO: Fix the code and enable rule
        exclude 'NestedBlockDepth'    //TODO: Fix the code and enable rule
    }

    ruleset('rulesets/junit.xml') {
        exclude 'JUnitStyleAssertions'       //TODO: Fix the code and enable rule
        exclude 'JUnitSetUpCallsSuper'       //TODO: Fix the code and enable rule
        exclude 'JUnitTearDownCallsSuper'       //TODO: Fix the code and enable rule
        exclude 'UseAssertTrueInsteadOfAssertEquals' //TODO: Fix the code and enable rule
        exclude 'UseAssertNullInsteadOfAssertEquals' //TODO: Fix the code and enable rule
        exclude 'JUnitPublicNonTestMethod' //TODO: Fix the code and enable rule
        exclude 'JUnitTestMethodWithoutAssert' //TODO: Fix the code and enable rule
        exclude 'UseAssertEqualsInsteadOfAssertTrue' //TODO: Fix the code and enable rule	
        exclude 'JUnitUnnecessarySetUp' //TODO: Fix the code and enable rule	
        exclude 'JUnitUnnecessaryTearDown' //TODO: Fix the code and enable rule	
    }

    ruleset('rulesets/concurrency.xml') {
        exclude 'SynchronizedMethod'         //TODO: Fix the code and enable rule
        exclude 'ThreadYield'         //TODO: Fix the code and enable rule
        exclude 'NestedSynchronization'         //TODO: Fix the code and enable rule
    }

    ruleset('rulesets/unnecessary.xml') {
//        exclude 'UnnecessaryCollectCall'    //TODO: Fix the code and enable rule
//        exclude 'UnnecessaryConstructor'    //TODO: Fix the code and enable rule
//        exclude 'UnnecessaryReturnKeyword'   //TODO: Fix the code and enable rule
        exclude 'UnnecessaryGetter'   //TODO: Fix the code and enable rule
        exclude 'UnnecessaryObjectReferences'   //TODO: this may be a bug in a codenarc rule
        exclude 'UnnecessaryStringInstantiation'   //TODO: Fix the code and enable rule
        exclude 'UnnecessaryOverridingMethod'   //TODO: Fix the code and enable rule
    }

    ruleset('rulesets/dry.xml') {
        exclude 'DuplicateStringLiteral' // TODO: analyze usage and verify correct code
        exclude 'DuplicateNumberLiteral'    //TODO: Fix the code and enable rule
    }

    ruleset('rulesets/design.xml') {
        exclude 'AbstractClassWithoutAbstractMethod' // TODO: analyze usage and verify correct code
        exclude 'FinalClassWithProtectedMember'             // TODO: analyze usage and verify correct code
        exclude 'ImplementationAsType'             // TODO: analyze usage and verify correct code
    }

    ruleset('rulesets/exceptions.xml') {
        exclude 'CatchThrowable'   // TODO: analyze usage and verify correct code
        exclude 'CatchException'    //TODO: Fix the code and enable rule
        exclude 'ThrowRuntimeException'    //TODO: Fix the code and enable rule
        exclude 'ThrowException'    //TODO: Fix the code and enable rule
    }

    ruleset('rulesets/unused.xml') {
        exclude 'UnusedPrivateField'    //TODO: Fix the code and enable rule
        exclude 'UnusedPrivateMethodParameter'   //TODO: Fix the code and enable rule
        exclude 'UnusedPrivateMethod'   //TODO: Fix the code and enable rule
        exclude 'UnusedVariable'        //TODO: Fix the code and enable rule
    }

}
