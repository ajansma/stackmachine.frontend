package translator;

import slu.compiler.LexicalAnalyzer;
import slu.compiler.Token;

public class TestProgram {

// Grammar Used:
// semantic expressions for part one:
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
// num { print(num.val) }

    private static void showTokens(String program) {
        Token tokenName;

        LexicalAnalyzer scanner = new LexicalAnalyzer(program);

        do {
            tokenName = scanner.getToken();

            System.out.println("<" + tokenName.toString() + ">");
        } while (!tokenName.getName().equals("end_program"));

        System.out.println("");
    }

    // Grammar
    // program    → void main { declarations }
    // declarations  → declaration declarations | 
    // declaration  → type identifiers {identifiers.type = type_value};
    // type   → int {type.val = int} | float {type.val = float} | boolean {type.val = boolean}
    // identifiers  → id more-identifiers
    // more-identifiers → , id more-identifiers | 
    //                      {addSymbol(id.lexeme, identifiers.type)}
    // this is right recursive

    //

    public static void main(String[] args) {
        String expression = "((10 * 3) + (50 / 5)) * 2 / 4";

        showTokens(expression);

        PostfixTranslator postfix = new PostfixTranslator(new LexicalAnalyzer(expression));

        System.out.println("Infix expression   " + expression);
        System.out.println("Postfix expression " + postfix.translate());
        System.out.println("The expression value is " + postfix.evaluate());

    }

}
