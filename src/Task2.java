import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;

public class Task2 {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("""
                Usage:
                  java -Xmx2g Task2 <dblp.xml|dblp.xml.gz> <dblp.dtd> [--limit=1000000]

                Exemple:
                  java -Xmx2g Task2 dblp.xml.gz dblp.dtd --limit=500000
                """);
            System.exit(2);
        }

        Path xmlPath = Paths.get(args[0]);
        Path dtdPath = Paths.get(args[1]);

        long limit = Long.MAX_VALUE;
        for (int i = 2; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--limit=")) limit = Long.parseLong(a.substring("--limit=".length()));
        }

        if (!Files.exists(xmlPath)) throw new FileNotFoundException("XML introuvable: " + xmlPath);
        if (!Files.exists(dtdPath)) throw new FileNotFoundException("DTD introuvable: " + dtdPath);
        
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxParameterEntitySizeLimit", "0");

        // ============================================================
        // ÉTAPE 1 : Comptage des paires (A → B) en ligne
        // ============================================================
        // Structure : pour chaque auteur A, on stocke une map A → compteur
        Map<String, Map<String, Integer>> pairCount = new HashMap<>();
        
        long pubCount = 0;
        long startTime = System.currentTimeMillis();

        try (DblpPublicationGenerator gen = new DblpPublicationGenerator(xmlPath, dtdPath, 256)) {
            
            while (pubCount < limit) {
                var opt = gen.nextPublication();
                if (opt.isEmpty()) break;
                
                pubCount++;
                
                // Affichage toutes les 100k publications
                if (pubCount % 100000 == 0) {
                    System.out.println("Publications traitées: " + pubCount);
                    System.out.println("Nombre de paires uniques: " + pairCount.size());
                }
                
                var pub = opt.get();
                List<String> authors = pub.authors;
                
                // On ignore les publications avec moins de 2 auteurs
                if (authors == null || authors.size() < 2) continue;
                
                // Premier auteur (A)
                String firstAuthor = authors.get(0);
                
                // Initialiser la map pour ce premier auteur si nécessaire
                pairCount.putIfAbsent(firstAuthor, new HashMap<>());
                Map<String, Integer> coauthorCounts = pairCount.get(firstAuthor);
                
                // Pour chaque co-auteur B (positions 1, 2, ...)
                for (int i = 1; i < authors.size(); i++) {
                    String coauthor = authors.get(i);
                    // Incrémenter le compteur pour la paire (A → B)
                    coauthorCounts.put(coauthor, coauthorCounts.getOrDefault(coauthor, 0) + 1);
                }
            }
        }
        
        long parsingTime = System.currentTimeMillis() - startTime;
        System.out.println("\n=== FIN DU PARSING ===");
        System.out.println("Publications traitées: " + pubCount);
        System.out.println("Temps de parsing: " + parsingTime / 1000 + " secondes");
        System.out.println("Nombre d'auteurs uniques (source): " + pairCount.size());
        
        // ============================================================
        // ÉTAPE 2 : Construction du graphe orienté filtré (seuil ≥ 6)
        // ============================================================
        System.out.println("\n=== CONSTRUCTION DU GRAPHE ORIENTÉ ===");
        
        // Graphe orienté : pour chaque auteur, liste des voisins sortants
        Map<String, List<String>> graph = new HashMap<>();
        // Ensemble de tous les auteurs (sommets)
        Set<String> allAuthors = new HashSet<>();
        
        int edgesKept = 0;
        int edgesFiltered = 0;
        
        for (var entry : pairCount.entrySet()) {
            String from = entry.getKey();
            allAuthors.add(from);
            
            for (var toEntry : entry.getValue().entrySet()) {
                String to = toEntry.getKey();
                int count = toEntry.getValue();
                
                allAuthors.add(to);
                
                if (count >= 6) {
                    // Conserver l'arête
                    graph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
                    edgesKept++;
                } else {
                    edgesFiltered++;
                }
            }
        }
        
        System.out.println("Nombre total de sommets: " + allAuthors.size());
        System.out.println("Arêtes conservées (≥6): " + edgesKept);
        System.out.println("Arêtes filtrées (<6): " + edgesFiltered);
        
        // ============================================================
        // ÉTAPE 3 : Détection des composantes fortement connexes (Kosaraju)
        // ============================================================
        System.out.println("\n=== DÉTECTION DES CFC ===");
        
        // Créer le graphe transposé (arêtes inversées)
        Map<String, List<String>> reverseGraph = new HashMap<>();
        for (var entry : graph.entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue()) {
                reverseGraph.computeIfAbsent(to, k -> new ArrayList<>()).add(from);
            }
        }
        
        // Étape 1 : DFS pour obtenir l'ordre de finition
        Set<String> visited = new HashSet<>();
        List<String> order = new ArrayList<>();
        
        for (String author : allAuthors) {
            if (!visited.contains(author) && graph.containsKey(author)) {
                dfs1(author, graph, visited, order);
            }
        }
        
        // Étape 2 : Parcours dans l'ordre inverse sur le graphe transposé
        visited.clear();
        List<Set<String>> components = new ArrayList<>();
        
        for (int i = order.size() - 1; i >= 0; i--) {
            String author = order.get(i);
            if (!visited.contains(author)) {
                Set<String> component = new HashSet<>();
                dfs2(author, reverseGraph, visited, component);
                if (component.size() > 1) { // On garde seulement les CFC de taille ≥ 2
                    components.add(component);
                }
            }
        }
        
        // Trier les communautés par taille (décroissant)
        components.sort((a, b) -> Integer.compare(b.size(), a.size()));
        
        System.out.println("Nombre de CFC (taille ≥ 2): " + components.size());
        
        // Afficher les 10 plus grandes
        System.out.println("\n=== TOP 10 DES CFC ===");
        for (int i = 0; i < Math.min(10, components.size()); i++) {
            Set<String> comp = components.get(i);
            System.out.println((i+1) + ". Taille: " + comp.size());
            // Afficher les 5 premiers noms pour donner un aperçu
            List<String> names = new ArrayList<>(comp);
            System.out.println("   Exemples: " + names.subList(0, Math.min(5, names.size())));
        }
        
        // Sauvegarder les résultats
        saveResults(components, graph, "resultats_tache2.txt");
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("\nTemps total d'exécution: " + totalTime / 1000 + " secondes");
    }
    
    // Premier DFS pour Kosaraju (ordre de finition)
    private static void dfs1(String node, Map<String, List<String>> graph, 
                              Set<String> visited, List<String> order) {
        visited.add(node);
        List<String> neighbors = graph.get(node);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    dfs1(neighbor, graph, visited, order);
                }
            }
        }
        order.add(node);
    }
    
    // Second DFS pour Kosaraju (extraire une CFC)
    private static void dfs2(String node, Map<String, List<String>> reverseGraph,
                              Set<String> visited, Set<String> component) {
        visited.add(node);
        component.add(node);
        List<String> neighbors = reverseGraph.get(node);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    dfs2(neighbor, reverseGraph, visited, component);
                }
            }
        }
    }
    
    // Sauvegarde des résultats
    private static void saveResults(List<Set<String>> components, 
                                     Map<String, List<String>> graph, 
                                     String filename) throws Exception {
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.println("=== RÉSULTATS TÂCHE 2 ===\n");
            writer.println("Nombre de CFC (taille ≥ 2): " + components.size() + "\n");
            
            for (int i = 0; i < Math.min(10, components.size()); i++) {
                Set<String> comp = components.get(i);
                writer.println((i+1) + ". Taille: " + comp.size());
                writer.println("   Membres: " + comp);
                writer.println();
            }
        }
        System.out.println("Résultats sauvegardés dans: " + filename);
    }
}

