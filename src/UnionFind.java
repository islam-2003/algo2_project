import java.util.HashMap;
import java.util.Map;

public class UnionFind {
    
    private Map<String, String> parent;
    private Map<String, Integer> rank;
    private Map<String, Integer> size;
    private int communityCount;

    public UnionFind() {
        parent = new HashMap<>();
        rank = new HashMap<>();
        size = new HashMap<>();
        communityCount = 0;
    }

    public void add(String element) {

        if(!parent.containsKey(element)){
            parent.put(element, element);
            rank.put(element, 0);
            size.put(element, 1);
            communityCount++;
        }
    }
}
