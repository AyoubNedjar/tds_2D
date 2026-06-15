package isilimageprocessing;

import CImage.CImageRGB;
import ImageProcessing.NonLineaire.MorphoElementaire;
import javax.swing.JOptionPane;

public class AppEx3PetitsPois {

    private final IsilImageProcessing app;

    public AppEx3PetitsPois(IsilImageProcessing app) {
        this.app = app;
    }

    public void executer() {
        java.io.File f = app.trouverImage("petitsPois.png");
        if (f == null) { app.imageNonTrouvee("petitsPois.png"); return; }
        try {
            CImageRGB rgb = new CImageRGB(f);
            int W = rgb.getLargeur(), H = rgb.getHauteur();
            int[][] red   = new int[W][H];
            int[][] green = new int[W][H];
            int[][] blue  = new int[W][H];
            rgb.getMatricesRGB(red, green, blue);

            int[][] poisRouges = new int[W][H];
            int[][] poisBleus  = new int[W][H];

            // Segmentation couleur : pois = 255, fond = 0
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    int r = red[x][y], g = green[x][y], b = blue[x][y];

                    if (r > 150 && g < 100 && b < 100)
                        poisRouges[x][y] = 255;

                    if (b > 150 && r < 100 && g < 100)
                        poisBleus[x][y] = 255;
                }
            }

            // Ouverture pour supprimer petits parasites (fonctionne sur pois=255)
            poisRouges = MorphoElementaire.ouverture(poisRouges, 5);
            poisBleus  = MorphoElementaire.ouverture(poisBleus,  5);

            // Inversion finale : pois → noir, fond → blanc
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    poisRouges[x][y] = 255 - poisRouges[x][y];
                    poisBleus[x][y]  = 255 - poisBleus[x][y];
                }
            }

            app.placeInSlot(0, poisRouges, "Pois rouges — binaire");
            app.placeInSlot(1, poisBleus,  "Pois bleus  — binaire");
            app.supprimerSlot(2);
            app.supprimerSlot(3);
            app.setActiveSlot(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(app, "Erreur Ex3 : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}