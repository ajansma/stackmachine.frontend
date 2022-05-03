package stackmachine.frontend;

import java.util.*;

import slu.compiler.*;

/*
Grammar:
program                ->  void main { declarations instructions }
declarations           ->  declaration declarations | ε
declaration            ->  type identifiers ;
type                   ->  int | float | boolean
identifiers            ->  id optional-assignment more-identifiers
more-identifiers       ->  , id optional-assignment more-identifiers | ε
optional-assignment    ->  = logic-expression | ε
instructions           ->  instruction instructions | ε
instruction            ->  declaration |
                           id = logic-expression ; |
                           if (logic-expression) instruction |
                           if (logic-expression) instruction
                           else instruction |
                           while (logic-expression) instruction |
                           do instruction while (logic-expression) ; |
                           print (id) ; |
                           { instructions }
logic-expression       ->  logic-expression || logic-term |
                           logic-term
logic-term             ->  logic-term && logic-factor |
                           logic-factor
logic-factor           ->  ! logic-factor | true | false |
                           relational-expression
relational-expression  ->  expression relational-operator expression |
                           expression
relational-operator    ->  < | <= | > | >= | == | !=
expression             ->  expression + term |
                           expression - term |
                           term
term                   ->  term * factor |
                           term / factor |
                           term % factor |
                           factor
factor                 ->  (expression) |
                           id           |
                           num
 */

