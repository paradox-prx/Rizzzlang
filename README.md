# RizzLang — User Manual

**RizzLang** is a toy programming language designed to demonstrate compiler front-end concepts (lexical analysis and tokenization) using a single deterministic finite automaton (DFA). This README details the keywords, rules, and usage of RizzLang.

## 1. Language Features

1. **Data Types**
    - `int` (integer)
    - `bool` (boolean)
    - `float` (floating-point)
    - `char` (character)
    - `string` (implied by string literal support, though not formally declared as a keyword)

2. **Keywords**
    - **Data type keywords:** `int`, `bool`, `float`, `char`, `boolean`, `integer`, `decimal`
    - **Input/Output keywords:**
        - `in(` and `out(`: Treated as special tokens if followed immediately by `(`, e.g., `in(x)` or `out("Hello")`.
    - **Boolean literals:** `true` and `false` recognized as special (boolean) tokens.

3. **Identifiers**
    - Only lowercase letters (`a-z`) are allowed for variable names, function names, etc.
    - No digits or underscores are permitted in identifiers.

4. **Numbers**
    - **Integers**: Sequences of digits (e.g., `123`).
    - **Decimals**: An integer part followed by a dot and fractional digits (e.g., `3.14159`).
    - Decimal numbers are rounded to 5 decimal places internally.
    - Exponents can be handled at a later stage if desired, but exponent operators (like `^`) are recognized.

5. **String Literals**
    - Enclosed in double quotes (`"..."`) or single quotes (`'...'`).
    - Support for any characters until a matching quote is encountered.
    - If the end quote is missing, the lexer reports an **unclosed string** error.

6. **Operators**
    - **Arithmetic operators**: `+`, `-`, `*`, `/`, `%`, `^`
    - **Comparison/assignment operators**: `=`, `<`, `>`
    - **Grouping**: `(` and `)`

7. **Comments**
    - **Single-line comments**: `//` until end of line.
    - **Multi-line comments**: `/* ... */` which can span multiple lines.
    - Unclosed multi-line comments produce an **error**.

8. **Whitespace**
    - Extra spaces, tabs, and newlines are ignored (except within string literals).
    - Newline characters increment the current line number for error reporting.

9. **Arithmetic Reclassification**
    - After tokenization, any sequence `<operand> <operator> <operand>` is merged into a single `ARITHMETIC` token.
    - Operands include integer, decimal, or identifier tokens. Operators are `+`, `-`, `*`, `/`, `%`, or `^`.

10. **Error Handling**
    - Unrecognized characters produce an **ERROR** token, plus a message with the line number.
    - Unterminated strings or comments also produce errors with line information.
    - The lexer never “gets stuck”; it always progresses or generates an error.

## 2. Example Program (test.rizz)

An example RizzLang file **test.rizz** might look like:

```plaintext
// This is a single-line comment

/* This is a 
   multi-line comment */

int x
x = 10

float y
y = 3.1415926

out("Value of y is: ")
out(y)

// Arithmetic test
x + y
