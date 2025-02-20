import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * MyCompiler.java
 *
 * Demonstrates a single, unified DFA for tokenizing a custom language with the following requirements:
 *   1. Data types: int, bool, float, char (and recognized keywords).
 *   2. Only lowercase letters (a-z) for identifiers.
 *   3. Arithmetic ops: +, -, *, /, %, ^ (with a reclassification <operand> <op> <operand> -> ARITHMETIC).
 *   4. Decimal numbers recognized up to 5 decimal places.
 *   5. Strings in "..." or '...'.
 *   6. Single-line (//) and multi-line (/* ... ) comments.
 *   7. "in(<identifier>)" and "out(<identifier>)" recognized as special I/O keywords.
        *   8. A single big DFA approach, with a single transition table, that drives all tokenization.
        *   9. Prints the "real" DFA transition table.
 *  10. Post-processing merges <operand> <operator> <operand> to ARITHMETIC tokens.
 */
public class MyCompiler {

    public static void main(String[] args) {
        // Read source code
        String sourceCode = readFile("test.rizz");
        if (sourceCode == null) {
            System.err.println("Error: Could not read file test.rizz");
            return;
        }

        // 1) Tokenize using the single big DFA
        LexicalAnalyzer lexer = new LexicalAnalyzer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        // 2) Reclassify <operand> <operator> <operand> as ARITHMETIC
        tokens = lexer.classifyArithmeticOperations(tokens);

        // 3) Print results
        System.out.println("========== Lexical Analysis ==========");
        System.out.println("Number of tokens: " + tokens.size());
        for (Token t : tokens) {
            System.out.println(t);
        }

        // 4) Build and print Symbol Table from identifiers
        SymbolTable symTable = new SymbolTable();
        for (Token t : tokens) {
            if (t.type == TokenType.IDENTIFIER) {
                symTable.add(t.lexeme, "Identifier");
            }
        }
        System.out.println("\n========== Symbol Table ==========");
        symTable.print();

        // 5) Print the big unified DFA's transition table
        System.out.println("\n========== Unified DFA Transition Table ==========");
        lexer.printDFATable();

        // 6) Print any errors
        if (!lexer.errorHandler.getErrors().isEmpty()) {
            System.out.println("\n========== Errors ==========");
            for (String error : lexer.errorHandler.getErrors()) {
                System.out.println(error);
            }
        }
    }

    /**
     * Reads an entire file into a String
     */
    private static String readFile(String filename) {
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

    // ------------------- TOKEN TYPES AND TOKEN CLASS --------------------
    enum TokenType {
        IDENTIFIER, KEYWORD, INTEGER, DECIMAL, BOOLEAN, CHARACTER, OPERATOR, COMMENT, STRING, ARITHMETIC, ERROR
    }

    static class Token {
        TokenType type;
        String lexeme;
        int line;

        public Token(TokenType type, String lexeme, int line) {
            this.type = type;
            this.lexeme = lexeme;
            this.line = line;
        }

        @Override
        public String toString() {
            return "Token [type=" + type + ", lexeme=\"" + lexeme + "\", line=" + line + "]";
        }
    }

    // ------------------- LEXICAL ANALYZER USING A SINGLE DFA --------------------
    static class LexicalAnalyzer {
        private final String src;
        private int pos = 0;
        private int line = 1;
        private final int[][] dfa; // The single big transition table
        private final int START_STATE = 0;
        private final int ERROR_STATE = -1;
        private final List<Token> tokens = new ArrayList<>();
        ErrorHandler errorHandler = new ErrorHandler();

        // Categories (columns in the DFA). We'll define them as constants:
        private static final int CAT_LETTER       = 0;  // a-z
        private static final int CAT_DIGIT        = 1;  // 0-9
        private static final int CAT_DOT          = 2;  // .
        private static final int CAT_SLASH        = 3;  // /
        private static final int CAT_STAR         = 4;  // *
        private static final int CAT_QUOTE_DOUBLE = 5;  // "
        private static final int CAT_QUOTE_SINGLE = 6;  // '
        private static final int CAT_PLUS         = 7;  // +
        private static final int CAT_MINUS        = 8;  // -
        private static final int CAT_PERCENT      = 9;  // %
        private static final int CAT_CARET        = 10; // ^
        private static final int CAT_EQUAL        = 11; // =
        private static final int CAT_LT           = 12; // <
        private static final int CAT_GT           = 13; // >
        private static final int CAT_LPAREN       = 14; // (
        private static final int CAT_RPAREN       = 15; // )
        private static final int CAT_WHITESPACE   = 16; // space, tab, \n, etc.
        private static final int CAT_OTHER        = 17; // anything not in the above

        /**
         * Our single big table. We define states 0..N.
         * Negative = error. Or we'll keep an error column as -1.
         * See below for explanation.
         */
        {
            /*
             * We'll define states for:
             *  0 = START
             *  1 = In IDENTIFIER
             *  2 = In integer
             *  3 = Saw decimal dot after integer
             *  4 = In float fractional part
             *  5 = Saw first slash -> possible comment
             *  6 = In single-line comment
             *  7 = Saw slash-*, in multi-line comment
             *  8 = Potential close multi-line comment if we see star slash
             *  9 = In string with double quote
             * 10 = In string with single quote
             * 11 = OPERATOR state (like +, -, etc.)
             *
             * We define the final states or transitions that "commit" the token
             * once we see a character that doesn't belong.
             *
             * We'll handle the acceptance and token creation in a loop outside
             * of the raw table.
             *
             * The columns are in the order:
             *   0=LETTER,1=DIGIT,2='.',3='/',4='*',5='"',6='\'',7='+',8='-',9='%',10='^',
             *   11='=',12='<',13='>',14='(',15=')',16=whitespace,17=other
             *
             * The table is an array of length [number_of_states][number_of_categories],
             * returning the next state or -1 for error.
             *
             * For strings and comments, we typically stay in that state until
             * we see a terminator. Or if we see end-of-file first, we produce an error.
             */
        }

        // We'll fill this table in the static initializer block below
        // for clarity:
        private static final int NUM_STATES = 12; // 0..11
        private static final int NUM_COLS   = 18; // categories 0..17

        public LexicalAnalyzer(String source) {
            this.src = source;
            this.dfa = buildDFATable();
        }

        /**
         * Build the single big DFA transition table.
         */
        private int[][] buildDFATable() {
            // Initialize all transitions to -1 (error)
            int[][] table = new int[NUM_STATES][NUM_COLS];
            for (int i=0; i<NUM_STATES; i++) {
                for (int j=0; j<NUM_COLS; j++) {
                    table[i][j] = ERROR_STATE;
                }
            }

            /* State 0: START */
            // letter -> state 1 (in identifier)
            table[0][CAT_LETTER] = 1;
            // digit -> state 2 (in integer)
            table[0][CAT_DIGIT] = 2;
            // '.' -> We'll treat leading '.' as "error" for a numeric unless we want .5?
            //        We'll keep it as error for simplicity.
            // '/'
            table[0][CAT_SLASH] = 5;    // possible comment or operator
            // '*', '"', ''', '+', '-', '%', '^' => go to operator state (11), but each is recognized as an immediate operator
            table[0][CAT_STAR]         = 11;
            table[0][CAT_QUOTE_DOUBLE] = 9;  // start string "
            table[0][CAT_QUOTE_SINGLE] = 10; // start string '
            table[0][CAT_PLUS]         = 11; // operator
            table[0][CAT_MINUS]        = 11; // operator
            table[0][CAT_PERCENT]      = 11; // operator
            table[0][CAT_CARET]        = 11; // operator
            table[0][CAT_EQUAL]        = 11; // operator
            table[0][CAT_LT]           = 11; // operator
            table[0][CAT_GT]           = 11; // operator
            table[0][CAT_LPAREN]       = 11; // operator
            table[0][CAT_RPAREN]       = 11; // operator
            // whitespace -> stay in 0 (just skip it)
            table[0][CAT_WHITESPACE]   = 0;
            // other => error

            /* State 1: in IDENTIFIER */
            // letter -> stay in 1
            table[1][CAT_LETTER] = 1;
            // digit => error in our language (only letters for ID?), or do we allow digits in ID?
            //   We'll keep it as an error to match "only a-z valid variable names."
            // anything else -> token ends, so that means we go to a special -1 => we produce IDENTIFIER

            /* State 2: in integer */
            // digit -> stay in 2
            table[2][CAT_DIGIT] = 2;
            // '.' -> go to 3 (saw decimal point, might become float)
            table[2][CAT_DOT]   = 3;

            /* State 3: saw decimal after integer => in fractional part state 4 */
            // digit -> go to 4
            table[3][CAT_DIGIT] = 4;

            /* State 4: in float fractional part */
            // digit -> stay in 4
            table[4][CAT_DIGIT] = 4;

            /* State 5: saw '/' from START => either '//' => single-line comment => state 6,
               or '/*' => multi-line => state 7, or just a single operator '/' => state 11?
               We'll handle it with second char look. But for the table, let's do:
               - slash again => 6
               - star => 7
               - else => operator.
            */
            table[5][CAT_SLASH] = 6; // '//' => single-line comment
            table[5][CAT_STAR]  = 7; // '/*' => multi-line comment
            // anything else => treat '/' as an operator => go to 11
            // We'll handle it by default = 11

            /* State 6: single-line comment. We remain in 6 until we see newline => then go to START(0).
               For the table, all categories remain in 6 except for whitespace if it's a newline.
               We'll do a simplified approach: if it's not newline, stay in 6. If it's newline, go to 0.
             */
            // We'll consider '\n' as part of whitespace category. We'll need a special check in code
            // to increment line. So let's do: whitespace => stay in 6 except if it's specifically '\n', then 0.
            // For simplicity, let's keep table: 6->6 for all categories, and we break out on code that sees newline.
            for (int cat=0; cat<NUM_COLS; cat++) {
                table[6][cat] = 6;
            }

            /* State 7: multi-line comment. We remain in 7 until we see '*' => possible end => go to 8.
               So star => 8, else => stay in 7
             */
            table[7][CAT_STAR] = 8;
            for (int cat=0; cat<NUM_COLS; cat++) {
                if (cat != CAT_STAR) {
                    table[7][cat] = 7;
                }
            }

            /* State 8: saw '*' inside multi-line. If next is '/', we end comment => go to START(0). If next is star again, stay in 8?
               Actually we only see one char at a time. We'll do: if next char is '/', go 0, else if star => stay in 8, else => 7
             * We'll store that in code. For the table, we do a simplified approach
             * but let's do: slash => 0, star => stay in 8, else => 7
             */
            table[8][CAT_SLASH] = 0;
            table[8][CAT_STAR]  = 8;
            for (int cat=0; cat<NUM_COLS; cat++) {
                if (cat != CAT_SLASH && cat != CAT_STAR) {
                    table[8][cat] = 7;
                }
            }

            /* State 9: in string with double quote.
               We remain in 9 for every input except a double quote => end string => produce token.
               We'll do that in code by seeing if next is " => end, else stay in 9.
             */
            for (int cat=0; cat<NUM_COLS; cat++) {
                table[9][cat] = 9;
            }
            // If we see another double quote => token ends => return to START
            table[9][CAT_QUOTE_DOUBLE] = ERROR_STATE; // we handle in code to finalize

            /* State 10: in string with single quote.
               Similar approach to state 9.
             */
            for (int cat=0; cat<NUM_COLS; cat++) {
                table[10][cat] = 10;
            }
            table[10][CAT_QUOTE_SINGLE] = ERROR_STATE; // handle in code to finalize

            /* State 11: OPERATOR state => as soon as we enter it, we finalize that operator token
               unless we are continuing to read e.g. '//' or '/*' which we handle from START though.
               So from 11 any input basically means "we're done, produce OPERATOR"
               We'll keep it simple: table[11][any] = -1
             */
            for (int cat=0; cat<NUM_COLS; cat++) {
                table[11][cat] = ERROR_STATE;
            }

            return table;
        }

        /**
         * Print out the transition table for this single big DFA.
         */
        public void printDFATable() {
            String[] stateNames = {
                    "0(START)", "1(ID)", "2(INT)", "3(SAW_DOT)", "4(FLOAT_FRAC)",
                    "5(SLASH?)", "6(SL_COMMENT)", "7(ML_COMMENT)", "8(MLC_STAR)",
                    "9(STR_DQ)", "10(STR_SQ)", "11(OPERATOR)"
            };
            String[] colNames = {
                    "LETTER","DIGIT","DOT","/","*","\"","'","+","-","%","^","=","<",">","(",")", "WS","OTHER"
            };

            System.out.println("DFA Transition Table (rows = states, columns = input categories):");
            System.out.print("       ");
            for (String c : colNames) {
                System.out.printf("%-8s", c);
            }
            System.out.println();

            for (int s=0; s<NUM_STATES; s++) {
                System.out.printf("%-8s", stateNames[s]);
                for (int c=0; c<NUM_COLS; c++) {
                    int nxt = dfa[s][c];
                    if (nxt == ERROR_STATE)
                        System.out.printf("%-8s", "ERR");
                    else
                        System.out.printf("%-8s", nxt);
                }
                System.out.println();
            }
        }

        /**
         * Tokenize by simulating the DFA. We read char by char,
         * compute the category, find nextState. If nextState=ERR or we must finalize,
         * we produce a token (if in final state) or an error, then reset to START.
         */
        public List<Token> tokenize() {
            while (pos < src.length()) {
                int startPos = pos;
                int startLine = line;

                // If we're in START, try to consume at least one char to see what we get
                Token t = getNextToken();
                if (t != null) {
                    // We got a token or an error
                    tokens.add(t);
                }
                // If t == null, it means we hit a whitespace or comment that we skip
                // or we are mid comment/string. We'll keep reading until a break condition
            }
            return tokens;
        }

        /**
         * Attempt to get the next token from the current position using the DFA.
         * Returns null if we recognized something that doesn't produce an explicit token
         * (like whitespace or an entire comment).
         */
        private Token getNextToken() {
            int currentState = START_STATE;
            int tokenStartPos = pos;
            int tokenLine = line;

            StringBuilder lexemeBuilder = new StringBuilder();

            boolean inToken = true;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                int category = getCategory(c);

                // If we're in single-line or multi-line comment states,
                // we handle them specially. If we finalize them, we skip returning a token.
                if (currentState == 6) { // single-line comment
                    // We remain in state 6 until we hit a newline
                    if (c == '\n') {
                        // end single-line comment
                        pos++;
                        line++;
                        return null; // skip producing a token for the comment
                    }
                    pos++;
                    if (c == '\n') {
                        line++;
                    }
                    continue;
                }
                else if (currentState == 7) { // in multi-line comment
                    // we remain in 7 until we see star -> possible end => state 8
                    if (category == CAT_STAR) {
                        currentState = 8;
                    }
                    pos++;
                    if (c == '\n') line++;
                    continue;
                }
                else if (currentState == 8) { // saw '*' in multi-line
                    // slash => end comment => back to start
                    // star => stay in 8
                    // else => go to 7
                    if (category == CAT_SLASH) {
                        // end comment
                        pos++;
                        return null;
                    }
                    else if (category == CAT_STAR) {
                        // remain in 8
                        pos++;
                        continue;
                    }
                    else {
                        // go back to 7
                        currentState = 7;
                        pos++;
                        if (c == '\n') line++;
                        continue;
                    }
                }
                // If we are in double-quote or single-quote string states
                if (currentState == 9) {
                    // double-quote string
                    if (c == '\n') {
                        line++;
                    }
                    if (c == '\"') {
                        // end string
                        pos++;
                        // produce a STRING token
                        return new Token(TokenType.STRING, lexemeBuilder.toString(), tokenLine);
                    } else {
                        // add char to string
                        lexemeBuilder.append(c);
                        pos++;
                        continue;
                    }
                }
                if (currentState == 10) {
                    // single-quote string
                    if (c == '\n') {
                        line++;
                    }
                    if (c == '\'') {
                        pos++;
                        return new Token(TokenType.STRING, lexemeBuilder.toString(), tokenLine);
                    } else {
                        lexemeBuilder.append(c);
                        pos++;
                        continue;
                    }
                }

                // Normal transition
                int nextState = dfa[currentState][category];
                if (nextState == ERROR_STATE) {
                    // We have to finalize the token from the current state if it's valid
                    return finalizeToken(currentState, lexemeBuilder.toString(), tokenLine);
                }
                else {
                    // legit transition
                    if (currentState == START_STATE && category == CAT_WHITESPACE) {
                        // skip whitespace
                        if (c == '\n') {
                            line++;
                        }
                        pos++;
                        return null; // no token produced
                    }
                    // if we just moved into state 5 (slash?), we need to see next char to decide
                    if (currentState == 0 && nextState == 5) {
                        // We won't finalize yet. We'll see next char in next iteration
                    }
                    if (currentState == 5) {
                        // we are in slash state, we handle transitions for comment or operator
                        // if nextState is 6 => single line comment
                        // if nextState is 7 => multi line comment
                        // if nextState is 11 => that means it's just '/'
                    }

                    // If we just moved to operator state 11, produce that operator immediately
                    if (nextState == 11) {
                        // But if we are from state 0 or 5, we produce an OPERATOR token for c or '/'
                        // If it's state 5 and category wasn't slash or star, it's '/', produce OPERATOR
                        // We'll do that by skipping
                        lexemeBuilder.append(c);
                        pos++;
                        return new Token(TokenType.OPERATOR, lexemeBuilder.toString(), tokenLine);
                    }

                    // We'll accumulate the char into lexemeBuilder if it's part of ID or number
                    if (nextState == 1 || nextState == 2 || nextState == 4) {
                        // building ID or integer or float
                        lexemeBuilder.append(c);
                    }
                    else if (nextState == 3) {
                        // we saw dot after integer
                        lexemeBuilder.append(c);
                    }
                    // If nextState is 6/7/8/9/10, we handle above special code loops

                    // consume the char
                    pos++;
                    if (c == '\n') {
                        line++;
                    }
                    currentState = nextState;
                }
            } // while pos < src.length()

            // We reached end of file. Finalize the token from currentState
            return finalizeToken(currentState, lexemeBuilder.toString(), line);
        }

        /**
         * Decide which token to produce based on the final state.
         */
        private Token finalizeToken(int state, String lexeme, int tokenLine) {
            // If we never consumed anything, no token:
            if (state == START_STATE && lexeme.isEmpty()) {
                return null;
            }
            switch (state) {
                case 1: // identifier
                    return buildIdentifierOrKeywordToken(lexeme, tokenLine);
                case 2: // integer
                    return new Token(TokenType.INTEGER, lexeme, tokenLine);
                case 4: // float
                    // format to 5 decimals
                    try {
                        double d = Double.parseDouble(lexeme);
                        DecimalFormat df = new DecimalFormat("#.#####");
                        String val = df.format(d);
                        return new Token(TokenType.DECIMAL, val, tokenLine);
                    } catch (NumberFormatException e) {
                        errorHandler.addError("Invalid float " + lexeme + " at line " + tokenLine);
                        return new Token(TokenType.ERROR, lexeme, tokenLine);
                    }
                case 9: // unterminated double-quote string
                    errorHandler.addError("Unterminated string literal at line " + tokenLine);
                    return new Token(TokenType.ERROR, lexeme, tokenLine);
                case 10: // unterminated single-quote string
                    errorHandler.addError("Unterminated string literal at line " + tokenLine);
                    return new Token(TokenType.ERROR, lexeme, tokenLine);
                default:
                    // We might have a leftover if we ended in state 3 (just a '.' after integer)
                    // or we ended in slash or comment state, etc. All are errors or nothing
                    if (!lexeme.isEmpty()) {
                        // If it's just partial stuff, produce error:
                        errorHandler.addError("Invalid token '" + lexeme + "' at line " + tokenLine);
                        return new Token(TokenType.ERROR, lexeme, tokenLine);
                    }
            }
            return null;
        }

        /**
         * Checks if the lexeme is a keyword or boolean literal or otherwise an identifier
         */
        private Token buildIdentifierOrKeywordToken(String lex, int ln) {
            // Check if it's "true"/"false"
            if (lex.equals("true") || lex.equals("false")) {
                return new Token(TokenType.BOOLEAN, lex, ln);
            }
            // Check if it's any of our reserved keywords
            String[] keys = {
                    "int","bool","float","char","in","out","input","output","if","else","while",
                    "boolean","integer","decimal"
            };
            for (String k : keys) {
                if (k.equals(lex)) {
                    return new Token(TokenType.KEYWORD, lex, ln);
                }
            }
            // Otherwise it's an identifier
            return new Token(TokenType.IDENTIFIER, lex, ln);
        }

        /**
         * Convert a char to a category for the table
         */
        private int getCategory(char c) {
            if (c >= 'a' && c <= 'z') return CAT_LETTER;
            if (c >= '0' && c <= '9') return CAT_DIGIT;
            switch (c) {
                case '.': return CAT_DOT;
                case '/': return CAT_SLASH;
                case '*': return CAT_STAR;
                case '"': return CAT_QUOTE_DOUBLE;
                case '\'':return CAT_QUOTE_SINGLE;
                case '+': return CAT_PLUS;
                case '-': return CAT_MINUS;
                case '%': return CAT_PERCENT;
                case '^': return CAT_CARET;
                case '=': return CAT_EQUAL;
                case '<': return CAT_LT;
                case '>': return CAT_GT;
                case '(': return CAT_LPAREN;
                case ')': return CAT_RPAREN;
            }
            if (Character.isWhitespace(c)) {
                return CAT_WHITESPACE;
            }
            return CAT_OTHER;
        }

        /**
         * Post-process tokens to reclassify <operand> <operator> <operand> => ARITHMETIC
         */
        public List<Token> classifyArithmeticOperations(List<Token> originalTokens) {
            List<Token> result = new ArrayList<>();
            int i=0;
            while (i < originalTokens.size()) {
                if (i+2 < originalTokens.size() &&
                        isOperand(originalTokens.get(i)) &&
                        isArithmeticOp(originalTokens.get(i+1)) &&
                        isOperand(originalTokens.get(i+2))) {

                    Token first = originalTokens.get(i);
                    Token op    = originalTokens.get(i+1);
                    Token third = originalTokens.get(i+2);
                    String merged = first.lexeme + " " + op.lexeme + " " + third.lexeme;
                    result.add(new Token(TokenType.ARITHMETIC, merged, first.line));
                    i += 3;
                } else {
                    result.add(originalTokens.get(i));
                    i++;
                }
            }
            return result;
        }

        private boolean isOperand(Token t) {
            return t.type == TokenType.IDENTIFIER || t.type == TokenType.INTEGER || t.type == TokenType.DECIMAL;
        }
        private boolean isArithmeticOp(Token t) {
            if (t.type == TokenType.OPERATOR) {
                String x = t.lexeme;
                return x.equals("+") || x.equals("-") || x.equals("*") ||
                        x.equals("/") || x.equals("%") || x.equals("^");
            }
            return false;
        }
    }

    // ------------------- SYMBOL TABLE --------------------
    static class SymbolTable {
        private Map<String, String> table = new HashMap<>();

        public void add(String identifier, String type) {
            if (!table.containsKey(identifier)) {
                table.put(identifier, type);
            }
        }

        public void print() {
            System.out.println("Identifier\tType");
            for (Map.Entry<String, String> e : table.entrySet()) {
                System.out.println(e.getKey() + "\t\t" + e.getValue());
            }
        }
    }

    // ------------------- ERROR HANDLER --------------------
    static class ErrorHandler {
        private final List<String> errors = new ArrayList<>();

        public void addError(String message) {
            errors.add(message);
        }
        public List<String> getErrors() {
            return errors;
        }
    }
}
