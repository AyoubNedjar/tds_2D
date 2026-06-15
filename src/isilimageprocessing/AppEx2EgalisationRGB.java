package isilimageprocessing;

import CImage.CImageRGB;
import ImageProcessing.Histogramme.Histogramme;
import javax.swing.JOptionPane;

public class AppEx2EgalisationRGB {

    private final IsilImageProcessing app;

    public AppEx2EgalisationRGB(IsilImageProcessing app) {
        this.app = app;
    }

    public void executer() {
        java.io.File f = app.trouverImage("lenaAEgaliser.jpg");
        if (f == null) { app.imageNonTrouvee("lenaAEgaliser.jpg"); return; }
        try {
            CImageRGB rgb = new CImageRGB(f);
            int W = rgb.getLargeur(), H = rgb.getHauteur();
            int[][] red   = new int[W][H];
            int[][] green = new int[W][H];
            int[][] blue  = new int[W][H];
            rgb.getMatricesRGB(red, green, blue);

            // (a) égalisation séparée canal par canal
            int[][] rA = Histogramme.rehaussement(red,   Histogramme.creeCourbeTonaleEgalisation(red));
            int[][] gA = Histogramme.rehaussement(green, Histogramme.creeCourbeTonaleEgalisation(green));
            int[][] bA = Histogramme.rehaussement(blue,  Histogramme.creeCourbeTonaleEgalisation(blue));
            CImageRGB resultA = new CImageRGB(rA, gA, bA);

            // (b) égalisation de la luminance — même courbe sur les 3 canaux
            int[][] lum = new int[W][H];
            for (int x = 0; x < W; x++)
                for (int y = 0; y < H; y++)
                    lum[x][y] = (red[x][y] + green[x][y] + blue[x][y]) / 3;
            int[] cL   = Histogramme.creeCourbeTonaleEgalisation(lum);
            int[][] rB = Histogramme.rehaussement(red,   cL);
            int[][] gB = Histogramme.rehaussement(green, cL);
            int[][] bB = Histogramme.rehaussement(blue,  cL);
            CImageRGB resultB = new CImageRGB(rB, gB, bB);

            app.placeInSlotRGB(0, rgb,     "Lena — original");
            app.placeInSlotRGB(1, resultA, "Lena — égalisée (a) canaux séparés");
            app.placeInSlotRGB(2, resultB, "Lena — égalisée (b) luminance");
            app.supprimerSlot(3);
            app.setActiveSlot(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(app, "Erreur Ex2 : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}
