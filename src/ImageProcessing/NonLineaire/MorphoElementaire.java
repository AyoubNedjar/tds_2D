package ImageProcessing.NonLineaire;

/**
 * MORPHOLOGIE ELEMENTAIRE
 * =======================
 * Travaille sur les FORMES des objets dans une image.
 * Pas de fréquences, pas de lissage — on agit sur la forme.
 *
 * IMAGE BINAIRE vs NIVEAUX DE GRIS :
 * ------------------------------------
 * Image binaire  : pixels valent soit 0 (noir) soit 255 (blanc)
 * Niveaux de gris: pixels valent entre 0 et 255
 *
 * Les méthodes fonctionnent pour les DEUX grâce au min/max :
 *   - Érosion  → prend le MINIMUM des voisins
 *     (binaire : si un voisin = 0 → résultat = 0)
 *     (gris    : le pixel prend la valeur la plus sombre de ses voisins)
 *   - Dilatation → prend le MAXIMUM des voisins
 *     (binaire : si un voisin = 255 → résultat = 255)
 *     (gris    : le pixel prend la valeur la plus claire de ses voisins)
 *
 * ELEMENT STRUCTURANT :
 * ----------------------
 * C'est le "tampon" carré nxn qu'on fait glisser sur l'image.
 * Ici toujours un carré de 1 partout.
 * tailleMasque = 3 → carré 3x3 (9 voisins)
 * tailleMasque = 5 → carré 5x5 (25 voisins)
 * tailleMasque doit être impair (3, 5, 7...)
 */
public class MorphoElementaire {

    /**
     * EROSION
     * -------
     * Pour chaque pixel, on prend le MINIMUM de tous ses voisins
     * dans le carré nxn.
     *
     * Effet binaire  : un pixel blanc reste blanc seulement si
     *                  TOUS ses voisins sont blancs → les bords grignotés
     * Effet niveaux de gris : chaque pixel prend la valeur
     *                         la plus sombre de son voisinage
     *
     * Résultat visuel :
     *   - Les objets rétrécissent
     *   - Les petits points blancs isolés disparaissent
     *   - Les objets collés se séparent
     *
     * @param image       matrice de pixels (int[][], valeurs 0-255)
     * @param tailleMasque taille du carré nxn (impair : 3, 5, 7...)
     * @return            nouvelle matrice érodée
     */
    public static int[][] erosion(int[][] image, int tailleMasque) {
        int hauteur = image.length;
        int largeur = image[0].length;
        int rayon = tailleMasque / 2;
        int[][] resultat = new int[hauteur][largeur];

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {

                // on cherche le MINIMUM dans le voisinage nxn
                int min = 255;
                for (int k = -rayon; k <= rayon; k++) {
                    for (int l = -rayon; l <= rayon; l++) {
                        int ni = i + k;
                        int nj = j + l;
                        // si le voisin est hors image → on traite comme 0 (noir)
                        // ce qui force le pixel de bord à devenir 0
                        if (ni < 0 || ni >= hauteur || nj < 0 || nj >= largeur) {
                            min = 0;
                        } else {
                            min = Math.min(min, image[ni][nj]);
                        }
                    }
                }
                resultat[i][j] = min;
            }
        }
        return resultat;
    }

    /**
     * DILATATION
     * ----------
     * Pour chaque pixel, on prend le MAXIMUM de tous ses voisins
     * dans le carré nxn.
     *
     * Effet binaire  : un pixel noir devient blanc si AU MOINS UN
     *                  voisin est blanc → les bords gonflent
     * Effet niveaux de gris : chaque pixel prend la valeur
     *                         la plus claire de son voisinage
     *
     * Résultat visuel :
     *   - Les objets grossissent
     *   - Les petits trous dans les objets se bouchent
     *   - Les objets séparés peuvent se rejoindre
     *
     * @param image        matrice de pixels (int[][], valeurs 0-255)
     * @param tailleMasque taille du carré nxn (impair : 3, 5, 7...)
     * @return             nouvelle matrice dilatée
     */
    public static int[][] dilatation(int[][] image, int tailleMasque) {
        int hauteur = image.length;
        int largeur = image[0].length;
        int rayon = tailleMasque / 2;
        int[][] resultat = new int[hauteur][largeur];

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {

                // on cherche le MAXIMUM dans le voisinage nxn
                int max = 0;
                for (int k = -rayon; k <= rayon; k++) {
                    for (int l = -rayon; l <= rayon; l++) {
                        int ni = i + k;
                        int nj = j + l;
                        // si le voisin est hors image → on ignore
                        // (on ne dilate pas au delà des bords)
                        if (ni >= 0 && ni < hauteur && nj >= 0 && nj < largeur) {
                            max = Math.max(max, image[ni][nj]);
                        }
                    }
                }
                resultat[i][j] = max;
            }
        }
        return resultat;
    }

    /**
     * OUVERTURE
     * ---------
     * = érosion PUIS dilatation
     *
     * Résultat visuel :
     *   - Supprime les petits objets parasites (bruit de forme)
     *   - Les grands objets gardent presque leur taille
     *   - Sépare les objets qui se touchent légèrement
     *
     * @param image        matrice de pixels
     * @param tailleMasque taille du carré nxn
     * @return             nouvelle matrice après ouverture
     */
    public static int[][] ouverture(int[][] image, int tailleMasque) {
        // étape 1 : érosion → les petits objets disparaissent
        int[][] imageErodee = erosion(image, tailleMasque);
        // étape 2 : dilatation → les grands objets retrouvent leur taille
        return dilatation(imageErodee, tailleMasque);
    }

    /**
     * FERMETURE
     * ---------
     * = dilatation PUIS érosion
     *
     * Résultat visuel :
     *   - Bouche les petits trous dans les objets
     *   - Comble les petites interruptions dans les contours
     *   - Les objets gardent presque leur taille
     *
     * @param image        matrice de pixels
     * @param tailleMasque taille du carré nxn
     * @return             nouvelle matrice après fermeture
     */
    public static int[][] fermeture(int[][] image, int tailleMasque) {
        // étape 1 : dilatation → les trous se bouchent
        int[][] imageDilatee = dilatation(image, tailleMasque);
        // étape 2 : érosion → l'objet retrouve sa taille d'origine
        return erosion(imageDilatee, tailleMasque);
    }
}