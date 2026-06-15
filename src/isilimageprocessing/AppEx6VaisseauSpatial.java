package isilimageprocessing;

import CImage.CImageNG;
import CImage.CImageRGB;
import ImageProcessing.Contours.ContoursNonLineaire;
import ImageProcessing.NonLineaire.MorphoComplexe;
import ImageProcessing.NonLineaire.MorphoElementaire;
import ImageProcessing.Seuillage.Seuillage;
import javax.swing.JOptionPane;

public class AppEx6VaisseauSpatial {

    private final IsilImageProcessing app;

    public AppEx6VaisseauSpatial(IsilImageProcessing app) {
        this.app = app;
    }

    public void executer() {
        java.io.File fV = app.trouverImage("vaisseaux.jpg");
        java.io.File fP = app.trouverImage("planete.jpg");
        if (fV == null) { app.imageNonTrouvee("vaisseaux.jpg"); return; }
        if (fP == null) { app.imageNonTrouvee("planete.jpg");   return; }
        try {
            int[][] matriceV = new CImageNG(fV).getMatrice();
            CImageRGB planeteRGB = new CImageRGB(fP);
            int MV = matriceV.length, NV = matriceV[0].length;
            int MP = planeteRGB.getLargeur(), NP = planeteRGB.getHauteur();

            // Étape 1 : seuillage simple (seuil 5) pour isoler tout ce qui n'est pas fond noir
            int[][] seuillageSimple = Seuillage.seuillageSimple(matriceV, 5);

            // Étape 2 : érosion taille 3 pour enlever les étoiles
            int[][] erosion = MorphoElementaire.erosion(seuillageSimple, 3);

            // Étape 3 : reconstruction → retrouve les 2 vaisseaux sans étoiles
            int[][] reconstruitVaisseaux = MorphoComplexe.reconstructionGeodesique(erosion, seuillageSimple);

            // Étape 4 : érosion taille 51 → seul le grand vaisseau survit
            int[][] erosionPetit = MorphoElementaire.erosion(reconstruitVaisseaux, 51);

            // Étape 5 : reconstruction → retrouve proprement le grand vaisseau
            int[][] grandVaisseau = MorphoComplexe.reconstructionGeodesique(erosionPetit, reconstruitVaisseaux);

            // Étape 6 : soustraction → isole le petit vaisseau
            int[][] petitVaisseau = new int[MV][NV];
            for (int i = 0; i < MV; i++)
                for (int j = 0; j < NV; j++)
                    petitVaisseau[i][j] = Math.max(0, reconstruitVaisseaux[i][j] - grandVaisseau[i][j]);

            // Étape 7 : érosion taille 9 pour enlever traces blanches/écriture
            int[][] erosionEcriture = MorphoElementaire.erosion(petitVaisseau, 9);

            // Étape 8 : reconstruction finale du petit vaisseau propre
            int[][] petitVaisseauFinal = MorphoComplexe.reconstructionGeodesique(erosionEcriture, reconstruitVaisseaux);

            // Étape 9 : coller le petit vaisseau sur la planète RGB
            int[][] rP = new int[MP][NP];
            int[][] gP = new int[MP][NP];
            int[][] bP = new int[MP][NP];
            planeteRGB.getMatricesRGB(rP, gP, bP);

            int Mmin = Math.min(MV, MP), Nmin = Math.min(NV, NP);
            for (int i = 0; i < Mmin; i++)
                for (int j = 0; j < Nmin; j++)
                    if (petitVaisseauFinal[i][j] != 0) {
                        rP[i][j] = matriceV[i][j];
                        gP[i][j] = matriceV[i][j];
                        bP[i][j] = matriceV[i][j];
                    }

            CImageRGB synthese = new CImageRGB(rP, gP, bP);
            synthese.enregistreFormatPNG(new java.io.File("synthese.png"));

            // Étape 10 : contour rouge avec gradientDilatation
            int[][] contour = ContoursNonLineaire.gradientDilatation(petitVaisseauFinal, 3);

            int[][] rS = new int[MP][NP];
            int[][] gS = new int[MP][NP];
            int[][] bS = new int[MP][NP];
            synthese.getMatricesRGB(rS, gS, bS);

            for (int i = 0; i < Mmin; i++)
                for (int j = 0; j < Nmin; j++)
                    if (contour[i][j] == 255) {
                        rS[i][j] = 255;
                        gS[i][j] = 0;
                        bS[i][j] = 0;
                    }

            CImageRGB synthese2 = new CImageRGB(rS, gS, bS);
            synthese2.enregistreFormatPNG(new java.io.File("synthese2.png"));

            app.placeInSlotRGB(0, synthese,  "Vaisseau — synthèse");
            app.placeInSlotRGB(1, synthese2, "Vaisseau — contour rouge");
            app.supprimerSlot(2);
            app.supprimerSlot(3);
            app.setActiveSlot(0);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(app, "Erreur Ex6 : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}