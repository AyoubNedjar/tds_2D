package ImageProcessing.Histogramme;

public class Histogramme 
{
    public static int[] Histogramme256(int mat[][])
    {
        int M = mat.length;
        int N = mat[0].length;
        int histo[] = new int[256];
        
        for(int i=0 ; i<256 ; i++) histo[i] = 0;
        
        for(int i=0 ; i<M ; i++)
            for(int j=0 ; j<N ; j++)
                if ((mat[i][j] >= 0) && (mat[i][j]<=255)) histo[mat[i][j]]++;
        
        return histo;
    }

    /**
     * elle retourne le plus petit niveau de gris existant
     * @param tab
     * @return
     */
    public static int minimum(int[][] image) {
        int[] histo = Histogramme256(image);

        for (int i = 0; i < 256; i++) {
            if (histo[i] > 0) {
                return i;  // premier indice avec au moins 1 pixel
            }
        }
        return 0;
    }

    /**
     * retourne le niveau de gris maximum existant de l'image
     * @param image
     * @return
     */
    public static int maximum(int[][] image) {
        int[] histo = Histogramme256(image);

        for (int i = 255; i >= 0; i--) {
            if (histo[i] > 0) {
                return i;  // dernier indice avec au moins 1 pixel
            }
        }
        return 255;
    }

    /**
     * a luminance c'est simplement la valeur moyenne de l'image — la luminosité globa ( café au lait , si on ajoute bcp de lait ca devient blanc)
     * @param image
     * @return
     */
    public static  int luminance(int[][] image) {
        int[] histo = Histogramme256(image);
        int M = image.length;
        int N = image[0].length;

        int somme = 0;
        for (int i = 0; i < 256; i++) {
            somme += i * histo[i];
        }
        return somme / (M * N);
    }

    /**
     *  L'écart-type du contraste sert à mesurer à quel point l'image est riche en détails visuels.
     * @param image
     * @return
     */
    public static double contraste1(int[][] image) {
        int[] histo = Histogramme256(image);
        int M = image.length;
        int N = image[0].length;
        int lum = luminance(image);

        double somme = 0;
        for (int i = 0; i < 256; i++) {
            somme += Math.pow(i - lum, 2) * histo[i];
        }
        return Math.sqrt(somme / (M * N));
    }

    public static double contraste2(int[][] image) {
        int min = minimum(image);
        int max = maximum(image);

        if (min + max == 0) return 0; // éviter division par zéro

        return (double)(max - min) / (double)(max + min);
    }







