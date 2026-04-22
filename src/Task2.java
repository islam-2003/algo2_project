import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

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
            if (a.startsWith("--limit="))
                limit = Long.parseLong(a.substring("--limit=".length()));
        }

        if (!Files.exists(xmlPath))
            throw new FileNotFoundException("XML introuvable: " + xmlPath);
        if (!Files.exists(dtdPath))
            throw new FileNotFoundException("DTD introuvable: " + dtdPath);

        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxParameterEntitySizeLimit", "0");

        // ============================================================
        // ÉTAPE 1 : Comptage des paires (A → B) en ligne
        // ============================================================
        Map<String, Map<String, Integer>> pairCount = new HashMap<>();

        long pubCount = 0;
        long startTime = System.currentTimeMillis();

        try (DblpPublicationGenerator gen = new DblpPublicationGenerator(xmlPath, dtdPath, 256)) {

            while (pubCount < limit) {
                var opt = gen.nextPublication();
                if (opt.isEmpty())
                    break;

                pubCount++;

                if (pubCount % 100000 == 0) {
                    System.out.println("Publications traitées: " + pubCount);
                    System.out.println("Nombre de paires uniques: " + pairCount.size());
                }

                var pub = opt.get();
                List<String> authors = pub.authors;

                if (authors == null || authors.size() < 2)
                    continue;

                String firstAuthor = authors.get(0);

                pairCount.putIfAbsent(firstAuthor, new HashMap<>());
                Map<String, Integer> coauthorCounts = pairCount.get(firstAuthor);

                for (int i = 1; i < authors.size(); i++) {
                    String coauthor = authors.get(i);
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

        Map<String, List<String>> graph = new HashMap<>();
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

        Map<String, List<String>> reverseGraph = new HashMap<>();
        for (var entry : graph.entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue()) {
                reverseGraph.computeIfAbsent(to, k -> new ArrayList<>()).add(from);
            }
        }

        Set<String> visited = new HashSet<>();
        List<String> order = new ArrayList<>();

        for (String author : allAuthors) {
            if (!visited.contains(author) && graph.containsKey(author)) {
                dfs1(author, graph, visited, order);
            }
        }

        visited.clear();
        List<Set<String>> components = new ArrayList<>();

        for (int i = order.size() - 1; i >= 0; i--) {
            String author = order.get(i);
            if (!visited.contains(author)) {
                Set<String> component = new HashSet<>();
                dfs2(author, reverseGraph, visited, component);
                if (component.size() > 1) {
                    components.add(component);
                }
            }
        }

        components.sort((a, b) -> Integer.compare(b.size(), a.size()));

        System.out.println("Nombre de CFC (taille ≥ 2): " + components.size());

        // ============================================================
        // AFFICHAGE TOP 10 AVEC DIAMÈTRES
        // ============================================================
        System.out.println("\n=== TOP 10 DES CFC AVEC DIAMÈTRE ===");
        for (int i = 0; i < Math.min(10, components.size()); i++) {
            Set<String> comp = components.get(i);
            int diameter = computeDiameter(comp, graph);
            System.out.println((i + 1) + ". Taille: " + comp.size() + ", Diamètre: " + diameter);
            List<String> names = new ArrayList<>(comp);
            System.out.println("   Exemples: " + names.subList(0, Math.min(5, names.size())));
        }

        // ============================================================
        // HISTOGRAMME DES TAILLES DES CFC
        // ============================================================
        System.out.println("\n=== HISTOGRAMME DES TAILLES DES CFC ===");

        Map<Integer, Integer> sizeHistogram = new TreeMap<>();
        for (Set<String> comp : components) {
            int size = comp.size();
            sizeHistogram.put(size, sizeHistogram.getOrDefault(size, 0) + 1);
        }

        System.out.println("Taille -> Nombre de CFC :");
        for (var entry : sizeHistogram.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }

        // Sauvegarder l'histogramme
        try (PrintWriter writer = new PrintWriter("histogramme_cfc.txt")) {
            writer.println("# Taille_CFC Nombre_CFC");
            for (var entry : sizeHistogram.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        }
        System.out.println("Histogramme sauvegardé dans: histogramme_cfc.txt");

        // ============================================================
        // SAUVEGARDE DES RÉSULTATS COMPLETS
        // ============================================================
        saveResultsWithDiameter(components, graph, "resultats_tache2_complet.txt");

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("\nTemps total d'exécution: " + totalTime / 1000 + " secondes");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<Integer, Integer> entry : sizeHistogram.entrySet()) {
            dataset.addValue(
                    entry.getValue(), // nombre de communautés
                    "Communautés", // nom de la série
                    entry.getKey() // taille de la communauté (axe X)
            );
        }

        try {
            JFreeChart chart = ChartFactory.createBarChart(
                    "Histogramme des tailles des communautés",
                    "Taille",
                    "Nombre de communautés",
                    dataset);
            File outputDir = new File("../output");
            outputDir.mkdirs();
            ChartUtilities.saveChartAsPNG(
                    new File(outputDir, "histogram_task2.png"),
                    chart,
                    800,
                    600);
            System.out.println("Histogramme sauvegardé dans output/histogram_task2.png");
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du graphique : " + e.getMessage());
            e.printStackTrace();
        }
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

    // Calcul du diamètre d'une composante (BFS depuis chaque sommet)
    private static int computeDiameter(Set<String> component, Map<String, List<String>> graph) {
        if (component.size() <= 1)
            return 0;

        int diameter = 0;
        List<String> nodes = new ArrayList<>(component);

        for (String start : nodes) {
            Map<String, Integer> distances = new HashMap<>();
            Queue<String> queue = new LinkedList<>();

            distances.put(start, 0);
            queue.add(start);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                int currentDist = distances.get(current);

                List<String> neighbors = graph.get(current);
                if (neighbors != null) {
                    for (String neighbor : neighbors) {
                        if (!distances.containsKey(neighbor) && component.contains(neighbor)) {
                            distances.put(neighbor, currentDist + 1);
                            queue.add(neighbor);
                        }
                    }
                }
            }

            for (int dist : distances.values()) {
                if (dist > diameter) {
                    diameter = dist;
                }
            }
        }
        return diameter;
    }

    // Sauvegarde des résultats avec diamètres
    private static void saveResultsWithDiameter(List<Set<String>> components,
            Map<String, List<String>> graph,
            String filename) throws Exception {
        try (PrintWriter writer = new PrintWriter(filename)) {
            writer.println("=== RÉSULTATS TÂCHE 2 AVEC DIAMÈTRES ===\n");
            writer.println("Nombre de CFC (taille >= 2): " + components.size() + "\n");

            for (int i = 0; i < Math.min(10, components.size()); i++) {
                Set<String> comp = components.get(i);
                int diameter = computeDiameter(comp, graph);
                writer.println((i + 1) + ". Taille: " + comp.size() + ", Diamètre: " + diameter);
                writer.println("   Membres: " + comp);
                writer.println();
            }
        }
        System.out.println("Résultats complets sauvegardés dans: " + filename);
    }
}
