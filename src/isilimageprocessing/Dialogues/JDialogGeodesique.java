package isilimageprocessing.Dialogues;

import CImage.CImageNG;
import CImage.Exceptions.CImageNGException;
import CImage.Observers.JLabelBeanCImage;
import ImageProcessing.NonLineaire.MorphoComplexe;
import isilimageprocessing.SlotData;
import isilimageprocessing.SlotReceiver;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

/**
 * Dialogue non-modal pour la dilatation ou la reconstruction géodésique.
 * Affiche marqueur, masque et résultat côte à côte.
 * Permet de choisir le masque parmi les slots et de permuter marqueur/masque.
 */
public class JDialogGeodesique extends JDialog
{
    public enum Type { DILATATION, RECONSTRUCTION }

    private final Type         type;
    private final SlotReceiver receiver;
    private final SlotData[]   masqueSlots;

    private SlotData    markerData;
    private SlotData    masqueData;
    private int[][]     resultat;

    private JScrollPane  scrollMarker;
    private JScrollPane  scrollMasque;
    private JScrollPane  scrollResultat;
    private JSpinner     spinnerIter;
    private JProgressBar progressBar;
    private JButton      btnTransfert;

    /**
     * @param parent      fenêtre parente
     * @param type        DILATATION ou RECONSTRUCTION
     * @param marker      image de départ (slot actif)
     * @param masqueSlots slots disponibles pour le masque (tous les slots NG)
     * @param receiver    callback pour envoyer le résultat vers un slot
     */
    public JDialogGeodesique(Frame parent, Type type,
                              SlotData marker,
                              SlotData[] masqueSlots,
                              SlotReceiver receiver)
    {
        super(parent,
              type == Type.DILATATION ? "Dilatation géodésique" : "Reconstruction géodésique",
              false);
        this.type        = type;
        this.receiver    = receiver;
        this.masqueSlots = (masqueSlots != null) ? masqueSlots : new SlotData[0];
        this.markerData  = marker;
        this.masqueData  = (this.masqueSlots.length > 0) ? this.masqueSlots[0] : null;
        buildUI();
    }

