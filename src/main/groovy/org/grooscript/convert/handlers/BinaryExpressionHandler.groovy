package org.grooscript.convert.handlers

import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.VariableExpression

import static org.grooscript.JsNames.*

/**
 * User: jorgefrancoleza
 * Date: 21/01/14
 */
class BinaryExpressionHandler extends BaseHandler {

    void handle(BinaryExpression expression) {

        //println 'Binary->'+expression.text + ' - '+expression.operation.text
        //Getting a range from a list
        if (expression.operation.text == '[' && expression.rightExpression instanceof RangeExpression) {
            out.addScript("${GS_RANGE_FROM_LIST}(")
            upgradedExpresion(expression.leftExpression)
            out.addScript(", ")
            factory.visitNode(expression.rightExpression.getFrom())
            out.addScript(", ")
            factory.visitNode(expression.rightExpression.getTo())
            out.addScript(')')
            //leftShift and rightShift function
        } else if (expression.operation.text == '<<' || expression.operation.text == '>>') {
            def nameFunction = expression.operation.text == '<<' ? 'leftShift' : 'rightShift'
            out.addScript("${GS_METHOD_CALL}(")
            upgradedExpresion(expression.leftExpression)
            out.addScript(",'${nameFunction}', ${GS_LIST}([")
            upgradedExpresion(expression.rightExpression)
            out.addScript(']))')
            //Regular Expression exact match all
        } else if (expression.operation.text == '==~') {
            out.addScript("${GS_EXACT_MATCH}(")
            upgradedExpresion(expression.leftExpression)
            out.addScript(',')
            //If is a regular expresion /fgsg/, comes like a contantExpresion fgsg, we keep /'s for javascript
            if (expression.rightExpression instanceof ConstantExpression) {
                out.addScript('/')
                factory.visitNode(expression.rightExpression, false)
                out.addScript('/')
            } else {
                upgradedExpresion(expression.rightExpression)
            }

            out.addScript(')')
            //A matcher of regular expresion
        } else if (expression.operation.text == '=~') {
            out.addScript("${GS_REG_EXP}(")
            //println 'rx->'+expression.leftExpression
            upgradedExpresion(expression.leftExpression)
            out.addScript(',')
            //If is a regular expresion /fgsg/, comes like a contantExpresion fgsg, we keep /'s for javascript
            if (expression.rightExpression instanceof ConstantExpression) {
                out.addScript('/')
                factory.visitNode(expression.rightExpression, false)
                out.addScript('/')
            } else {
                upgradedExpresion(expression.rightExpression)
            }

            out.addScript(')')
            //Equals
        } else if (expression.operation.text == '==') {
            writeFunctionWithLeftAndRight(GS_EQUALS, expression)
            //in
        } else if (expression.operation.text == 'in') {
            writeFunctionWithLeftAndRight(GS_IN, expression)
            //Spaceship operator <=>
        } else if (expression.operation.text == '<=>') {
            writeFunctionWithLeftAndRight(GS_SPACE_SHIP, expression)
            //instanceof
        } else if (expression.operation.text == 'instanceof') {
            out.addScript("${GS_INSTANCE_OF}(")
            upgradedExpresion(expression.leftExpression)
            out.addScript(', "')
            upgradedExpresion(expression.rightExpression)
            out.addScript('")')
            //Multiply
        } else if (expression.operation.text == '*') {
            writeFunctionWithLeftAndRight(GS_MULTIPLY, expression)
            //Power
        } else if (expression.operation.text == '**') {
            writeFunctionWithLeftAndRight('Math.pow', expression)
            //Plus
        } else if (expression.operation.text == '+') {
            writeFunctionWithLeftAndRight(GS_PLUS, expression)
            //Minus
        } else if (expression.operation.text == '-') {
            writeFunctionWithLeftAndRight(GS_MINUS, expression)
        } else {

            //Execute setter if available
            if (expression.leftExpression instanceof PropertyExpression &&
                    (expression.operation.text in ['=', '+=', '-=']) &&
                    !(expression.leftExpression instanceof AttributeExpression)) {

                PropertyExpression pe = (PropertyExpression)expression.leftExpression
                out.addScript("${GS_SET_PROPERTY}(")
                upgradedExpresion(pe.objectExpression)
                out.addScript(',')
                upgradedExpresion(pe.property)
                out.addScript(',')
                if (expression.operation.text == '+=') {
                    factory.visitNode(expression.leftExpression)
                    out.addScript(' + ')
                } else if (expression.operation.text == '-=') {
                    factory.visitNode(expression.leftExpression)
                    out.addScript(' - ')
                }
                upgradedExpresion(expression.rightExpression)
                out.addScript(')')

            } else {
                //println ' other->'+expression.text
                //If we are assigning a variable, and don't exist in scope, we add to it
                if (expression.operation.text=='=' && expression.leftExpression instanceof VariableExpression
                        && !context.allActualScopeContains(expression.leftExpression.name) &&
                        !context.variableScopingContains(expression.leftExpression.name)) {
                    context.addToActualScope(expression.leftExpression.name)
                }

                //If is a boolean operation, we have to apply groovyTruth
                //Left
                if (expression.operation.text in ['&&', '||']) {
                    out.addScript '('
                    factory.handExpressionInBoolean(expression.leftExpression)
                    out.addScript ')'
                } else {
                    upgradedExpresion(expression.leftExpression)
                }
                //Operator
                //println 'Operator->'+expression.operation.text
                out.addScript(' '+expression.operation.text+' ')
                //Right
                //println 'Right->'+expression.rightExpression
                if (expression.operation.text in ['&&','||']) {
                    out.addScript '('
                    factory.handExpressionInBoolean(expression.rightExpression)
                    out.addScript ')'
                } else {
                    upgradedExpresion(expression.rightExpression)
                }
                if (expression.operation.text=='[') {
                    out.addScript(']')
                }
            }
        }
    }

    //Adding () for operators order, can spam loads of ()
    private upgradedExpresion(expresion) {
        if (expresion instanceof BinaryExpression) {
            out.addScript('(')
        }
        factory.visitNode(expresion)
        if (expresion instanceof BinaryExpression) {
            out.addScript(')')
        }
    }

    private writeFunctionWithLeftAndRight(functionName, expression) {
        out.addScript("${functionName}(")
        upgradedExpresion(expression.leftExpression)
        out.addScript(', ')
        upgradedExpresion(expression.rightExpression)
        out.addScript(')')
    }
}
