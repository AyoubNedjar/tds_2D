package isilimageprocessing;

import CImage.CImageNG;
import ImageProcessing.NonLineaire.MorphoElementaire;
import ImageProcessing.Seuillage.Seuillage;
import javax.swing.JOptionPane;

public class AppEx5SegmentationOutils {

    private final IsilImageProcessing app;

    public AppEx5SegmentationOutils(IsilImageProcessing app) {
        this.app = app;
    }

    public void executer() {
        java.io.File f = app.trouverImage("tools.png");
        if (f == null) { app.imageNonTrouvee("tools.png"); return; }
        try {
            int[][] orig = new CImageNG(f).getMatrice();
            int M = orig.length, N = orig[0].length;

            // Étape 1 : ouverture taille 21 → isole le fond
            int[][] fond = MorphoElementaire.ouverture(orig, 21);

            // Étape 2 : soustraction orig - fond → isole les outils
            int[][] soustrait = new int[M][N];
            for (int i = 0; i < M; i++)
                for (int j = 0; j < N; j++)
                    soustrait[i][j] = Math.max(0, orig[i][j] - fond[i][j]);

            // Étape 3 : seuillage automatique → image binaire
            int[][] resultat = Seuillage.seuillageAutomatique(soustrait);

            app.placeInSlot(0, orig,      "Outils — original");
            app.placeInSlot(1, resultat,  "Outils — segmentés");
            app.supprimerSlot(2);
            app.supprimerSlot(3);
            app.setActiveSlot(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(app, "Erreur Ex5 : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}