public class Main {
    public static void main(String[] args) {
        // Pass the filename "test.rizz" (or any source) to our compiler
        RizzLang compiler = new RizzLang("test.rizz");
        compiler.run();
    }
}
