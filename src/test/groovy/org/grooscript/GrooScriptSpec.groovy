/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grooscript

import org.grooscript.util.GsConsole
import spock.lang.Specification
import spock.lang.Unroll

import static org.grooscript.util.Util.LINE_SEPARATOR as LS

class GrooScriptSpec extends Specification {

    def 'default options'() {
        expect:
        GrooScript.defaultConversionOptions == [
                classpath: null,
                customization: null,
                mainContextScope: null,
                initialText: null,
                finalText: null,
                addGsLib: null,
                recursive: false,
                requireJsModule: false,
                consoleInfo: false,
                includeDependencies: false,
                nashornConsole: false
        ]
    }

    @Unroll
    def 'convert some groovy files to one .js file'() {
        given:
        GrooScript.convert(SOURCES_FOLDER, destinationFile, [classpath: SOURCES_CLASSPATH])

        expect:
        new File(destinationFile).exists()

        cleanup:
        new File(destinationFile).delete()
        new File(DEST_FOLDER).deleteDir()

        where:
        destinationFile << [BIG_JS_FILE, "$DEST_FOLDER/$BIG_JS_FILE"]
    }

    @Unroll
    def 'convert some files to one file'() {
        given:
        GrooScript.convert([new File(SOURCES_FOLDER)], new File(destinationFile), [classpath: SOURCES_CLASSPATH])

        expect:
        new File(destinationFile).exists()

        cleanup:
        new File(destinationFile).delete()
        new File(DEST_FOLDER).deleteDir()

        where:
        destinationFile << [BIG_JS_FILE, "$DEST_FOLDER/$BIG_JS_FILE"]
    }

    def 'convert some groovy files to one folder that not exists'() {
        given:
        GrooScript.convert(SOURCES_FOLDER, DEST_FOLDER, [classpath: SOURCES_CLASSPATH])

        expect:
        new File(DEST_FOLDER).exists()
        new File(SOURCES_FOLDER).listFiles().count { it.file } == new File(DEST_FOLDER).listFiles().count {
            it.file && it.name.endsWith('.js')
        }

        cleanup:
        new File(DEST_FOLDER).deleteDir()
    }

    def 'not repeating js conversion options when converting to a file'() {
        given:
        def testCount = 0
        def initialTextCount = 0
        def finalTextCount = 0
        def options = [classpath: SOURCES_CLASSPATH, initialText: INITIAL, finalText: FINAL, addGsLib: 'testWithNode']
        GrooScript.convert(SOURCES_FOLDER, BIG_JS_FILE, options)

        when:
        new File(BIG_JS_FILE).eachLine { line ->
            if (line.startsWith('var gs = require(\'./grooscript.js\');')) {
                testCount++
            }
            if (line.startsWith(INITIAL)) {
                initialTextCount++
            }
            if (line.startsWith(FINAL)) {
                finalTextCount++
            }
        }

        then:
        testCount == 1
        initialTextCount == 1
        finalTextCount == 1

        cleanup:
        new File(BIG_JS_FILE).delete()
    }

    def 'evaluate js code'() {
        when:
        def testResult = GrooScript.evaluateGroovyCode('println "Hello!"')

        then:
        testResult.console == 'Hello!'
        testResult.jsCode == 'gs.println("Hello!");' + LS
        !testResult.exception
        !testResult.assertFails
    }

    @Unroll
    def 'evaluate js code another gs lib'() {
        when:
        def testResult = GrooScript.evaluateGroovyCode('println "Hello!"', libs)

        then:
        testResult.console == 'Hello!'
        testResult.jsScript.contains 'Apache License, Version 2.0'

        where:
        libs << ['grooscript', 'grooscript, grooscript.min']
    }

    @Unroll
    def 'convert to groovy and javascript does nothing'() {
        given:
        def data = testData

        expect:
        GrooScript.toGroovy(data) == data
        GrooScript.toJavascript(data) == data
        GrooScript.toJsObj(data) == data

        where:
        testData << [null, '', 'hello', 55, [1, 2, 3], [one: 1, two: 2], 0, false]
    }

    @Unroll
    def 'convert function #nameFunc generates js code'() {
        given:
        def code = """
import org.grooscript.GrooScript

GrooScript.${nameFunc}('hello')
"""
        expect:
        GrooScript.convert(code) == "gs.${nameFunc}(\"hello\");" + LS

        where:
        nameFunc << ['toJavascript', 'toGroovy', 'toJsObj']
    }

    @Unroll
    def 'convert function #nameFunc generates js code with import static'() {
        given:
        def code = """
import static org.grooscript.GrooScript.${nameFunc}

${nameFunc}('hello')
"""
        expect:
        GrooScript.convert(code) == "gs.${nameFunc}(\"hello\");" + LS

        where:
        nameFunc << ['toJavascript', 'toGroovy', 'toJsObj']
    }

    def 'show error message in console if nothing to convert'()
    {
        given:
        GroovySpy(GsConsole, global: true)

        when:
        GrooScript.convert(SOURCES_FOLDER_WITHOUT_FILES, BIG_JS_FILE)

        then:
        1 * GsConsole.error('No files to be converted. *.groovy or *.java files not found.')
    }

    @Unroll
    def 'native code returns the string code'() {
        given:
        def data = testData

        expect:
        GrooScript.nativeJs(data) == data

        where:
        testData << ['', 'hello', null]
    }

    def 'convert to native javascript'() {
        given:
        def code = '''
import org.grooscript.GrooScript

GrooScript.nativeJs('hello')
'''
        expect:
        GrooScript.convert(code) == 'hello;' + LS
    }

    def 'convert to native javascript using static import'() {
        given:
        def code = '''
import static org.grooscript.GrooScript.nativeJs

nativeJs('hello')
'''
        expect:
        GrooScript.convert(code) == 'hello;' + LS
    }

    private static final DEST_FOLDER = 'folder'
    private static final SOURCES_CLASSPATH = 'src/test/src'
    private static final SOURCES_FOLDER = 'src/test/src/files'
    private static final SOURCES_FOLDER_WITHOUT_FILES = 'src/test/src'
    private static final BIG_JS_FILE = 'allTogether.js'
    private static final INITIAL = '// INITIALINITIAL'
    private static final FINAL = '// FINALFINAL'
}