    public static int[][] rehaussement(int[][] image, int[] courbeTonale) {
        int M = image.length;
        int N = image[0].length;
        int[][] resultat = new int[M][N];

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                // pixel de valeur image[i][j]
                // est remplacé par courbeTonale[image[i][j]]
                resultat[i][j] = courbeTonale[image[i][j]];
            }
        }
        return resultat;
    }

    /**
     * COURBE TONALE LINEAIRE AVEC SATURATION
     * ----------------------------------------
     * Étire les valeurs entre smin et smax pour occuper toute la plage 0-255.
     * Les valeurs en dehors de [smin, smax] sont saturées (0 ou 255).
     *
     * Formule 1.32 :
     *   si i <= smin → 0
     *   si i >= smax → 255
     *   sinon        → (i - smin) × 255 / (smax - smin)
     *
     * UTILISATION :
     *   Avec saturation    → choisir smin et smax manuellement
     *   Sans saturation    → passer minimum(image) et maximum(image)
     *
     * Exemple smin=50, smax=180 :
     *   courbeTonale[50]  = 0
     *   courbeTonale[115] = 127
     *   courbeTonale[180] = 255
     *
     * @param smin valeur de gris minimale (sera saturée à 0)
     * @param smax valeur de gris maximale (sera saturée à 255)
     * @return     courbe tonale de 256 valeurs
     */
    public static int[] creeCourbeTonaleLineaireSaturation(int smin, int smax) {
        int[] courbe = new int[256];

        for (int i = 0; i < 256; i++) {
            if (i <= smin) {
                // en dessous de smin → saturé à 0
                courbe[i] = 0;
            } else if (i >= smax) {
                // au dessus de smax → saturé à 255
                courbe[i] = 255;
            } else {
                // entre smin et smax → étirement linéaire
                courbe[i] = (int) Math.round(
                        (double)(i - smin) * 255.0 / (double)(smax - smin)
                );
            }
        }
        return courbe;
    }

    /**
     * COURBE TONALE GAMMA
     * --------------------
     * Éclaircit ou assombrit l'image selon la valeur de gamma.
     *
     * Formule 1.33 :
     *   courbeTonale[i] = 255 × (i/255)^(1/gamma)
     *
     * gamma > 1 → éclaircit l'image (les valeurs moyennes remontent)
     * gamma < 1 → assombrit l'image (les valeurs moyennes descendent)
     * gamma = 1 → aucun changement
     *
     * Exemple gamma=2.0 :
     *   courbeTonale[0]   = 0
     *   courbeTonale[50]  = 113   ← 50 monte à 113
     *   courbeTonale[128] = 180   ← 128 monte à 180
     *   courbeTonale[255] = 255
     *
     * @param gamma facteur gamma (entre 1/3 et 3 selon le syllabus)
     * @return      courbe tonale de 256 valeurs
     */
    public static int[] creeCourbeTonaleGamma(double gamma) {
        int[] courbe = new int[256];

        for (int i = 0; i < 256; i++) {
            courbe[i] = (int) Math.round(
                    255.0 * Math.pow(i / 255.0, 1.0 / gamma)
            );
        }
        return courbe;
    }

    /**
     * COURBE TONALE NEGATIF
     * ----------------------
     * Inverse toutes les valeurs — noir devient blanc, blanc devient noir.
     *
     * Formule 1.34 :
     *   courbeTonale[i] = 255 - i
     *
     * courbeTonale[0]   = 255   ← noir → blanc
     * courbeTonale[128] = 127   ← gris reste gris
     * courbeTonale[255] = 0     ← blanc → noir
     *
     * Pas besoin de l'image en paramètre car la formule est universelle —
     * indépendante du contenu de l'image.
     *
     * @return courbe tonale de 256 valeurs
     */
    public static int[] creeCourbeTonaleNegatif() {
        int[] courbe = new int[256];

        for (int i = 0; i < 256; i++) {
            courbe[i] = 255 - i;
        }
        return courbe;
    }

    /**
     * COURBE TONALE EGALISATION
     * --------------------------
     * Redistribue les valeurs pour que toutes les nuances de gris
     * soient utilisées également sur toute la plage 0-255.
     *
     * 4 étapes de l'algorithme :
     *   1. Calculer l'histogramme
     *   2. Normaliser (diviser par M×N → probabilités entre 0 et 1)
     *   3. Calculer l'histogramme cumulé
     *   4. Multiplier par 255 → courbe tonale
     *
     * Exemple :
     *   hist[50]=2, hist[150]=1, hist[200]=1, M×N=4
     *   histn[50]=0.5, histn[150]=0.25, histn[200]=0.25
     *   C[50]=0.5, C[150]=0.75, C[200]=1.0
     *   courbe[50]=127, courbe[150]=191, courbe[200]=255
     *
     * A besoin de l'image car le résultat dépend de son histogramme
     * (différent pour chaque image).
     *
     * @param image image en niveaux de gris (int[][], valeurs 0-255)
     * @return      courbe tonale de 256 valeurs
     */
    public static int[] creeCourbeTonaleEgalisation(int[][] image) {
        int M = image.length;
        int N = image[0].length;
        int[] hist = Histogramme256(image);

        // Étape 2 : normalisation → probabilités
        double[] histn = new double[256];
        for (int k = 0; k < 256; k++) {
            histn[k] = (double) hist[k] / (M * N);
        }

        // Étape 3 : histogramme cumulé
        // C[k] = somme de histn[0] à histn[k]
        double[] C = new double[256];
        C[0] = histn[0];
        for (int k = 1; k < 256; k++) {
            C[k] = C[k-1] + histn[k];
        }

        // Étape 4 : mise à l'échelle 0-1 → 0-255
        int[] courbe = new int[256];
        for (int k = 0; k < 256; k++) {
            courbe[k] = (int) Math.round(255.0 * C[k]);
        }

        return courbe;
    }












}
