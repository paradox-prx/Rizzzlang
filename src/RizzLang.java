import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class RizzLang {
    private String filename;

    public RizzLang(String filename) {
        this.filename = filename;
    }


    public void run() {
        // 1) Read file
        String sourceCode = readFile(filename);
        if (sourceCode == null) {
            System.err.println("Error: Could not read file " + filename);
            return;
        }

        // 2) Tokenize using single big DFA
        LexicalAnalyzer lexer = new LexicalAnalyzer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        // 3) Reclassify <operand> <operator> <operand> => ARITHMETIC
        tokens = lexer.classifyArithmeticOperations(tokens);

        // 4) Print out the tokens
        System.out.println("========== Lexical Analysis ==========");
        System.out.println("Number of tokens: " + tokens.size());
        for (Token t : tokens) {
            System.out.println(t);
        }

        SymbolTable symTable = new SymbolTable();
        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER) {
                symTable.add(t.getLexeme(), "Identifier");
            }
        }

        System.out.println("\n========== Symbol Table ==========");
        symTable.print();

        System.out.println("\n========== Unified DFA Transition Table ==========");
        lexer.printDFATable();

        if (!lexer.getErrorHandler().getErrors().isEmpty()) {
            System.out.println("\n========== Errors ==========");
            for (String error : lexer.getErrorHandler().getErrors()) {
                System.out.println(error);
            }
        }
    }


    private String readFile(String filename) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }
}
