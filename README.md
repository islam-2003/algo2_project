# Projet Algorithmique 2 – Détection de communautés dans DBLP

Projet du cours **INFO-F-203 – Algorithmique 2** (ULB, 2025-2026).

Analyse de la base de données bibliographique DBLP pour détecter des communautés
de chercheurs selon deux modèles complémentaires : graphe non-orienté (Tâche 1)
et graphe orienté (Tâche 2).

---

## Structure du projet

```
algo2_project-tache2/
├── src/
│   ├── DblpPublicationGenerator.java   # Parseur DBLP fourni (ne pas modifier)
│   ├── DblpParsingDemo.java            # Démo d'utilisation du parseur (fournie)
│   ├── UnionFind.java                  # Structure Union-Find (Tâche 1)
│   ├── Task1.java                      # Tâche 1 : communautés non-orientées
│   ├── Task2.java                      # Tâche 2 : CFC dans le graphe orienté
│   ├── dblp-2026-01-01.xml.gz          # Données DBLP (à télécharger, voir ci-dessous)
│   └── dblp.dtd                        # DTD officielle DBLP
├── output/                             # Fichiers de sortie générés automatiquement
│   ├── histogram_task1.png             # Histogramme Tâche 1 (PNG)
│   ├── histogram_task2.png             # Histogramme Tâche 2 (PNG)
│   ├── histogramme_cfc.txt             # Histogramme Tâche 2 (texte)
│   └── resultats_tache2.txt            # Résultats complets Tâche 2
├── lib/
│   ├── jfreechart-1.0.19.jar           # Bibliothèque graphiques
│   └── jcommon-1.0.23.jar              # Dépendance JFreeChart
├── dblp.dtd                            # Copie de la DTD à la racine
├── Projet_Algorithmique_2_2026.pdf     # Énoncé du projet
└── README.md                           # Ce fichier
```

---

## Données DBLP

Le fichier `dblp-2026-01-01.xml.gz` (~1 Go compressé) doit être placé dans `src/`.
**Ne pas le décompresser** : le parseur lit directement le format `.xml.gz`.

Téléchargement :
```
https://drops.dagstuhl.de/storage/artifacts/dblp/xml/2026/dblp-2026-01-01.xml.gz
```

---

## Prérequis

- Java JDK 17 ou plus récent
- Les JARs JFreeChart et JCommon sont déjà dans `lib/`

---

## Compilation

Depuis la **racine** du projet :

```bash
javac -cp "lib/jfreechart-1.0.19.jar;lib/jcommon-1.0.23.jar" -d src src/*.java
```

Sur Linux/macOS, remplacer `;` par `:` dans le classpath :

```bash
javac -cp "lib/jfreechart-1.0.19.jar:lib/jcommon-1.0.23.jar" -d src src/*.java
```

---

## Exécution

### Tâche 1 – Communautés non-orientées (Union-Find)

```bash
java -Xmx4g -cp "src;lib/jfreechart-1.0.19.jar;lib/jcommon-1.0.23.jar" Task1 src/dblp-2026-01-01.xml.gz src/dblp.dtd
```

Avec limite (utile pour tester) :

```bash
java -Xmx4g -cp "src;lib/jfreechart-1.0.19.jar;lib/jcommon-1.0.23.jar" Task1 src/dblp-2026-01-01.xml.gz src/dblp.dtd --limit=500000
```

**Sorties produites :**
- `output/histogram_task1.png` – histogramme des tailles de communautés (PNG)
- Affichage intermédiaire toutes les 100 000 publications : nombre de communautés + top 10 tailles

---

### Tâche 2 – Communautés fortement connexes (Kosaraju)

```bash
java -Xmx4g -cp "src;lib/jfreechart-1.0.19.jar;lib/jcommon-1.0.23.jar" Task2 src/dblp-2026-01-01.xml.gz src/dblp.dtd
```

Avec limite :

```bash
java -Xmx4g -cp "src;lib/jfreechart-1.0.19.jar;lib/jcommon-1.0.23.jar" Task2 src/dblp-2026-01-01.xml.gz src/dblp.dtd --limit=500000
```

**Sorties produites :**
- `output/histogram_task2.png` – histogramme des tailles des CFC (PNG)
- `output/histogramme_cfc.txt` – histogramme au format texte (taille → nombre de CFC)
- `output/resultats_tache2.txt` – résultats complets : top 10 CFC avec taille, diamètre et liste des auteur·rice·s

---

### Bonus – Répartition géographique des communautés (Python)

Ce script interroge l'API OpenAlex pour identifier le pays d'affiliation de chaque
auteur des 10 plus grandes CFC, puis génère un graphique à barres empilées.

**Prérequis Python :**
```bash
pip install requests matplotlib numpy
```

**Exécution** (depuis `src/`) :
```bash
python3 bonus.py
```

Le script lit automatiquement `resultats_tache2_complet.txt` généré par la Tâche 2.

**Sortie produite :**
- `output/proportion_auteurs_par_pays.png` – proportion d'auteurs par pays dans chaque communauté
