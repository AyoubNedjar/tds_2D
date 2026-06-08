package ImageProcessing.Lineaire;

import ImageProcessing.Complexe.Complexe;
import ImageProcessing.Complexe.MatriceComplexe;
import ImageProcessing.Fourier.Fourier;

/**
 * FILTRAGE LINEAIRE GLOBAL
 * ========================
 * Toutes les méthodes suivent exactement ces 5 étapes :
 *
 * 1. Convertir int[][] en double[][] (Fourier2D attend du double)
 * 2. Fourier2D(image)     → tableau de nombres complexes
 *                           (basses fréquences dans les 4 coins)
 * 3. decroise(F)          → basses fréquences au centre
 * 4. Appliquer le filtre  → multiplier chaque case par H (0 ou entre 0 et 1)
 *                           C'EST LA SEULE CHOSE QUI CHANGE entre les 4 méthodes
 * 5. decroise(Ffiltre)    → remettre les basses fréquences dans les coins
 * 6. InverseFourier2D(F)  → retour en nombres complexes
 * 7. Prendre partie réelle + clamp 0-255 → int[][]
 *
 * LE FILTRE H :
 * ------------
 * Pour chaque case (i,j) du tableau centré, on calcule la distance au centre :
 *   d = √( (i - centreY)² + (j - centreX)² )
 *
 * Passe-bas idéal  : H = 1 si d <= fc, sinon H = 0
 * Passe-haut idéal : H = 0 si d <= fc, sinon H = 1
 * Butterworth bas  : H = 1 / (1 + (d/fc)^(2*ordre))
 * Butterworth haut : H = 1 / (1 + (fc/d)^(2*ordre))
 */
public class FiltrageLineaireGlobal {

    // =========================================================
    // MÉTHODE UTILITAIRE — convertit int[][] en double[][]
    // Fourier2D attend un double[][] donc on convertit d'abord
    // =========================================================
    private static double[][] versDouble(int[][] image) {
        int M = image.length;
        int N = image[0].length;
        double[][] result = new double[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                result[i][j] = (double) image[i][j];
        return result;
    }

    // =========================================================
    // MÉTHODE UTILITAIRE — applique le filtre H sur le tableau
    // centré et retourne l'image filtrée en int[][]
    // Reçoit la MatriceComplexe déjà centrée + le tableau de H
    // =========================================================
    private static int[][] appliquerFiltre(MatriceComplexe Fcentree, double[][] H) {
        int M = Fcentree.getLignes();
        int N = Fcentree.getColonnes();

        // --- ÉTAPE 4 : multiplier chaque case par son H ---
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                Complexe c = Fcentree.get(i, j);
                // H=1 → case inchangée
                // H=0 → case devient (0+0i) → fréquence effacée
                // H entre 0 et 1 → case atténuée (Butterworth)
                Fcentree.set(i, j,
                        c.getPartieReelle()     * H[i][j],
                        c.getPartieImaginaire() * H[i][j]
                );
            }
        }

        // --- ÉTAPE 5 : decroise pour remettre en place ---
        // On remet les basses fréquences dans les coins
        // pour que InverseFourier2D fonctionne correctement
        MatriceComplexe Fdecentree = Fourier.decroise(Fcentree);

        // --- ÉTAPE 6 : Fourier inverse → retour en spatial ---
        MatriceComplexe resultat = Fourier.InverseFourier2D(Fdecentree);

