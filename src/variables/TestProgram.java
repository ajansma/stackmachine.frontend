package variables;

import slu.compiler.LexicalAnalyzer;
import slu.compiler.Token;

public class TestProgram {

    private static void showTokens(String program) {
        Token tokenName;

        try {
            LexicalAnalyzer scanner = new LexicalAnalyzer(program);

            do {
                tokenName = scanner.getToken();

                System.out.println("<" + tokenName.toString() + ">");

            } while (!tokenName.getName().equals("end_program"));

            System.out.println("");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        String program = "void main { int a, b, c, d; float x, y, z; }";

        showTokens(program);

        try {
            SyntaxAnalyzer parser = new SyntaxAnalyzer(new LexicalAnalyzer(program));

            parser.compile();

            System.out.println("The symbol table: \n\n" + parser.symbolTable());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}