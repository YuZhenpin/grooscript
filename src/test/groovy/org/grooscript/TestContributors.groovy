package org.grooscript

import spock.lang.Specification
import org.grooscript.test.TestJs
import spock.lang.Unroll

/**
 * JFL 27/08/12
 */
class TestContributors extends Specification {

    def converter = new GsConverter()

    def readAndConvert(nameOfFile,consoleOutput,String textSearch=null,String textReplace=null) {

        def file = TestJs.getGroovyTestScript(nameOfFile)

        def String jsScript = converter.toJs(file.text)

        if (textSearch && jsScript.indexOf(textSearch)>=0) {
            //jsScript.replaceAll('/'+textSearch+'/',textReplace)
            jsScript = jsScript.substring(0,jsScript.indexOf(textSearch)) +
                    textReplace + jsScript.substring(jsScript.indexOf(textSearch)+textSearch.size())
        }

        if (consoleOutput) {
            println 'jsScript->\n'+jsScript
        }

        return TestJs.jsEval(jsScript)
    }


    def 'test jochen' () {
        when:
        def result = readAndConvert('contribution/JochenTheodorou',false)

        then:
        !result.assertFails
    }

    @Unroll('Testing MrHaki #file')
    def 'test MrHaki' () {
        expect:
        def result = readAndConvert(file,false)//file=='contribution/MrHakiGetSetProperties')
        //println 'Console->'+result.gSconsole
        !result.assertFails

        where:
        file                                    |_
        'contribution/MrHakiClosureReturn'      |_
        'contribution/MrHakiFirstLast'          |_
        'contribution/MrHakiSum'                |_
        'contribution/MrHakiLooping'            |_
        'contribution/MrHakiInject'             |_
        'contribution/MrHakiGrep'               |_
        'contribution/MrHakiGetSetProperties'   |_
        'contribution/MrHakiSpread'             |_

    }

    def 'test alex anderson' () {
        when:
        def result = readAndConvert('contribution/AlexAnderson',false)

        then:
        //println 'Console->'+result.gSconsole
        !result.assertFails

    }

    def 'test mario garcia' () {
        when:
        def result = readAndConvert('contribution/MarioGarcia',false)

        then:
        //println 'Console->'+result.gSconsole
        !result.assertFails

    }

    @Unroll('Testing anonymous web #file')
    def 'test anonymous contributions in web' () {
        expect:
        def result = readAndConvert(file,false)
        !result.assertFails
        result.gSconsole.contains(text)
        //println result.gSconsole

        where:
        file                       | text
        'contribution/Anonymous0'  | 'FizzBuzz\n91'
        'contribution/Anonymous1'  | 'fizzbuzz\n91'
        'contribution/Anonymous2'  | 'fizZbuzZ\n16'

    }

    def 'bugs coming from monkfish'() {
        when:
        def result = readAndConvert('contribution/MonkFish',false,
                'gSobject.value = 0;',
                'gSobject.value = 0;gSobject.two = function() {return 2;};')

        then:
        //println 'Console->'+result.gSconsole
        !result.assertFails
    }

    def 'testing more web' () {
        when:
        def result = readAndConvert('contribution/Anonymous3',false)

        then:
        //println 'Console->'+result.gSconsole
        !result.assertFails

    }

    def 'testing mario extends'() {
        when:
        def result = readAndConvert('contribution/MarioGarcia2',false)

        then:
        //println 'Console->'+result.gSconsole
        !result.assertFails
    }

    def 'testing mario maps'() {
        when:
        def result = readAndConvert('contribution/MarioGarcia3',false)

        then:
        //println 'Console->'+result.gSconsole
        !result.assertFails
    }

    def 'twitter code found scoping closures'() {
        when:
        def result = readAndConvert('contribution/Twitter1',false)

        then:
        //println 'Console->'+result.gSconsole
        !result.assertFails
    }

}