    private void buildUI()
    {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ---- NORD : sélection masque + paramètres + boutons + progress ----
        JPanel nord = new JPanel(new BorderLayout(4, 4));

        // Ligne 1 : boutons de sélection du masque parmi les slots
        JPanel ligneMasque = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        ligneMasque.add(new JLabel("Masque :"));
        for (int i = 0; i < masqueSlots.length; i++) {
            final SlotData sd = masqueSlots[i];
            String nom = (sd.label != null && !sd.label.equals("—"))
                          ? sd.label : ("Slot " + (i + 1));
            if (nom.length() > 14) nom = nom.substring(0, 12) + "…";
            JButton btn = new JButton("Slot " + (i + 1) + " : " + nom);
            btn.setFont(btn.getFont().deriveFont(11f));
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { selectionnerMasque(sd); }
            });
            ligneMasque.add(btn);
        }
        JButton btnFichier = new JButton("Charger fichier…");
        btnFichier.setFont(btnFichier.getFont().deriveFont(11f));
        btnFichier.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { chargerMasqueFichier(); }
        });
        ligneMasque.add(btnFichier);
        nord.add(ligneMasque, BorderLayout.NORTH);

        // Ligne 2 : paramètres + boutons d'action
        JPanel ligneActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        if (type == Type.DILATATION) {
            ligneActions.add(new JLabel("Nb itérations :"));
            spinnerIter = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
            spinnerIter.setPreferredSize(new Dimension(65, 24));
            ligneActions.add(spinnerIter);
        }

        JButton btnPermuter = new JButton("⇄ Permuter opérandes");
        btnPermuter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { permuter(); }
        });

        JButton btnTraitement = new JButton("▶ Traitement");
        btnTraitement.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { lancer(); }
        });

        btnTransfert = new JButton("Transfert →");
        btnTransfert.setEnabled(false);
        btnTransfert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (resultat != null) receiver.accept(resultat, getTitle());
            }
        });

        JButton btnTerminer = new JButton("Terminer");
        btnTerminer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        ligneActions.add(btnPermuter);
        ligneActions.add(Box.createHorizontalStrut(8));
        ligneActions.add(btnTraitement);
        ligneActions.add(btnTransfert);
        ligneActions.add(btnTerminer);
        nord.add(ligneActions, BorderLayout.CENTER);

        // Ligne 3 : barre de progression
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Prêt");
        nord.add(progressBar, BorderLayout.SOUTH);

        add(nord, BorderLayout.NORTH);

        // ---- CENTRE : 3 images côte à côte ----
        JPanel panelImages = new JPanel(new GridLayout(1, 3, 8, 0));

        JPanel pMarqueur = new JPanel(new BorderLayout());
        pMarqueur.setBorder(BorderFactory.createTitledBorder("Marqueur"));
        scrollMarker = creerScroll(markerData != null ? markerData.matrix : null, false);
        pMarqueur.add(scrollMarker, BorderLayout.CENTER);
        panelImages.add(pMarqueur);

        JPanel pMasque = new JPanel(new BorderLayout());
        pMasque.setBorder(BorderFactory.createTitledBorder("Masque géodésique"));
        scrollMasque = creerScroll(masqueData != null ? masqueData.matrix : null, false);
        pMasque.add(scrollMasque, BorderLayout.CENTER);
        panelImages.add(pMasque);

        JPanel pResultat = new JPanel(new BorderLayout());
        pResultat.setBorder(BorderFactory.createTitledBorder("Résultat  (glissez vers un slot)"));
        scrollResultat = new JScrollPane(new JLabel("—", SwingConstants.CENTER));
        scrollResultat.setPreferredSize(new Dimension(240, 270));
        pResultat.add(scrollResultat, BorderLayout.CENTER);
        panelImages.add(pResultat);

        add(panelImages, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(760, 540));
        setLocationRelativeTo(getParent());
    }

    private JScrollPane creerScroll(int[][] matrix, boolean draggable)
    {
        JLabelBeanCImage lbl = new JLabelBeanCImage();
        lbl.setMode(JLabelBeanCImage.INACTIF);
        if (matrix != null) {
            try { lbl.setCImage(new CImageNG(matrix)); }
            catch (CImageNGException ignored) {}
        }
        if (draggable) configurerDrag(lbl);

        JScrollPane sp = new JScrollPane(lbl);
        sp.setPreferredSize(new Dimension(240, 270));
        return sp;
    }

    private void configurerDrag(final JLabelBeanCImage lbl)
    {
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setToolTipText("Glissez cette image vers un slot");
        lbl.setTransferHandler(new TransferHandler() {
            @Override public int getSourceActions(JComponent c) { return COPY; }
            @Override protected Transferable createTransferable(JComponent c) {
                final SlotData sd = new SlotData(resultat, getTitle());
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

    private void selectionnerMasque(SlotData sd)
    {
        masqueData = sd;
        scrollMasque.setViewportView(creerScroll(sd.matrix, false).getViewport().getView());
        scrollMasque.revalidate();
    }

    private void permuter()
    {
        if (masqueData == null) return;
        SlotData tmp = markerData;
        markerData   = masqueData;
        masqueData   = tmp;
        scrollMarker.setViewportView(creerScroll(markerData.matrix, false).getViewport().getView());
        scrollMasque.setViewportView(creerScroll(masqueData.matrix, false).getViewport().getView());
        scrollMarker.revalidate();
        scrollMasque.revalidate();
    }

    private void chargerMasqueFichier()
    {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choisir le masque géodésique (image NG)");
        fc.setCurrentDirectory(new File("."));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            CImageNG img = new CImageNG(fc.getSelectedFile());
            masqueData = new SlotData(img.getMatrice(), fc.getSelectedFile().getName());
            scrollMasque.setViewportView(creerScroll(masqueData.matrix, false).getViewport().getView());
            scrollMasque.revalidate();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erreur lecture : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (CImageNGException ex) {
            JOptionPane.showMessageDialog(this, "Erreur image : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void lancer()
    {
        if (masqueData == null) {
            JOptionPane.showMessageDialog(this,
                "Veuillez sélectionner un masque géodésique.",
                "Masque manquant", JOptionPane.WARNING_MESSAGE);
            return;
        }
        progressBar.setIndeterminate(true);
        progressBar.setString("Calcul en cours…");
        btnTransfert.setEnabled(false);

        final int[][] m     = markerData.matrix;
        final int[][] k     = masqueData.matrix;
        final int nbIter    = (type == Type.DILATATION && spinnerIter != null)
                               ? (Integer) spinnerIter.getValue() : 0;
        final Type t = type;

        new SwingWorker<int[][], Void>() {
            @Override
            protected int[][] doInBackground() throws Exception {
                return (t == Type.RECONSTRUCTION)
                    ? MorphoComplexe.reconstructionGeodesique(m, k)
                    : MorphoComplexe.dilatationGeodesique(m, k, nbIter);
            }
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                try {
                    resultat = get();
                    scrollResultat.setViewportView(
                        creerScroll(resultat, true).getViewport().getView());
                    scrollResultat.revalidate();
                    progressBar.setValue(100);
                    progressBar.setString("Terminé ✓");
                    btnTransfert.setEnabled(true);
                } catch (InterruptedException ex) {
                    progressBar.setString("Annulé");
                } catch (ExecutionException ex) {
                    progressBar.setString("Erreur ✗");
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(JDialogGeodesique.this,
                        cause.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
