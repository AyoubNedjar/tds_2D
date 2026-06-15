package ImageProcessing.Seuillage;

public class Seuillage {

    public static int[][] seuillageSimple(int[][] image, int seuil) {
        int M = image.length, N = image[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                resultat[i][j] = image[i][j] >= seuil ? 255 : 0;
        return resultat;
    }

    public static int[][] seuillageDouble(int[][] image, int seuil1, int seuil2) {
        int M = image.length, N = image[0].length;
        int[][] resultat = new int[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++) {
                if      (image[i][j] < seuil1)  resultat[i][j] = 0;
                else if (image[i][j] < seuil2)  resultat[i][j] = 128;
                else                             resultat[i][j] = 255;
            }
        return resultat;
    }

    public static int[][] seuillageAutomatique(int[][] image) {
        int M = image.length, N = image[0].length;

        // Seuil initial = moyenne de l'image
        long somme = 0;
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                somme += image[i][j];
        double seuil = (double) somme / (M * N);

        // Itération jusqu'à stabilité
        while (true) {
            double m1 = 0, m2 = 0;
            int c1 = 0, c2 = 0;
            for (int i = 0; i < M; i++)
                for (int j = 0; j < N; j++) {
                    if (image[i][j] < seuil) { m1 += image[i][j]; c1++; }
                    else                      { m2 += image[i][j]; c2++; }
                }
            if (c1 > 0) m1 /= c1;
            if (c2 > 0) m2 /= c2;

            double nouveauSeuil = (m1 + m2) / 2.0;
            if (Math.abs(nouveauSeuil - seuil) < 0.5) break;
            seuil = nouveauSeuil;
        }

        return seuillageSimple(image, (int) Math.round(seuil));
    }
}