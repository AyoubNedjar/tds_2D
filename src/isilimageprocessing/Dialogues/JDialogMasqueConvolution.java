package isilimageprocessing.Dialogues;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialogue permettant à l'utilisateur de saisir un masque de convolution.
 * L'utilisateur choisit d'abord la taille (impaire), puis remplit chaque case.
 */
public class JDialogMasqueConvolution extends JDialog
{
    private double[][] masque;
    private int taille;
    private JTextField[][] champsMasque;
    private JPanel panelMasque;
    private JSpinner spinnerTaille;

    public JDialogMasqueConvolution(Frame parent, boolean modal)
    {
        super(parent, "Masque de convolution", modal);
        masque = null;
        initComponents();
    }

    private void initComponents()
    {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel du haut : choix de la taille ---
        JPanel panelTaille = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTaille.add(new JLabel("Taille du masque (impair) :"));

        SpinnerNumberModel model = new SpinnerNumberModel(3, 3, 11, 2);
        spinnerTaille = new JSpinner(model);
        panelTaille.add(spinnerTaille);

        JButton btnValiderTaille = new JButton("Valider taille");
        btnValiderTaille.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mettreAJourGrille((Integer) spinnerTaille.getValue());
            }
        });
        panelTaille.add(btnValiderTaille);
        add(panelTaille, BorderLayout.NORTH);

        // --- Panel central : grille du masque ---
        panelMasque = new JPanel();
        add(new JScrollPane(panelMasque), BorderLayout.CENTER);

        // --- Panel du bas : OK / Annuler ---
        JPanel panelBoutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                valider();
            }
        });
        JButton btnAnnuler = new JButton("Annuler");
        btnAnnuler.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        panelBoutons.add(btnOK);
        panelBoutons.add(btnAnnuler);
        add(panelBoutons, BorderLayout.SOUTH);

        mettreAJourGrille(3);

        setMinimumSize(new Dimension(300, 200));
        pack();
        setLocationRelativeTo(getParent());
    }

    private void mettreAJourGrille(int n)
    {
        taille = n;
        champsMasque = new JTextField[n][n];
        panelMasque.removeAll();
        panelMasque.setLayout(new GridLayout(n, n, 5, 5));
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                champsMasque[i][j] = new JTextField("0", 5);
                champsMasque[i][j].setHorizontalAlignment(JTextField.CENTER);
                panelMasque.add(champsMasque[i][j]);
            }
        }
        pack();
    }

    private void valider()
    {
        double[][] m = new double[taille][taille];
        for (int i = 0; i < taille; i++)
        {
            for (int j = 0; j < taille; j++)
            {
                String texte = champsMasque[i][j].getText().trim().replace(",", ".");
                try
                {
                    m[i][j] = Double.parseDouble(texte);
                }
                catch (NumberFormatException ex)
                {
                    JOptionPane.showMessageDialog(this,
                        "Valeur invalide à la case (" + i + ", " + j + ") : \"" + texte + "\"",
                        "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        masque = m;
        dispose();
    }

    /** Retourne le masque saisi, ou null si l'utilisateur a annulé. */
    public double[][] getMasque()
    {
        return masque;
    }
}
