package variables;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import slu.compiler.*;

// Ariana Jansma
// HW2 Declaration of Variables

/*
    // Grammar
    // program    →         void main { declarations }
    // declarations  →      declaration declarations
                            | epsilon
    // declaration  →       type {identifiers.val = type_value} identifiers ;
    // type   →             int {type.val = int}
                            | float {type.val = float}
                            | boolean {type.val = boolean}
    // identifiers  →       id {addSymbol(id.lexeme, identifiers.type); more-identifiers.val = indentifiers.val} more-identifiers
    // more-identifiers → , id  {addSymbol(id.lexeme, more-identifiers.val)} more-identifiers
                            | epsilon

    // this is right recursive
 */

public class SyntaxAnalyzer implements ISyntaxAnalyzer {
    private Token token;
    private LexicalAnalyzer lexicalAnalyzer;
    private Hashtable<String, DataType> symbolTable;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lexicalAnalyzer = lex;
        this.token = this.lexicalAnalyzer.getToken();
        this.symbolTable = new Hashtable<String, DataType>();
    }

    public String symbolTable() {
        String symbols = "";

        Set<Map.Entry<String, DataType>> s = this.symbolTable.entrySet();

        for(Map.Entry<String, DataType> m : s)
            symbols = symbols + "<'" + m.getKey() + "', " + m.getValue().toString() + "> \n";

        return symbols;
    }

    public void compile() {
        program();
    }

    private void program(){
        match("void");
        match("main");
        match("open_curly_bracket");

        declarations();

        match("closed_curly_bracket");
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

            // call more identifiers
            moreIdentifiers(type);
        }
    }

    private void addSymbol(Identifier id, PrimitiveType type){
        if(this.symbolTable.get(id.getLexeme()) == null){
            this.symbolTable.put(id.getLexeme(), type);
        }
        else
            System.out.println("\nError at line " + this.lexicalAnalyzer.getLine() + ", identifier '" + id.getLexeme() + "' is already declared");
    }

    // Java code for the grammar rules and the semantic actions

    private void match(String tokenName) {
        if (this.token.getName().equals(tokenName))
            this.token = this.lexicalAnalyzer.getToken();
        else {
            System.out.println("\nError at line " +
                    this.lexicalAnalyzer.getLine() + ", " +
                    this.lexicalAnalyzer.getLexeme(tokenName) + " expected");
            System.out.println(this.token.getName());
        }
    }
}