package isilimageprocessing;

import CImage.CImageNG;
import CImage.CImageRGB;
import ImageProcessing.Contours.ContoursNonLineaire;
import ImageProcessing.NonLineaire.MorphoElementaire;
import ImageProcessing.Seuillage.Seuillage;
import javax.swing.JOptionPane;

public class AppEx7ContoursTartines {

    private final IsilImageProcessing app;

    public AppEx7ContoursTartines(IsilImageProcessing app) {
        this.app = app;
    }

    public void executer() {
        java.io.File f = app.trouverImage("Tartines.jpg");
        if (f == null) { app.imageNonTrouvee("Tartines.jpg"); return; }
        try {
            CImageRGB imgRGB = new CImageRGB(f);
            int M = imgRGB.getLargeur(), N = imgRGB.getHauteur();

            int[][] red   = new int[M][N];
            int[][] green = new int[M][N];
            int[][] blue  = new int[M][N];
            imgRGB.getMatricesRGB(red, green, blue);

            // Étape 1 : ouverture sur canal vert → supprime reflets
            int[][] ouverture = MorphoElementaire.ouverture(green, 11);

            // Étape 2 : gradient Beucher pour détecter les contours
            int[][] gradient = ContoursNonLineaire.gradientBeucher(ouverture, 3);

            // Étape 3 : seuillage simple
            int[][] contourMask = Seuillage.seuillageSimple(gradient, 67);

            // Étape 4 : tracé en vert épais sur image originale
            int epaisseur = 2;
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    if (contourMask[i][j] == 255) {
                        for (int di = -epaisseur; di <= epaisseur; di++) {
                            for (int dj = -epaisseur; dj <= epaisseur; dj++) {
                                int ni = i + di, nj = j + dj;
                                if (ni >= 0 && ni < M && nj >= 0 && nj < N) {
                                    red[ni][nj]   = 0;
                                    green[ni][nj] = 255;
                                    blue[ni][nj]  = 0;
                                }
                            }
                        }
                    }
                }
            }

            CImageRGB resultat = new CImageRGB(red, green, blue);
            int[][] orig = new CImageNG(f).getMatrice();

            app.placeInSlot(0,    orig,     "Tartines — original");
            app.placeInSlotRGB(1, resultat, "Tartines — contours verts");
            app.supprimerSlot(2);
            app.supprimerSlot(3);
            app.setActiveSlot(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(app, "Erreur Ex7 : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}