        // --- ÉTAPE 7 : partie réelle + clamp 0-255 → int[][] ---
        int[][] imageResultat = new int[M][N];
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                // on prend la partie réelle (la partie imaginaire
                // est quasi nulle à cause des erreurs d'arrondi)
                double valeur = resultat.get(i, j).getPartieReelle();
                // clamp : on force entre 0 et 255
                imageResultat[i][j] = Math.min(255, Math.max(0, (int) Math.round(valeur)));
            }
        }
        return imageResultat;
    }

    // =========================================================
    // MÉTHODE 1 — filtrePasseBasIdeal
    // Garde les basses fréquences (proches du centre)
    // Bloque les hautes fréquences (loin du centre)
    // Coupure BRUTALE : H = 1 ou H = 0
    // =========================================================
    public static int[][] filtrePasseBasIdeal(int[][] image, int frequenceCoupure) {
        int M = image.length;
        int N = image[0].length;

        // ÉTAPE 1 : int[][] → double[][]
        double[][] imageDouble = versDouble(image);

        // ÉTAPE 2 : Fourier → tableau de fréquences
        MatriceComplexe F = Fourier.Fourier2D(imageDouble);

        // ÉTAPE 3 : decroise → basses fréquences au centre
        MatriceComplexe Fcentree = Fourier.decroise(F);

        // ÉTAPE 4 : calcul du filtre H pour chaque case
        int centreY = M / 2;
        int centreX = N / 2;
        double[][] H = new double[M][N];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                // distance de cette case au centre du tableau
                double d = Math.sqrt(
                        Math.pow(i - centreY, 2) +
                                Math.pow(j - centreX, 2)
                );
                // si dans le cercle → H=1 (garde)
                // si hors du cercle → H=0 (bloque)
                H[i][j] = (d <= frequenceCoupure) ? 1.0 : 0.0;
            }
        }

        // ÉTAPES 5-6-7 : appliquer H + decroise + inverse + clamp
        return appliquerFiltre(Fcentree, H);
    }

    // =========================================================
    // MÉTHODE 2 — filtrePasseHautIdeal
    // Garde les hautes fréquences (loin du centre)
    // Bloque les basses fréquences (proches du centre)
    // Coupure BRUTALE : H = 0 ou H = 1
    // =========================================================
    public static int[][] filtrePasseHautIdeal(int[][] image, int frequenceCoupure) {
        int M = image.length;
        int N = image[0].length;

        double[][] imageDouble = versDouble(image);
        MatriceComplexe F = Fourier.Fourier2D(imageDouble);
        MatriceComplexe Fcentree = Fourier.decroise(F);

        int centreY = M / 2;
        int centreX = N / 2;
        double[][] H = new double[M][N];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                double d = Math.sqrt(
                        Math.pow(i - centreY, 2) +
                                Math.pow(j - centreX, 2)
                );
                // INVERSE du passe-bas :
                // si dans le cercle → H=0 (bloque les basses)
                // si hors du cercle → H=1 (garde les hautes)
                H[i][j] = (d <= frequenceCoupure) ? 0.0 : 1.0;
            }
        }

        return appliquerFiltre(Fcentree, H);
    }

    // =========================================================
    // MÉTHODE 3 — filtrePasseBasButterworth
    // Même effet que passe-bas idéal mais coupure PROGRESSIVE
    // Formule : H = 1 / (1 + (d/fc)^(2*ordre))
    // Plus l'ordre est grand → plus la pente est raide
    // =========================================================
    public static int[][] filtrePasseBasButterworth(int[][] image, int frequenceCoupure, int ordre) {
        int M = image.length;
        int N = image[0].length;

        double[][] imageDouble = versDouble(image);
        MatriceComplexe F = Fourier.Fourier2D(imageDouble);
        MatriceComplexe Fcentree = Fourier.decroise(F);

        int centreY = M / 2;
        int centreX = N / 2;
        double[][] H = new double[M][N];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                double d = Math.sqrt(
                        Math.pow(i - centreY, 2) +
                                Math.pow(j - centreX, 2)
                );
                // si d=0 (centre exact) → H=1
                // si d=fc → H=0.5 (passe à moitié)
                // si d très grand → H≈0 (presque bloqué)
                if (d == 0) {
                    H[i][j] = 1.0;
                } else {
                    H[i][j] = 1.0 / (1.0 + Math.pow(d / frequenceCoupure, 2.0 * ordre));
                }
            }
        }

        return appliquerFiltre(Fcentree, H);
    }

    // =========================================================
    // MÉTHODE 4 — filtrePasseHautButterworth
    // Même effet que passe-haut idéal mais coupure PROGRESSIVE
    // Formule : H = 1 / (1 + (fc/d)^(2*ordre))
    // =========================================================
    public static int[][] filtrePasseHautButterworth(int[][] image, int frequenceCoupure, int ordre) {
        int M = image.length;
        int N = image[0].length;

        double[][] imageDouble = versDouble(image);
        MatriceComplexe F = Fourier.Fourier2D(imageDouble);
        MatriceComplexe Fcentree = Fourier.decroise(F);

        int centreY = M / 2;
        int centreX = N / 2;
        double[][] H = new double[M][N];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                double d = Math.sqrt(
                        Math.pow(i - centreY, 2) +
                                Math.pow(j - centreX, 2)
                );
                // si d=0 (centre exact) → H=0 (basse fréquence bloquée)
                // si d=fc → H=0.5
                // si d très grand → H≈1 (haute fréquence garde)
                if (d == 0) {
                    H[i][j] = 0.0;
                } else {
                    H[i][j] = 1.0 / (1.0 + Math.pow((double) frequenceCoupure / d, 2.0 * ordre));
                }
            }
        }

        return appliquerFiltre(Fcentree, H);
    }
}