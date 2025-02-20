import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
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
