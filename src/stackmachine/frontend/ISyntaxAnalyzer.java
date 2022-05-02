package stackmachine.frontend;

public interface ISyntaxAnalyzer {

    public boolean compile();
    public String output();
    public String intermediateCode();
    public String symbolTable();

}
