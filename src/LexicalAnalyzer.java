import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class LexicalAnalyzer {
    private final String src;
    private int pos = 0;
    private int line = 1;
    private final int[][] dfa;
    private final int START_STATE = 0;
    private final int ERROR_STATE = -1;

    private final int STATE_ID = 1;           // in identifier
    private final int STATE_INT = 2;          // in integer
    private final int STATE_FLOAT_FRAC = 4;   // in fractional part
    private final int STATE_SL_COMMENT = 6;   // single-line comment
    private final int STATE_ML_COMMENT = 7;   // multi-line comment
    private final int STATE_ML_COMMENT_STAR = 8; // saw '*' inside multi-line
    private final int STATE_STR_DQ = 9;       // double-quote string
    private final int STATE_STR_SQ = 10;      // single-quote string
    private final int STATE_OPERATOR = 11;    // operator

    private static final int CAT_LETTER       = 0;  // a-z
    private static final int CAT_DIGIT        = 1;  // 0-9
    private static final int CAT_DOT          = 2;  // '.'
    private static final int CAT_SLASH        = 3;  // '/'
    private static final int CAT_STAR         = 4;  // '*'
    private static final int CAT_QUOTE_DOUBLE = 5;  // '"'
    private static final int CAT_QUOTE_SINGLE = 6;  // '\''
    private static final int CAT_PLUS         = 7;  // '+'
    private static final int CAT_MINUS        = 8;  // '-'
    private static final int CAT_PERCENT      = 9;  // '%'
    private static final int CAT_CARET        = 10; // '^'
    private static final int CAT_EQUAL        = 11; // '='
    private static final int CAT_LT           = 12; // '<'
    private static final int CAT_GT           = 13; // '>'
    private static final int CAT_LPAREN       = 14; // '('
    private static final int CAT_RPAREN       = 15; // ')'
    private static final int CAT_WHITESPACE   = 16; // space, tab, \n
    private static final int CAT_OTHER        = 17; // everything else

    private static final int NUM_STATES = 12;
    private static final int NUM_COLS   = 18;

    private ErrorHandler errorHandler = new ErrorHandler();

    public LexicalAnalyzer(String source) {
        this.src = source;
        this.dfa = buildDFATable();
    }

    private int[][] buildDFATable() {
        int[][] table = new int[NUM_STATES][NUM_COLS];
        // Initialize all to -1
        for (int i=0; i<NUM_STATES; i++) {
            for (int j=0; j<NUM_COLS; j++) {
                table[i][j] = ERROR_STATE;
            }
        }

        // State 0: START
        table[0][CAT_LETTER]       = STATE_ID;       // start ID
        table[0][CAT_DIGIT]        = STATE_INT;      // start integer
        table[0][CAT_SLASH]        = 5;              // slash? => comment or operator
        table[0][CAT_STAR]         = STATE_OPERATOR; // operator: '*'
        table[0][CAT_QUOTE_DOUBLE] = STATE_STR_DQ;   // "...
        table[0][CAT_QUOTE_SINGLE] = STATE_STR_SQ;   // '...
        table[0][CAT_PLUS]         = STATE_OPERATOR; // '+'
        table[0][CAT_MINUS]        = STATE_OPERATOR; // '-'
        table[0][CAT_PERCENT]      = STATE_OPERATOR; // '%'
        table[0][CAT_CARET]        = STATE_OPERATOR; // '^'
        table[0][CAT_EQUAL]        = STATE_OPERATOR; // '='
        table[0][CAT_LT]           = STATE_OPERATOR; // '<'
        table[0][CAT_GT]           = STATE_OPERATOR; // '>'
        table[0][CAT_LPAREN]       = STATE_OPERATOR; // '('
        table[0][CAT_RPAREN]       = STATE_OPERATOR; // ')'
        // whitespace => remain in 0
        table[0][CAT_WHITESPACE]   = 0;

        table[STATE_ID][CAT_LETTER] = STATE_ID;  // keep reading letters

        table[STATE_INT][CAT_DIGIT] = STATE_INT; // keep reading digits
        table[STATE_INT][CAT_DOT]   = 3;         // see a dot => next might be float

        table[3][CAT_DIGIT] = STATE_FLOAT_FRAC;

        table[STATE_FLOAT_FRAC][CAT_DIGIT] = STATE_FLOAT_FRAC;


        for (int cat=0; cat<NUM_COLS; cat++) {
            table[5][cat] = STATE_OPERATOR; // default => operator
        }
        table[5][CAT_SLASH] = STATE_SL_COMMENT;
        table[5][CAT_STAR]  = STATE_ML_COMMENT;

        for (int cat=0; cat<NUM_COLS; cat++) {
            table[STATE_SL_COMMENT][cat] = STATE_SL_COMMENT;
        }

        for (int cat=0; cat<NUM_COLS; cat++) {
            table[STATE_ML_COMMENT][cat] = STATE_ML_COMMENT;
        }
        table[STATE_ML_COMMENT][CAT_STAR] = STATE_ML_COMMENT_STAR;

        for (int cat=0; cat<NUM_COLS; cat++) {
            table[STATE_ML_COMMENT_STAR][cat] = STATE_ML_COMMENT;
        }
        table[STATE_ML_COMMENT_STAR][CAT_SLASH] = 0;   // close comment
        table[STATE_ML_COMMENT_STAR][CAT_STAR]  = STATE_ML_COMMENT_STAR;

        for (int cat=0; cat<NUM_COLS; cat++) {
            table[STATE_STR_DQ][cat] = STATE_STR_DQ;
            table[STATE_STR_SQ][cat] = STATE_STR_SQ;
        }

        for (int cat=0; cat<NUM_COLS; cat++) {
            table[STATE_OPERATOR][cat] = ERROR_STATE;
        }

        return table;
    }


    public void printDFATable() {
        String[] stateNames = {
                "0(START)","1(ID)","2(INT)","3(DOT->FLOAT)","4(FLOAT_FRAC)",
                "5(SLASH?)","6(SL_COMMENT)","7(MULTILINE)","8(MULTI_STAR)",
                "9(STR_DQ)","10(STR_SQ)","11(OPERATOR)"
        };
        String[] colNames = {
                "LETTER","DIGIT","DOT","/","*","\"","'","+","-","%","^","=","<",">","(",")","WS","OTHER"
        };

        System.out.println("DFA Transition Table (rows=states, cols=categories):");
        System.out.print("       ");
        for (String c : colNames) {
            System.out.printf("%-8s", c);
        }
        System.out.println();

        for (int s=0; s<NUM_STATES; s++) {
            System.out.printf("%-8s", stateNames[s]);
            for (int c=0; c<NUM_COLS; c++) {
                int nxt = dfa[s][c];
                if (nxt == ERROR_STATE) System.out.printf("%-8s","ERR");
                else System.out.printf("%-8s", nxt);
            }
            System.out.println();
        }
    }


    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < src.length()) {
            Token t = getNextToken();
            if (t != null) {
                tokens.add(t);
            }
        }
        return tokens;
    }


    private Token getNextToken() {
        int currentState = START_STATE;
        int tokenLine = line;
        StringBuilder lexeme = new StringBuilder();

        while (pos < src.length()) {
            char c = src.charAt(pos);
            int cat = getCategory(c);

            // special single-line comment logic
            if (currentState == STATE_SL_COMMENT) {
                if (c == '\n') {
                    pos++;
                    line++;
                    // end single-line comment
                    return null;
                }
                else {
                    pos++;
                    if (c == '\n') line++;
                    continue;
                }
            }
            if (currentState == STATE_ML_COMMENT || currentState == STATE_ML_COMMENT_STAR) {
                int nxt = dfa[currentState][cat];
                pos++;
                if (c == '\n') line++;
                if (nxt == ERROR_STATE) {

                    continue;
                } else {
                    currentState = nxt;
                    if (currentState == START_STATE) {
                        return null;
                    }
                    continue;
                }
            }

            if (currentState == STATE_STR_DQ) {
                if (c == '\n') {
                    line++;
                }
                if (c == '"') {
                    pos++;
                    return new Token(TokenType.STRING, lexeme.toString(), tokenLine);
                }
                else {
                    lexeme.append(c);
                    pos++;
                    continue;
                }
            }
            if (currentState == STATE_STR_SQ) {
                if (c == '\n') {
                    line++;
                }
                if (c == '\'') {
                    pos++;
                    return new Token(TokenType.STRING, lexeme.toString(), tokenLine);
                }
                else {
                    lexeme.append(c);
                    pos++;
                    continue;
                }
            }

            int nxtState = dfa[currentState][cat];
            if (nxtState == ERROR_STATE) {
                Token tk = finalizeToken(currentState, lexeme.toString(), tokenLine);
                if (tk != null) {
                    return tk;
                } else {
                    // produce an error token for 'c'
                    errorHandler.addError("Unexpected char '" + c + "' at line " + line);
                    pos++;
                    if (c == '\n') line++;
                    return new Token(TokenType.ERROR, ""+c, line);
                }
            } else {
                if (currentState == START_STATE && cat == CAT_WHITESPACE) {
                    // skip
                    pos++;
                    if (c == '\n') line++;
                    return null;
                }
                if (nxtState == STATE_OPERATOR) {
                    pos++;
                    if (c == '\n') line++;
                    return new Token(TokenType.OPERATOR, String.valueOf(c), line);
                }
                if (nxtState == STATE_ID || nxtState == STATE_INT || nxtState == STATE_FLOAT_FRAC) {
                    lexeme.append(c);
                }
                pos++;
                if (c == '\n') line++;
                currentState = nxtState;
            }
        }
        if (currentState == STATE_ML_COMMENT || currentState == STATE_ML_COMMENT_STAR) {
            errorHandler.addError("Unclosed multi-line comment at line " + line);
            return new Token(TokenType.ERROR, "Unclosed comment", line);
        }
        if (currentState == STATE_STR_DQ) {
            errorHandler.addError("Unclosed double-quoted string at line " + line);
            return new Token(TokenType.ERROR, lexeme.toString(), line);
        }
        if (currentState == STATE_STR_SQ) {
            errorHandler.addError("Unclosed single-quoted string at line " + line);
            return new Token(TokenType.ERROR, lexeme.toString(), line);
        }

        Token tk = finalizeToken(currentState, lexeme.toString(), tokenLine);
        return tk;
    }


    private Token finalizeToken(int state, String lex, int ln) {
        if (lex.isEmpty() && state == START_STATE) {
            return null;
        }
        switch (state) {
            case STATE_ID:
                return buildIdentifierOrKeyword(lex, ln);
            case STATE_INT:
                return new Token(TokenType.INTEGER, lex, ln);
            case STATE_FLOAT_FRAC:
                try {
                    double d = Double.parseDouble(lex);
                    DecimalFormat df = new DecimalFormat("#.#####");
                    String val = df.format(d);
                    return new Token(TokenType.DECIMAL, val, ln);
                } catch (NumberFormatException e) {
                    errorHandler.addError("Invalid decimal '" + lex + "' at line " + ln);
                    return new Token(TokenType.ERROR, lex, ln);
                }
            default:
                return null;
        }
    }

    private Token buildIdentifierOrKeyword(String lex, int ln) {
        if (lex.equals("true") || lex.equals("false")) {
            return new Token(TokenType.BOOLEAN, lex, ln);
        }
        String[] keys = {
                "int","bool","float","char","in","out","input","output","if","else","while",
                "boolean","integer","decimal"
        };
        for (String k : keys) {
            if (k.equals(lex)) {
                return new Token(TokenType.KEYWORD, lex, ln);
            }
        }
        return new Token(TokenType.IDENTIFIER, lex, ln);
    }

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

    public List<Token> classifyArithmeticOperations(List<Token> originalTokens) {
        List<Token> result = new ArrayList<>();
        int i = 0;
        while (i < originalTokens.size()) {
            if (i+2 < originalTokens.size() &&
                    isOperand(originalTokens.get(i)) &&
                    isArithmeticOp(originalTokens.get(i+1)) &&
                    isOperand(originalTokens.get(i+2))) {
                Token first = originalTokens.get(i);
                Token mid   = originalTokens.get(i+1);
                Token last  = originalTokens.get(i+2);
                String merged = first.getLexeme() + " " + mid.getLexeme() + " " + last.getLexeme();
                result.add(new Token(TokenType.ARITHMETIC, merged, first.getLine()));
                i += 3;
            } else {
                result.add(originalTokens.get(i));
                i++;
            }
        }
        return result;
    }

    private boolean isOperand(Token t) {
        return (t.getType() == TokenType.IDENTIFIER
                || t.getType() == TokenType.INTEGER
                || t.getType() == TokenType.DECIMAL);
    }
    private boolean isArithmeticOp(Token t) {
        if (t.getType() == TokenType.OPERATOR) {
            String x = t.getLexeme();
            return x.equals("+") || x.equals("-") || x.equals("*")
                    || x.equals("/") || x.equals("%") || x.equals("^");
        }
        return false;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
}
