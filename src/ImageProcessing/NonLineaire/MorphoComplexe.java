package ImageProcessing.NonLineaire;

public class MorphoComplexe {

    /**
     * DILATATION GEODESIQUE
     * ---------------------
     * On dilate l'image normalement, puis on applique min(résultat, masque).
     * On répète ça nbIter fois.
     *
     * À chaque itération :
     *   1. On dilate l'image courante (max des voisins 3x3)
     *   2. résultat[i][j] = min(dilatation[i][j], masque[i][j])
     *
     * Le masque limite jusqu'où la dilatation peut aller.
     * Pour une image binaire : masque vaut 0 ou 255
     * Pour une image en niveaux de gris : masque vaut entre 0 et 255
     *
     * @param image             image de départ (int[][], valeurs 0-255)
     * @param masqueGeodesique  zone maximale autorisée pour la dilatation
     * @param nbIter            nombre d'itérations (>= 1)
     * @return                  image après dilatation géodésique
     */
    public static int[][] dilatationGeodesique(int[][] image, int[][] masqueGeodesique, int nbIter) {
        int hauteur = image.length;
        int largeur = image[0].length;

        // on part de l'image originale
        int[][] courant = new int[hauteur][largeur];
        for (int i = 0; i < hauteur; i++)
            for (int j = 0; j < largeur; j++)
                courant[i][j] = image[i][j];

        // on répète nbIter fois
        for (int iter = 0; iter < nbIter; iter++) {

            int[][] resultat = new int[hauteur][largeur];

            // ÉTAPE 1 : dilatation normale (max des voisins 3x3)
            for (int i = 0; i < hauteur; i++) {
                for (int j = 0; j < largeur; j++) {

                    int max = 0;
                    // on parcourt les 9 voisins du pixel (i,j)
                    for (int k = -1; k <= 1; k++) {
                        for (int l = -1; l <= 1; l++) {
                            int ni = i + k;
                            int nj = j + l;
                            // si le voisin existe dans l'image
                            if (ni >= 0 && ni < hauteur && nj >= 0 && nj < largeur) {
                                max = Math.max(max, courant[ni][nj]);
                            }
                        }
                    }

                    // ÉTAPE 2 : on applique le masque géodésique
                    // le pixel ne peut pas dépasser la valeur du masque
                    // min(dilatation, masque) :
                    // - si masque = 0   → pixel bloqué à 0
                    // - si masque = 255 → pixel peut passer (255 ou moins)
                    resultat[i][j] = Math.min(max, masqueGeodesique[i][j]);
                }
            }

            // le résultat devient l'image courante pour la prochaine itération
            courant = resultat;
        }

        return courant;
    }

    /**
     * RECONSTRUCTION GEODESIQUE
     * -------------------------
     * C'est la dilatation géodésique répétée jusqu'à stabilité.
     * On continue à dilater jusqu'à ce que plus rien ne change
     * entre deux itérations consécutives.
     *
     * Utilisation typique :
     *   - image    = version érodée de l'image originale
     *   - masque   = image originale
     *   → on reconstruit les objets sans le bruit
     *
     * @param image             image de départ (souvent une version érodée)
     * @param masqueGeodesique  image originale (limite la reconstruction)
     * @return                  image reconstruite
     */
    public static int[][] reconstructionGeodesique(int[][] image, int[][] masqueGeodesique) {
        int hauteur = image.length;
        int largeur = image[0].length;

        // on part de l'image originale
        int[][] courant = new int[hauteur][largeur];
        for (int i = 0; i < hauteur; i++)
            for (int j = 0; j < largeur; j++)
                courant[i][j] = image[i][j];

        // on répète jusqu'à stabilité
        while (true) {

            // une itération de dilatation géodésique
            int[][] resultat = new int[hauteur][largeur];

            for (int i = 0; i < hauteur; i++) {
                for (int j = 0; j < largeur; j++) {

                    // ÉTAPE 1 : max des voisins 3x3
                    int max = 0;
                    for (int k = -1; k <= 1; k++) {
                        for (int l = -1; l <= 1; l++) {
                            int ni = i + k;
                            int nj = j + l;
                            if (ni >= 0 && ni < hauteur && nj >= 0 && nj < largeur) {
                                max = Math.max(max, courant[ni][nj]);
                            }
                        }
                    }

                    // ÉTAPE 2 : min avec le masque
                    resultat[i][j] = Math.min(max, masqueGeodesique[i][j]);
                }
            }

            // on vérifie si quelque chose a changé
            // si rien n'a changé → on s'arrête
            boolean stable = true;
            for (int i = 0; i < hauteur && stable; i++)
                for (int j = 0; j < largeur && stable; j++)
                    if (resultat[i][j] != courant[i][j])
                        stable = false;

            courant = resultat;

            // rien n'a changé → reconstruction terminée
            if (stable) break;
        }

        return courant;
    }

    /**
     * FILTRE MEDIAN
     * -------------
     * Pour chaque pixel, on prend la valeur MÉDIANE de ses voisins nxn.
     * La médiane = valeur du milieu après tri de tous les voisins.
     *
     * Différence avec le filtre moyenneur :
     *   - Moyenneur : fait la MOYENNE → sensible aux valeurs extrêmes
     *   - Médian    : prend la MEDIANE → insensible au bruit sel et poivre
     *
     * Exemple avec voisins : 200 198 10 255 199 201 198 200 197
     *   Triés   : 10 197 198 198 199 200 200 201 255
     *   Médiane : 199  ← valeur du milieu
     *   Le 10 et le 255 (bruit) n'influencent pas le résultat !
     *
     * Ne s'applique qu'aux images en niveaux de gris.
     *
     * @param image        image en niveaux de gris (int[][], valeurs 0-255)
     * @param tailleMasque taille du carré nxn (impair : 3, 5, 7...)
     * @return             image filtrée
     */
    public static int[][] filtreMedian(int[][] image, int tailleMasque) {
        int hauteur = image.length;
        int largeur = image[0].length;
        int rayon = tailleMasque / 2;
        int[][] resultat = new int[hauteur][largeur];

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {

                // on collecte tous les voisins dans le carré nxn
                int[] voisins = new int[tailleMasque * tailleMasque];
                int count = 0;

                for (int k = -rayon; k <= rayon; k++) {
                    for (int l = -rayon; l <= rayon; l++) {
                        int ni = i + k;
                        int nj = j + l;
                        if (ni >= 0 && ni < hauteur && nj >= 0 && nj < largeur) {
                            voisins[count++] = image[ni][nj];
                        }
                    }
                }

                // on trie les voisins collectés
                // tri à bulles simple sur les count premiers éléments
                for (int a = 0; a < count - 1; a++) {
                    for (int b = 0; b < count - 1 - a; b++) {
                        if (voisins[b] > voisins[b + 1]) {
                            int temp = voisins[b];
                            voisins[b] = voisins[b + 1];
                            voisins[b + 1] = temp;
                        }
                    }
                }

                // on prend la valeur du milieu = médiane
                resultat[i][j] = voisins[count / 2];
            }
        }

        return resultat;
    }
}