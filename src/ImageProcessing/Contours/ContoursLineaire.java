package ImageProcessing.Contours;

import ImageProcessing.Lineaire.FiltrageLineaireLocal;

public class ContoursLineaire {

    /**
     * GRADIENT DE PREWITT
     * -------------------
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @param dir   1 = horizontal (contours verticaux)
     *              2 = vertical (contours horizontaux)
     * @return      nouvelle matrice avec les contours détectés
     */
    public static int[][] gradientPrewitt(int[][] image, int dir) {
        double[][] masque;
        if (dir == 2) {
            masque = new double[][] {
                    {-1, 0, 1},
                    {-1, 0, 1},
                    {-1, 0, 1}
            };
        } else {
            masque = new double[][] {
                    {-1, -1, -1},
                    { 0,  0,  0},
                    { 1,  1,  1}
            };
        }
        int[][] brut = FiltrageLineaireLocal.filtreMasqueConvolutionBrut(image, masque);
        int M = brut.length, N = brut[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, brut[i][j] + 128);
        return resultat;
    }




//--------------------------------------------------------------------------------------------------------



    /**
     * GRADIENT DE SOBEL
     *
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @param dir   1 = horizontal (contours verticaux)
     *              2 = vertical (contours horizontaux)
     * @return      nouvelle matrice avec les contours détectés
     */
    public static int[][] gradientSobel(int[][] image, int dir) {
        double[][] masque;
        if (dir == 2) {
            masque = new double[][] {
                    {-1, 0, 1},
                    {-2, 0, 2},
                    {-1, 0, 1}
            };
        } else {
            masque = new double[][] {
                    {-1, -2, -1},
                    { 0,  0,  0},
                    { 1,  2,  1}
            };
        }
        int[][] brut = FiltrageLineaireLocal.filtreMasqueConvolutionBrut(image, masque);
        int M = brut.length, N = brut[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, Math.max(0, brut[i][j] + 128));
        return resultat;
    }





    //----------------------------------------------------------------------------------------------------------------
    /**
     * LAPLACIEN 4
     * -----------
     * Calcule la dérivée seconde de l'image en regardant 4 voisins.
     * C'est la version la plus simple du Laplacien.
     *
     * PRINCIPE :
     * Combine la dérivée seconde horizontale ET verticale en une seule opération.
     * Pas besoin de faire deux passes séparées comme Prewitt/Sobel.
     *
     * Formule 1.52 :
     * résultat = haut + bas + gauche + droite - 4×centre
     *
     * Masque :
     *  0  +1   0
     * +1  -4  +1
     *  0  +1   0
     *
     * Zone uniforme → résultat ≈ 0 (pas de contour)
     * Zone de contour → résultat élevé (passage par zéro = contour précis)
     *
     * DIFFÉRENCE AVEC LAPLACIEN 8 :
     * Laplacien4 ignore les diagonales → moins précis mais moins de bruit.
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @return      nouvelle matrice avec les contours détectés
     */
    public static int[][] laplacien4(int[][] image) {
        double[][] masque = {{0,1,0},{1,-4,1},{0,1,0}};
        int[][] brut = FiltrageLineaireLocal.filtreMasqueConvolutionBrut(image, masque);
        int M = image.length, N = image[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, Math.max(0, brut[i][j] + 128));
        return resultat;
    }


    /**
     * LAPLACIEN 8
     * -----------
     * Calcule la dérivée seconde de l'image en regardant 8 voisins.
     * Version plus complète du Laplacien — inclut les diagonales.
     *
     * PRINCIPE :
     * Même chose que Laplacien4 mais avec les 4 voisins diagonaux en plus.
     * Le centre vaut -8 car il y a maintenant 8 voisins.
     *
     * Formule 1.53 :
     * résultat = tous les 8 voisins - 8×centre
     *
     * Masque :
     * +1  +1  +1
     * +1  -8  +1
     * +1  +1  +1
     *
     * DIFFÉRENCE AVEC LAPLACIEN 4 :
     * Laplacien8 prend en compte les diagonales → détecte plus de contours
     * mais encore plus sensible au bruit que Laplacien4.
     *
     * @param image matrice de pixels en niveaux de gris (int[][], valeurs 0-255)
     * @return      nouvelle matrice avec les contours détectés
     */
    public static int[][] laplacien8(int[][] image) {
        double[][] masque = {
                { 1,  1,  1},
                { 1, -8,  1},
                { 1,  1,  1}
        };
        int[][] brut = FiltrageLineaireLocal.filtreMasqueConvolutionBrut(image, masque);
        int M = brut.length, N = brut[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = Math.min(255, Math.max(0, brut[i][j] + 128));
        return resultat;
    }
}
