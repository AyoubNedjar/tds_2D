package ImageProcessing.Contours;

import ImageProcessing.NonLineaire.MorphoElementaire;

public class ContoursNonLineaire {

    /**
     * GRADIENT D'ÉROSION
     * ------------------
     * Détecte les contours INTÉRIEURS de l'objet.
     *
     * Formule 1.54 :
     * GE(f) = f - (f ⊖ B)
     *
     * On soustrait l'image érodée (rétrécie) de l'image originale.
     * Ce qui reste = les pixels perdus par l'érosion = bord intérieur.
     *
     * Résultat : contour fin à l'INTÉRIEUR de l'objet.
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @return      image avec les contours intérieurs
     */
    public static int[][] gradientErosion(int[][] image, int taille) {
        int[][] erodee = MorphoElementaire.erosion(image, taille);
        int M = image.length, N = image[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, Math.max(0,
                        image[i][j] - erodee[i][j]));
        return resultat;
    }

    /**
     * GRADIENT DE DILATATION
     * ----------------------
     * Détecte les contours EXTÉRIEURS de l'objet.
     *
     * Formule 1.55 :
     * GD(f) = (f ⊕ B) - f
     *
     * On soustrait l'image originale de l'image dilatée (agrandie).
     * Ce qui reste = les pixels gagnés par la dilatation = bord extérieur.
     *
     * Résultat : contour fin à l'EXTÉRIEUR de l'objet.
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @return      image avec les contours extérieurs
     */
    public static int[][] gradientDilatation(int[][] image, int taille) {
        int[][] dilatee = MorphoElementaire.dilatation(image, taille);
        int M = image.length, N = image[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, Math.max(0,
                        dilatee[i][j] - image[i][j]));
        return resultat;
    }

    /**
     * GRADIENT DE BEUCHER
     * -------------------
     * Détecte les contours INTÉRIEURS et EXTÉRIEURS simultanément.
     * C'est la dérivée première morphologique — contour épais symétrique.
     *
     * Formule 1.56 :
     * GB(f) = (f ⊕ B) - (f ⊖ B)
     *       = GD(f) + GE(f)
     *
     * Équivalent morphologique du gradient de Prewitt/Sobel.
     * Plus large que les deux gradients séparés.
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @return      image avec les contours symétriques
     */
    public static int[][] gradientBeucher(int[][] image, int taille) {
        int[][] dilatee = MorphoElementaire.dilatation(image, taille);
        int[][] erodee  = MorphoElementaire.erosion(image, taille);
        int M = image.length, N = image[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, Math.max(0,
                        dilatee[i][j] - erodee[i][j]));
        return resultat;
    }

    /**
     * LAPLACIEN NON-LINÉAIRE
     * ----------------------
     * Dérivée seconde morphologique — contour fin et précis.
     * Équivalent morphologique du Laplacien linéaire.
     *
     * Formule 1.60 :
     * LNL(f) = GD(f) - GE(f)
     *        = (dilatation - image) - (image - érosion)
     *        = dilatation - 2×image + érosion
     *
     * Le résultat peut être négatif → offset +128 pour centrer sur le gris.
     * Positif d'un côté du contour, négatif de l'autre
     * → passage par zéro exactement sur le bord = plus précis que Beucher.
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @return      image avec les contours précis (centrés sur 128)
     */
    public static int[][] laplacienNonLineaire(int[][] image, int taille) {
        int[][] gd = gradientDilatation(image, taille);
        int[][] ge = gradientErosion(image, taille);
        int M = image.length, N = image[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, Math.max(0,
                        gd[i][j] - ge[i][j] + 128));
        return resultat;
    }
}