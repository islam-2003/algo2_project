import java.io.FileNotFoundException;
import java.nio.file.*;
import java.util.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import java.io.File;

public class Task1 {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("""
                Usage:
                  java -Xmx2g DblpParsingDemo <dblp.xml|dblp.xml.gz> <dblp.dtd> [--limit=1000000]

                Exemple:
                  java -Xmx2g DblpParsingDemo dblp.xml.gz dblp.dtd --limit=500000
                """);
            System.exit(2);
        }

        Path xmlPath = Paths.get(args[0]);
        Path dtdPath = Paths.get(args[1]);

        long limit = Long.MAX_VALUE; // optionnel: s'arrêter après N publications
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

        UnionFind uf = new UnionFind();
        long pubCount = 0;

        try (DblpPublicationGenerator gen = new DblpPublicationGenerator(xmlPath, dtdPath, 256)) {
            // Boucle de consommation : on traite les publications une par une,
            // jusqu'à atteindre la limite (si fournie) ou la fin du fichier.
            while (pubCount < limit) {
                
                // nextPublication() renvoie :
                //   - Optional.of(pub) si une publication est disponible ;
                //   - Optional.empty() si on a atteint la fin du flux (EOF).
                //
                // Cela évite d'utiliser null et oblige à gérer explicitement le cas EOF.
                Optional<DblpPublicationGenerator.Publication> opt = gen.nextPublication();
                if (opt.isEmpty()) break; // EOF

                pubCount++;
                if (pubCount % 100000 == 0) {
                    System.out.println("Publications traitées: " + pubCount);
                    System.out.println("nombre de communités:" + uf.getCommunityCount());
                    List<Integer> topSizes = new ArrayList<>(uf.getSizes().values());
                    Collections.sort(topSizes, Collections.reverseOrder());
                    topSizes = topSizes.subList(0, Math.min(10, topSizes.size()));
                    System.out.println("Top 10 tailles : " + topSizes);
                }
                DblpPublicationGenerator.Publication p = opt.get();

                List<String> authors = p.authors;
                if (authors == null || authors.isEmpty()) {
                    continue;
                }

                
                for (String a : authors) {
                    uf.add(a);
                }

                // Unir les auteurs de la publication
                for (int i = 1; i < authors.size(); i++) {
                    uf.union(authors.get(0), authors.get(i));
                }


            }
        }
        Map<Integer, Integer> histogram = new TreeMap<>(); // On utilise TreeMap pour trier Les tailles.
            for (int s : uf.getSizes().values()) {
                if (histogram.containsKey(s)) {
                    histogram.put(s, histogram.get(s) + 1); // incrémenter le compte pour cette taille
                } else {
                    histogram.put(s, 1); // ajouter la taille avec un compte initial de 1
            }
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
                dataset.addValue(
                entry.getValue(),    // nombre de communautés
                "Communautés",       // nom de la série
                entry.getKey()       // taille de la communauté (axe X)
                );
            }
        
        try {
            JFreeChart chart = ChartFactory.createBarChart(
                "Histogramme des tailles des communautés",
                "Taille",
                "Nombre de communautés",
                dataset
            );
            File outputDir = new File("../output");
            outputDir.mkdirs();
            ChartUtilities.saveChartAsPNG(
                new File(outputDir, "histogram_task1.png"),
                chart,
                800,
                600
            );
            System.out.println("Histogramme sauvegardé dans output/histogram_task1.png");
            } catch (Exception e) {
            System.err.println("Erreur lors de la génération du graphique : " + e.getMessage());
            e.printStackTrace();
            }
}
}

