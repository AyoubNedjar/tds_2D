package isilimageprocessing;

import CImage.CImageNG;
import ImageProcessing.NonLineaire.MorphoComplexe;
import ImageProcessing.Lineaire.FiltrageLineaireLocal;
import javax.swing.JOptionPane;

public class AppEx1ReductionBruit {

    private final IsilImageProcessing app;

    public AppEx1ReductionBruit(IsilImageProcessing app) {
        this.app = app;
    }

    public void executer() {
        java.io.File f1 = app.trouverImage("imageBruitee1.png");
        java.io.File f2 = app.trouverImage("imageBruitee2.png");
        if (f1 == null) { app.imageNonTrouvee("imageBruitee1.png"); return; }
        if (f2 == null) { app.imageNonTrouvee("imageBruitee2.png"); return; }
        try {
            int[][] m1 = new CImageNG(f1).getMatrice();
            int[][] m2 = new CImageNG(f2).getMatrice();

            // Image 1 (fleur) : bruit sel & poivre dense
            // → 2 passes de médian 3×3, puis médian 5×5
            int[][] r1 = MorphoComplexe.filtreMedian(m1, 3);
            r1 = MorphoComplexe.filtreMedian(r1, 3);
            r1 = MorphoComplexe.filtreMedian(r1, 5);

            // Image 2 (portrait) : bruit structuré (rayures)
            // → médian 5×5 pour casser les lignes, puis moyenneur 3×3 pour lisser
            int[][] r2 = MorphoComplexe.filtreMedian(m2, 5);
            r2 = FiltrageLineaireLocal.filtreMoyenneur(r2, 3);

            app.placeInSlot(0, m1, "Bruitée 1 — original");
            app.placeInSlot(1, r1, "Bruitée 1 — médian ×2 + médian 5×5");
            app.placeInSlot(2, m2, "Bruitée 2 — original");
            app.placeInSlot(3, r2, "Bruitée 2 — médian 5×5 + moyenneur");
            app.setActiveSlot(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(app, "Erreur Ex1 : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}