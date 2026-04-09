
# Projet Algorithmique 2 – Détection de communautés dans DBLP


Ce projet s’inscrit dans le cadre du cours INFO-F-203 – Algorithmique 2 (ULB).
L’objectif est de détecter des communautés de chercheurs à partir de la base de données DBLP,
selon différentes définitions de communautés, en respectant une contrainte de traitement en flux.


## Structure du projet

- `src/` : contient le code source Java
  - `DblpParsingDemo.java` : programme principal, lecture du flux DBLP et traitement en ligne
  - `DblpPublicationGenerator.java` : parseur DBLP fourni par l’enseignant
  - `UnionFind.java` : structure Union-Find utilisée pour maintenir les communautés
- `dblp.dtd` : DTD officielle associée au fichier XML DBLP
- `dblp-2026-01-01.xml.gz` : snapshot DBLP du 1er janvier 2026 (à télécharger séparément)
- `README.md` : instructions de compilation et d’exécution
- `Projet_Algorithmique_2_2026.pdf` : énoncé du projet

---

## Prérequis

- Java JDK 17 ou plus récent
- Le fichier `dblp-2026-01-01.xml.gz` doit être téléchargé depuis :
  https://drops.dagstuhl.de/storage/artifacts/dblp/xml/2026/dblp-2026-01-01.xml.gz
- Le fichier DBLP ne doit pas être décompressé.

---

## Compilation

Depuis la racine du projet :

```bash
javac src/*.java

