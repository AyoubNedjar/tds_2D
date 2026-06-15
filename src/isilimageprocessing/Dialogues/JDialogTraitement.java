package isilimageprocessing.Dialogues;

import CImage.CImageNG;
import CImage.Exceptions.CImageNGException;
import CImage.Observers.JLabelBeanCImage;
import isilimageprocessing.SlotData;
import isilimageprocessing.SlotReceiver;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

/**
 * Dialogue générique non-modal pour toute transformation image.
 * Affiche l'originale à gauche, le résultat (draggable) à droite.
 * Paramètres configurables via un JPanel fourni par l'appelant.
 */
public class JDialogTraitement extends JDialog
{
    /** Action de calcul fournie par l'appelant (exécutée hors EDT). */
    public interface ComputeAction {
        int[][] compute() throws Exception;
    }

    /** Panel réutilisable pour la saisie d'un masque de convolution. */
    public static class MasquePanel extends JPanel
    {
        private JTextField[][] champs;
        private int taille = 3;
        private final JPanel grille = new JPanel();

        public MasquePanel()
        {
            setLayout(new BorderLayout(4, 4));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            top.add(new JLabel("Taille :"));
            final JSpinner sp = new JSpinner(new SpinnerNumberModel(3, 3, 11, 2));
            top.add(sp);
            JButton btn = new JButton("Valider");
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    miseAJour((Integer) sp.getValue());
                }
            });
            top.add(btn);
            add(top, BorderLayout.NORTH);

            JScrollPane sc = new JScrollPane(grille);
            sc.setPreferredSize(new Dimension(175, 135));
            add(sc, BorderLayout.CENTER);

            miseAJour(3);
        }

        private void miseAJour(int n)
        {
            taille = n;
            champs = new JTextField[n][n];
            grille.removeAll();
            grille.setLayout(new GridLayout(n, n, 2, 2));
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) {
                    champs[i][j] = new JTextField("0", 3);
                    champs[i][j].setHorizontalAlignment(JTextField.CENTER);
                    grille.add(champs[i][j]);
                }
            grille.revalidate();
            grille.repaint();
        }

        /** Lève NumberFormatException si une cellule contient une valeur invalide. */
        public double[][] getMasque() throws NumberFormatException
        {
            double[][] m = new double[taille][taille];
            for (int i = 0; i < taille; i++)
                for (int j = 0; j < taille; j++) {
                    String txt = champs[i][j].getText().trim().replace(",", ".");
                    m[i][j] = Double.parseDouble(txt);
                }
            return m;
        }
    }

    // -----------------------------------------------------------------------

    private final SlotReceiver slotReceiver;
    private int[][]     resultat;
    private JProgressBar progressBar;
    private JScrollPane  scrollResultat;
    private JButton      btnTransfert;

    public JDialogTraitement(Frame parent, String titre,
                              final int[][] imageOriginale,
                              JPanel panelParametres,
                              final ComputeAction action,
                              SlotReceiver slotReceiver)
    {
        super(parent, titre, false); // non-modal : drop vers la fenêtre principale possible
        this.slotReceiver = slotReceiver;
        buildUI(imageOriginale, panelParametres, action);
    }

    private void buildUI(final int[][] imageOriginale,
                          JPanel panelParametres,
                          final ComputeAction action)
    {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ---- NORD : paramètres + boutons + barre de progression ----
        JPanel nord = new JPanel(new BorderLayout(6, 4));

        JPanel ligne1 = new JPanel(new BorderLayout(6, 0));
        ligne1.add(panelParametres, BorderLayout.CENTER);

        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        JButton btnTraitement = new JButton("Traitement");
        btnTraitement.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { lancer(action); }
        });

        btnTransfert = new JButton("Transfert →");
        btnTransfert.setEnabled(false);
        btnTransfert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (resultat != null) slotReceiver.accept(resultat, getTitle());
            }
        });

        JButton btnTerminer = new JButton("Terminer");
        btnTerminer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        boutons.add(btnTraitement);
        boutons.add(btnTransfert);
        boutons.add(btnTerminer);
        ligne1.add(boutons, BorderLayout.EAST);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Prêt");

        nord.add(ligne1, BorderLayout.CENTER);
        nord.add(progressBar, BorderLayout.SOUTH);
        add(nord, BorderLayout.NORTH);

        // ---- CENTRE : images côte à côte ----
        JPanel panelImages = new JPanel(new GridLayout(1, 2, 8, 0));

        JPanel gauche = new JPanel(new BorderLayout());
        gauche.setBorder(BorderFactory.createTitledBorder("Image originale"));
        gauche.add(creerScrollImage(imageOriginale, false), BorderLayout.CENTER);
        panelImages.add(gauche);

        JPanel droite = new JPanel(new BorderLayout());
        droite.setBorder(BorderFactory.createTitledBorder("Résultat  (glissez vers un slot)"));
        scrollResultat = new JScrollPane(
            new JLabel("— cliquez sur Traitement —", SwingConstants.CENTER));
        scrollResultat.setPreferredSize(new Dimension(330, 300));
        droite.add(scrollResultat, BorderLayout.CENTER);
        panelImages.add(droite);

        add(panelImages, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(740, 490));
        setLocationRelativeTo(getParent());
    }

    private JScrollPane creerScrollImage(int[][] matrix, boolean draggable)
    {
        JLabelBeanCImage lbl = new JLabelBeanCImage();
        lbl.setMode(JLabelBeanCImage.INACTIF);
        try { lbl.setCImage(new CImageNG(matrix)); }
        catch (CImageNGException ignored) {}

        if (draggable) {
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.setToolTipText("Glissez cette image vers un slot");
            final String titre = getTitle();
            lbl.setTransferHandler(new TransferHandler() {
                @Override public int getSourceActions(JComponent c) { return COPY; }
                @Override protected Transferable createTransferable(JComponent c) {
                    final SlotData sd = new SlotData(resultat, titre);
                    return new Transferable() {
                        public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{ SlotData.FLAVOR }; }
                        public boolean isDataFlavorSupported(DataFlavor f) { return SlotData.FLAVOR.equals(f); }
                        public Object getTransferData(DataFlavor f) { return sd; }
                    };
                }
            });
            lbl.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    JComponent c = (JComponent) e.getSource();
                    c.getTransferHandler().exportAsDrag(c, e, TransferHandler.COPY);
                }
            });
        }

        JScrollPane sp = new JScrollPane(lbl);
        sp.setPreferredSize(new Dimension(330, 300));
        return sp;
    }

    private void lancer(final ComputeAction action)
    {
        progressBar.setIndeterminate(true);
        progressBar.setString("Calcul en cours…");
        btnTransfert.setEnabled(false);

        new SwingWorker<int[][], Void>() {
            @Override
            protected int[][] doInBackground() throws Exception { return action.compute(); }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                try {
                    resultat = get();
                    scrollResultat.setViewportView(
                        creerScrollImage(resultat, true).getViewport().getView());
                    scrollResultat.revalidate();
                    progressBar.setValue(100);
                    progressBar.setString("Terminé ✓");
                    btnTransfert.setEnabled(true);
                } catch (InterruptedException ex) {
                    progressBar.setString("Annulé");
                } catch (ExecutionException ex) {
                    progressBar.setString("Erreur ✗");
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(JDialogTraitement.this,
                        cause.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