public class SyntaxAnalyzer implements ISyntaxAnalyzer {
    private Token token;
    private LexicalAnalyzer lexicalAnalyzer;
    private Map<String, DataType> symbolTable;
    private Map<String, Object> values; // added this to keep track of variables' values
    private List<String> code;
    private String compile;
    private int label;
    private Stack<Object> stack;
    private String postfix;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lexicalAnalyzer = lex;
        this.token = this.lexicalAnalyzer.getToken();
        this.symbolTable = new HashMap<String, DataType>();
        this.values = new HashMap<String, Object>();
        this.code = new ArrayList<String>();
        this.compile = "";
        this.label = 0;
        this.stack = new Stack<Object>();
        this.postfix = "";
    }


    @Override
    public boolean compile() {
        program();

        if(this.compile.equals("")){
            return true;
        }
        return false;
    }

    @Override
    public String output() {
        return this.compile;
    }

    public String intermediateCode() {
        String code = "";

        for (String instruction : this.code)
            code = code + instruction + "\n";

        return code;
    }

    public String symbolTable() {
        String symbols = "";

        Set<Map.Entry<String, DataType>> s = this.symbolTable.entrySet();

        for(Map.Entry<String, DataType> m : s)
            symbols = symbols + "<'" + m.getKey() + "', " +
                    m.getValue().toString() + "> \n";

        return symbols;
    }

    // generates a new label for intermediate code
    private int newLabel(){
        int newLabel = label;
        label++;
        return newLabel;
    }

    // Java code for the grammar rules and the semantic actions
    private void program(){
        match("void");
        match("main");
        match("open_curly_bracket");

        declarations();
        instructions();

        match("closed_curly_bracket");

        // add end to intermediate code
        this.code.add("halt");
    }

    private void declarations(){
        String tokenName = this.token.getName();

        if(tokenName.equals("int") || tokenName.equals("float") || tokenName.equals("boolean")) {
            declaration();
            declarations();
        }
    }

    private void declaration(){
        identifiers(type());
        match("semicolon");
    }

    private String type(){
        String type = this.token.getName();

        if(type.equals("int")){
            match("int");
        }
        else if(type.equals("float")){
            match("float");
        }
        else if(type.equals("boolean")){
            match("boolean");
        }
        else{
            System.out.println("Invalid type!");
        }
        return type;
    }

    private void identifiers(String type){
        if(this.token.getName().equals("id")) {
            // get the id
            Identifier id = (Identifier) this.token;

            // add symbol using helper function
            addSymbol(id, new PrimitiveType(type));

            match("id");

            optional_assignment(id);

            moreIdentifiers(type);
        }
    }

    private void moreIdentifiers(String type){
        if(this.token.getName().equals("comma")) {
            match("comma");
            // more-identifiers.val = identifiers.val
            Identifier id = (Identifier) this.token;

            // add symbol
            addSymbol(id, new PrimitiveType(type));

            // match
            match("id");

            // call optional-assignment
            optional_assignment(id);

            // call more identifiers
            moreIdentifiers(type);
        }
    }

    private void optional_assignment(Identifier id){
        // if =
        if(this.token.getName().equals("assignment")){
            this.code.add("lvalue " + id.getLexeme());
            // match =
            match("assignment");

            // call logic expression
            logic_expression();

            // add intermediate code
            this.code.add("=");

            // add value
            Object a = this.stack.pop();
            addValue(id, a);
        }
        // epsilon
    }

    // convert to right recursion
    private void instructions() {
        String tokenName = token.getName();
        // delcaration
        if (tokenName.equals("int") || tokenName.equals("float") || tokenName.equals("boolean")) {
            instruction();
            instructions();
        }
        // checking other instruction starters
        // else is currently not included here -- possibly should be ?
        else if(tokenName.equals("id") || tokenName.equals("if")  || tokenName.equals("while") || tokenName.equals("do") || tokenName.equals("print") || tokenName.equals("open_curly_bracket")){
            instruction();
            instructions();
        }
        // epsilon
    }

    // convert to right recursion
    private void instruction() {
        String tokenName = token.getName();
        if (tokenName.equals("int") || tokenName.equals("float") || tokenName.equals("boolean")) {
            declaration();
        }

        else if(this.token.getName().equals("id")){
            Identifier id = (Identifier) this.token;
            match("id");

            // add intermediate code
            this.code.add("lvalue " + id.getLexeme());

            match("assignment");

            logic_expression();

            // add value to table
            addValue(id, (int)stack.pop());

            // add intermediate code
            this.code.add("=");

            match("semicolon");
        }

        else if(this.token.getName().equals("if")){
            // if (condition)
            match("if");

            match("open_parenthesis");

            logic_expression();

            match("closed_parenthesis");

            // intermediate code
            int out = newLabel();
            this.code.add("goFalse label_" + out);

            instruction();

            // optional else
            if(this.token.getName().equals("else")){
                // intermediate code
                int label_else = newLabel();
                this.code.add("goto label_" + label_else);
                this.code.add("label_" + out);

                match("else");

                instruction();

                // intermediate code
                this.code.add("label_" + label_else);
            }

            else{
                // if intermediate code
                this.code.add("label " + out);
            }
        }

        else if(this.token.getName().equals("while")){
            int test = newLabel();

            match("while");
            match("open_parenthesis");

            // intermediate code
            this.code.add("label_" + test);

            logic_expression();

            // intermediate code
            int out = newLabel();
            this.code.add("gofalse label_" + out);

            match("closed_parenthesis");
            instruction();

            // intermediate code
            this.code.add("goto label_" + test);
            this.code.add("label_" + out);
        }

        else if(this.token.getName().equals("do")){
            match("do");

            // intermediate code
            int test = newLabel();
            this.code.add("label_" + test);

            instruction();
            match("while");
            match("open_parenthesis");

            logic_expression();

            match("closed_parenthesis");

            // intermediate code
            int out = newLabel();
            this.code.add("gofalse label_" + out);
            this.code.add("goto label_" + test);
            this.code.add("label_" + out);

            match("semicolon");
        }

        else if(this.token.getName().equals("print")){
            match("print");
            match("open_parenthesis");

            // intermediate code
            Identifier id = (Identifier) this.token;
            this.code.add("print " + id.getLexeme());

            match("id");
            match("closed_parenthesis");
            match("semicolon");

        }

        else if(this.token.getName().equals("open_curly_bracket")){
            match("open_curly_bracket");
            instructions();
            match("closed_curly_bracket");
        }

        else{
            this.compile += "instruction error on line" + this.lexicalAnalyzer.getLine() + "\n";
        }

    }

    // convert to right recursion
    // logic_expression -> logic-term more-logic-terms
    private void logic_expression(){
        logic_term();
        more_logic_terms();
    }

    // convert to right recursion
    private void logic_term(){
        logic_factor();
        more_logic_factors();
    }

    private void more_logic_factors(){
        if(this.token.getName().equals("and")){
            match("and");
            logic_factor();
            more_logic_factors();
        }
        // epsilon
    }

    private void more_logic_terms(){
        if(this.token.getName().equals("or")){
            match("or");
            logic_term();
            more_logic_terms();
        }
        // epsilon
    }

    // logic-factor           ->  ! logic-factor | true | false |
    //                           relational-expression
    private void logic_factor(){
        if(this.token.getName().equals("not")){
            match("not");
            logic_factor();
        }

        else if(this.token.getName().equals("true")){
            match("true");
        }

        else if(this.token.getName().equals("false")){
            match("false");
        }

        else{
            relational_expression();
        }
    }

    // relational-expression  ->  expression relational-operator expression |
    //                           expression
    private void relational_expression(){
        expression();
        // check if relational operator is involved
        String tokenName = this.token.getName();
        if(tokenName.equals("less_than") || tokenName.equals("less_equals") || tokenName.equals("greater_than") || tokenName.equals("greater_equals") || tokenName.equals("equals") || tokenName.equals("not_equals")){
            String operator = relational_operator();
            expression();
            this.code.add(operator);
        }
    }

    // relational-operator    ->  < | <= | > | >= | == | !=
    private String relational_operator(){
        if(this.token.getName().equals("less_than")){
            match("less_than");
            return "<";
        }

        else if(this.token.getName().equals("less_equals")){
            match("less_equals");
            return "<=";
        }

        else if(this.token.getName().equals("greater_than")){
            match("greater_than");
            return ">";
        }

        else if(this.token.getName().equals("greater_equals")){
            match("greater_equals");
            return ">=";
        }

        else if(this.token.equals("equals")){
            match("equals");
            return "=";
        }

        else if(this.token.equals("not_equals")){
            match("not_equals");
            return "!=";
        }

        // compiler error
        else{
            this.compile += "\nError at line " +
                    this.lexicalAnalyzer.getLine() + ", " +
                    "relational operator" + " expected";
        }
        return "";
    }

    // factor → ( expression ) |
    //          num { print(num.val) }
    private void factor() {
        if (this.token.getName().equals("open_parenthesis")) {
            match("open_parenthesis");
            expression();
            match("closed_parenthesis");
        }
        else if(this.token.getName().equals("id")){
            Identifier id = (Identifier) this.token;

            int number = (int) values.get(id.getLexeme());
            match("id");

            // add to stack
            this.postfix = this.postfix + " " + number + " ";
            this.stack.push(number);

            // add intermediate code
            this.code.add("rvalue " + id.getLexeme());
        }
        else if (this.token.getName().equals("int")) {
            // add integer to token
            IntegerNumber number = (IntegerNumber) this.token;

            // add integer to stack
            this.postfix = this.postfix + " " + number.getValue() + " ";
            this.stack.push(number.getValue());

            // add intermediate code
            this.code.add("push " + number.getValue());

            // match
            match("int");
        }
        else if (this.token.getName().equals("float")){
            RealNumber number = (RealNumber) this.token;

            // add to stack
            this.postfix = this.postfix + " " + number.getValue() + " ";
            this.stack.push(number.getValue());

            // add intermediate code
            this.code.add("push " + number.getValue());

            // match
            match("float");
        }
        else {
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

            // add to intermediate code
            this.code.add("*");

            // pop off stack and perform operation
            int num1 = (int) this.stack.pop();
            int num2 = (int) this.stack.pop();
            this.stack.push(num2 * num1);
            int val = num2 * num1;
            moreFactors();

        } else if (this.token.getName().equals("divide")) {
            match("divide");
            factor();
            // add to postfix
            this.postfix = this.postfix + " / ";

            // add to intermediate code
            this.code.add("/");

            // pop off the stack
            int num1 = (int) this.stack.pop();
            int num2 = (int) this.stack.pop();
            int newNum = num2 / num1;
            this.stack.push(newNum);
            moreFactors();
        } else if (this.token.getName().equals("remainder")) {
            match("remainder");
            factor();
            // add to postfix
            this.postfix = this.postfix + " % ";

            // add to intermediate code
            this.code.add("%");

            // pop off the stack
            int num1 = (int) this.stack.pop();
            int num2 = (int) this.stack.pop();
            this.stack.push(num2 % num1);
            int val = num2 % num1;

            // add to intermediate code
            this.code.add("push " + val);

            // next call
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

            // add + to intermediate code
            this.code.add("+");

            // pop off the stack
            int num1 = (int) this.stack.pop();
            int num2 = (int) this.stack.pop();
            int newNum = num2 + num1;
            // push to stack
            this.stack.push(newNum);

            moreTerms();
        } else if (this.token.getName().equals("subtract")) {
            match("subtract");
            term();
            // add - to the token
            this.postfix = this.postfix + " - ";

            // add to intermediate code
            this.code.add("-");

            // pop the two top numbers off the stack
            int num1 = (int) this.stack.pop();
            int num2 = (int) this.stack.pop();

            // add to stack
            this.stack.push(num2 - num1);

            moreTerms();
        }
    }

    // this function assigns variables their value
    private void addValue(Identifier id, Object a){
        if(this.symbolTable.get(id.getLexeme()) != null){
            this.values.put(id.getLexeme(), a);
        }

        else{
            this.compile += "\nError at line " + this.lexicalAnalyzer.getLine() + ", identifier '" + id.getLexeme() + "' is not declared";
        }
    }

    private void addSymbol(Identifier id, PrimitiveType type){
        if(this.symbolTable.get(id.getLexeme()) == null){
            this.symbolTable.put(id.getLexeme(), type);
        }
        else {
            this.compile += "\nError at line " + this.lexicalAnalyzer.getLine() + ", identifier '" + id.getLexeme() + "' is already declared";
            //System.out.println("\nError at line " + this.lexicalAnalyzer.getLine() + ", identifier '" + id.getLexeme() + "' is already declared");
        }
    }

    // Java code for the grammar rules and the semantic actions
    private void match(String tokenName) {
        if (this.token.getName().equals(tokenName))
            this.token = this.lexicalAnalyzer.getToken();
        else {
            this.compile += "\nError at line " +
                    this.lexicalAnalyzer.getLine() + ", " +
                    this.lexicalAnalyzer.getLexeme(tokenName) + " expected";
            System.out.println("\nError at line " +
                    this.lexicalAnalyzer.getLine() + ", " +
                    this.lexicalAnalyzer.getLexeme(tokenName) + " expected");
            System.out.println(this.token.getName());
        }
    }

}

