package isilimageprocessing.Dialogues;

import ImageProcessing.Histogramme.Histogramme;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialogue modal affichant les paramètres statistiques d'une image NG :
 * dimensions, minimum, maximum, luminance, contraste (écart-type et Michelson).
 */
public class JDialogParametresImage extends JDialog
{
    public JDialogParametresImage(Frame parent, int[][] image)
    {
        super(parent, "Paramètres de l'image", true);
        buildUI(image);
    }

    private void buildUI(int[][] image)
    {
        int    M   = image.length;
        int    N   = image[0].length;
        int    min = Histogramme.minimum(image);
        int    max = Histogramme.maximum(image);
        int    lum = Histogramme.luminance(image);
        double c1  = Histogramme.contraste1(image);
        double c2  = Histogramme.contraste2(image);

        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        // Titre interne
        JLabel titre = new JLabel("Statistiques de l'image active", SwingConstants.CENTER);
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 13f));
        titre.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(titre, BorderLayout.NORTH);

        // Grille de paramètres (label à gauche, valeur à droite)
        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 8));
        grid.setBorder(BorderFactory.createTitledBorder("Valeurs calculées"));

        row(grid, "Dimensions",           M + " × " + N + " pixels");
        row(grid, "Niveau minimum",       String.valueOf(min));
        row(grid, "Niveau maximum",       String.valueOf(max));
        row(grid, "Luminance  (µ)",       String.valueOf(lum));
        row(grid, "Contraste  (σ)",       String.format("%.4f", c1));
        row(grid, "Contraste Michelson",  String.format("%.4f", c2));

        add(grid, BorderLayout.CENTER);

        // Bouton Fermer
        JButton btnFermer = new JButton("Fermer");
        btnFermer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.add(btnFermer);
        add(south, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(340, 240));
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private void row(JPanel grid, String label, String valeur)
    {
        JLabel lbl = new JLabel(label + " :");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        grid.add(lbl);
        grid.add(new JLabel(valeur));
    }
}
