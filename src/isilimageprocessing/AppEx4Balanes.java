package isilimageprocessing;

import CImage.CImageNG;
import ImageProcessing.NonLineaire.MorphoComplexe;
import ImageProcessing.NonLineaire.MorphoElementaire;
import javax.swing.JOptionPane;

public class AppEx4Balanes {

    private final IsilImageProcessing app;

    public AppEx4Balanes(IsilImageProcessing app) {
        this.app = app;
    }

    public void executer() {
        java.io.File f = app.trouverImage("balanes.png");
        if (f == null) { app.imageNonTrouvee("balanes.png"); return; }
        try {
            int[][] orig = new CImageNG(f).getMatrice();
            int M = orig.length, N = orig[0].length;

            // Grandes balanes : survivent à érosion taille 15
            int[][] erodeeGrande   = MorphoElementaire.erosion(orig, 15);
            int[][] grandesBalanes = MorphoComplexe.reconstructionGeodesique(erodeeGrande, orig);

// Toutes les balanes : survivent à érosion taille 5
            int[][] erodeePetite  = MorphoElementaire.erosion(orig, 5);
            int[][] toutesBalanes = MorphoComplexe.reconstructionGeodesique(erodeePetite, orig);

// Petites balanes = toutes - grandes
            int[][] petitesBalanes = new int[M][N];
            for (int i = 0; i < M; i++)
                for (int j = 0; j < N; j++)
                    petitesBalanes[i][j] = Math.max(0, toutesBalanes[i][j] - grandesBalanes[i][j]);

            app.placeInSlot(0, grandesBalanes, "Balanes — grandes");
            app.placeInSlot(1, petitesBalanes, "Balanes — petites");
            app.supprimerSlot(2);
            app.supprimerSlot(3);
            app.setActiveSlot(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(app, "Erreur Ex4 : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}
