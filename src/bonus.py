import requests
from collections import Counter
import time
import matplotlib.pyplot as plt
import numpy as np
import re

def parse_communities(filename):
    communities = []
    with open(filename, 'r', encoding='utf-8') as file:
        for line in file:
            if "Membres:" in line:
                partie = line.split("Membres: [")[1]
                partie = partie.strip().rstrip("]") 
                authors = [a.strip() for a in partie.split(",")]
                communities.append(authors)
    return communities

def get_country(author):
    author_clean = re.sub(r'\s+\d+$', '', author).strip()
    url = "https://api.openalex.org/authors?search=" + author_clean + "&per_page=1&mailto=islam.mekhiouba@ulb.be"
    try:
        headers = {
            "User-Agent": "mailto:islam.mekhiouba@ulb.be"
        }
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            data = response.json()
            print(data)
            results = data.get("results", [])
            if results:
                institution = results[0].get("last_known_institution")
                if institution:
                    return institution.get("country_code", "Inconnu")
            return 'Inconnu'
        else:
            print(f"Erreur pour {author}: {response.status_code}")
            return 'Inconnu'
    except Exception as e:
        print("Exception pour  " + author + " : " + str(e))
        return 'Inconnu'
    
    

def get_countries_per_community(communities):
    results = []
    for i, community in enumerate(communities):
        print("Traitement communauté " + str(i+1) + "/" + str(len(communities)) + "...")
        counter = Counter()
        for author in community:
            country = get_country(author)
            counter[country] += 1
            # """D'après Claude, il faut faire une pause de 0.5s entre les requêtes
            #         pour éviter de surcharger l'API"""
            time.sleep(3)
        results.append(counter)
    return results

def plot_countries(countries_per_community):
    # 1. Collecter tous les pays uniques
    all_countries = set()
    for counter in countries_per_community:
        all_countries.update(counter.keys())
    all_countries = sorted(all_countries)
    
    # 2. Pour chaque communauté, calculer la proportion par pays
    # (nombre d'auteurs de ce pays / total auteurs de la communauté)
    propotions = []
    for counter in countries_per_community:
        total = sum(counter.values())
        proportions = [counter[country] / total for country in all_countries]
        propotions.append(proportions)
    # 3. Tracer les barres empilées
    ind = np.arange(len(countries_per_community))  # position des communautés sur l'axe x
    bottom = np.zeros(len(countries_per_community))  # pour empiler les barres
    plt.figure(figsize=(12, 6))
    # 4. Sauvegarder en PNG
    for i, country in enumerate(all_countries):
        values = [prop[i] for prop in propotions]
        plt.bar(ind, values, bottom=bottom, label=country)
        bottom += values
    plt.xlabel('Communautés')
    plt.ylabel('Proportion d\'auteurs')
    plt.title('Proportion d\'auteurs par pays dans chaque communauté')
    plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()
    plt.savefig('../output/proportion_auteurs_par_pays.png')

if __name__ == "__main__":
    print(get_country("Dacheng Tao"))
    # communities = parse_communities("../output/resultats_tache2_complet.txt")
    # print(f"{len(communities)} communautés trouvées")
    # countries = get_countries_per_community(communities)
    # plot_countries(countries)
    # print("Graphique sauvegardé sur le dossier output")