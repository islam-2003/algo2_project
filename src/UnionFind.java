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

    public void add(String author) {

        if(!parent.containsKey(author)){
            parent.put(author, author);
            rank.put(author, 0);
            size.put(author, 1);
            communityCount++;
        }
    }

    public String find(String author){
        
        String root = author;
        while (!root.equals(parent.get(root))) {
            root = parent.get(root);
        }
        while (!author.equals(root)){
            
            String newAuthor = parent.get(author);
            parent.put(author, root);
            author = newAuthor;
        }
        return root;
    }

    public void union(String author1, String author2){

        String root1 = find(author1);
        String root2 = find(author2);
        if (root1.equals(root2)) return;
        if (rank.get(root1) < rank.get(root2)) {
            parent.put(root1, root2);
            size.put(root2, size.get(root2) + size.get(root1));
        } else if (rank.get(root1) > rank.get(root2)) {
            parent.put(root2, root1);
            size.put(root1, size.get(root1) + size.get(root2));
        } else {
            parent.put(root2, root1);
            rank.put(root1, rank.get(root1) + 1);
            size.put(root1, size.get(root1) + size.get(root2));
        }
        communityCount--;
    }

    public int getCommunitySize(String author){
        String root = find(author);
        return size.get(root);
    }

    public int getCommunityCount(){
        return communityCount;
    }

    public Map<String, Integer> getSizes(){
        return size;
    }

    public Map<String, String> getParent(){
        return parent;
    }
}
