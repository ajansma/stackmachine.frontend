package translator;

import slu.compiler.IntegerNumber;
import slu.compiler.LexicalAnalyzer;
import slu.compiler.Token;

import java.util.Stack;

// Ariana Jansma
// Infix to Postfix Translator

// The grammar:
// expression -> term more-terms
// more-terms → + term { print(“+”) } more-terms |
//              – term { print(“-”) } more-terms |
//              ε
// term → factor more-factors
// more-factors →* factor { print(“*”) } more-factors |
//                / factor { print(“/”) } more-factors |
//                % factor { print(“%”) } more-factors |
//                 ε
// factor → ( expression ) |
//          num { print(num.val) }

public class PostfixTranslator implements IPostfixTranslator{
    private Token token;
    private LexicalAnalyzer lexicalAnalyzer;
    private Stack<Integer> stack;
    private String postfix;

    public PostfixTranslator(LexicalAnalyzer lex) {
        this.lexicalAnalyzer = lex;
        this.token = this.lexicalAnalyzer.getToken();
        this.stack = new Stack<Integer>();
        this.postfix = "";
    }

    // returns the postfix token
    public String translate() {
        // returns the postfix expression
        expression();
        return this.postfix;
    }

    // factor → ( expression ) |
    //          num { print(num.val) }
    private void factor() {
        if (this.token.getName().equals("open_parenthesis")) {
            match("open_parenthesis");
            expression();
            match("closed_parenthesis");
        } else if (this.token.getName().equals("int")) {
            // add integer to token
            IntegerNumber number = (IntegerNumber) this.token;
            // add integer to stack
            this.postfix = this.postfix + " " + number.getValue() + " ";
            this.stack.push(number.getValue());
            match("int");
        } else {
            System.out.println("\nError at line " +
                    this.lexicalAnalyzer.getLine() +
                    ", open parenthesis or int expected");
        }
    }

    // more-factors →* factor { print(“*”) } more-factors |
    //                / factor { print(“/”) } more-factors |
    //                % factor { print(“%”) } more-factors |
    //                ε
    private void moreFactors() {
        if (this.token.getName().equals("multiply")) {
            match("multiply");
            factor();
            // add to token
            this.postfix = this.postfix + " * ";
            // pop off stack and perform operation
            int num1 = this.stack.pop();
            int num2 = this.stack.pop();
            this.stack.push(num2 * num1);
            moreFactors();
        } else if (this.token.getName().equals("divide")) {
            match("divide");
            factor();
            this.postfix = this.postfix + " / ";
            // pop off the stack
            int num1 = this.stack.pop();
            int num2 = this.stack.pop();
            int newNum = num2 / num1;
            this.stack.push(newNum);
            moreFactors();
        } else if (this.token.getName().equals("remainder")) {
            match("remainder");
            factor();
            this.postfix = this.postfix + " % ";
            // pop off the stack
            int num1 = this.stack.pop();
            int num2 = this.stack.pop();
            this.stack.push(num2 % num1);
            moreFactors();
        }
    }

    // Root
    // expression -> term more-terms
    private void expression() {
        term();
        moreTerms();
    }

    // term → factor more-factors
    private void term() {
        factor();
        moreFactors();
    }

    // more-terms → + term { print(“+”) } more-terms |
    //              – term { print(“-”) } more-terms |
    //              ε
    private void moreTerms() {
        if (this.token.getName().equals("add")) {
            match("add");
            term();
            // add the + to the token
            this.postfix = this.postfix + " + ";
            // pop off the stack
            int num1 = this.stack.pop();
            int num2 = this.stack.pop();
            int newNum = num2 + num1;
            this.stack.push(newNum);
            moreTerms();
        } else if (this.token.getName().equals("subtract")) {
            match("subtract");
            term();
            // add - to the token
            this.postfix = this.postfix + " - ";
            // pop the two top numbers off the stack
            int num1 = this.stack.pop();
            int num2 = this.stack.pop();
            this.stack.push(num2 - num1);
            moreTerms();
        }
    }

    // return the expression value
    public int evaluate() {
        // returns the expression value
        return this.stack.pop();
    }

    // Java code for the grammar rules and the semantic actions
    private void match(String tokenName) {
        if (this.token.getName().equals(tokenName))
            this.token = this.lexicalAnalyzer.getToken();
        else
            System.out.println("\nError at line " + this.lexicalAnalyzer.getLine() + ", " + this.lexicalAnalyzer.getLexeme(tokenName) + " expected");
    }
}
