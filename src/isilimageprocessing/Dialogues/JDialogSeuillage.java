package isilimageprocessing.Dialogues;

import CImage.CImageNG;
import CImage.Exceptions.CImageNGException;
import CImage.Observers.JLabelBeanCImage;
import ImageProcessing.Histogramme.Histogramme;
import ImageProcessing.Seuillage.Seuillage;
import isilimageprocessing.SlotData;
import isilimageprocessing.SlotReceiver;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Dialogue non-modal pour les opérations de seuillage.
 * Affiche l'image originale / résultat et leurs histogrammes côte à côte.
 */
public class JDialogSeuillage extends JDialog
{
    public enum Type { SIMPLE, DOUBLE, AUTOMATIQUE }

    private final Type         type;
    private final int[][]      imageOriginale;
    private final SlotReceiver receiver;

    private int[][]      resultat;
    private JProgressBar progressBar;
    private JButton      btnTransfert;
    private JScrollPane  scrollResultat;
    private JPanel       panelHistoResultat;

    private JSpinner spSeuil;   // SIMPLE
    private JSpinner spSeuil1;  // DOUBLE
    private JSpinner spSeuil2;  // DOUBLE

    // -----------------------------------------------------------------------

    public JDialogSeuillage(Frame parent, Type type,
                             int[][] imageOriginale, SlotReceiver receiver)
    {
        super(parent, getTitre(type), false);
        this.type           = type;
        this.imageOriginale = imageOriginale;
        this.receiver       = receiver;
        buildUI();
    }

    private static String getTitre(Type t)
    {
        switch (t) {
            case SIMPLE:      return "Seuillage simple";
            case DOUBLE:      return "Seuillage double";
            case AUTOMATIQUE: return "Seuillage automatique";
            default:          return "Seuillage";
        }
    }

    // -----------------------------------------------------------------------

    private void buildUI()
    {
        setLayout(new BorderLayout(6, 6));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ---- NORD : paramètres + boutons + barre de progression ----
        JPanel nord = new JPanel(new BorderLayout(4, 4));

        JPanel ligne1 = new JPanel(new BorderLayout(6, 0));
        ligne1.add(buildParamPanel(), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        JButton btnTraitement = new JButton("Traitement");
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

        btns.add(btnTraitement);
        btns.add(btnTransfert);
        btns.add(btnTerminer);
        ligne1.add(btns, BorderLayout.EAST);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Prêt");

        nord.add(ligne1,      BorderLayout.CENTER);
        nord.add(progressBar, BorderLayout.SOUTH);
        add(nord, BorderLayout.NORTH);

        // ---- CENTRE : images côte à côte + histogrammes côte à côte ----
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));

        // Images
        JPanel imagesRow = new JPanel(new GridLayout(1, 2, 8, 0));

        JPanel pOrig = new JPanel(new BorderLayout());
        pOrig.setBorder(BorderFactory.createTitledBorder("Image originale"));
        pOrig.add(creerScrollImage(imageOriginale, false), BorderLayout.CENTER);
        imagesRow.add(pOrig);

        JPanel pRes = new JPanel(new BorderLayout());
        pRes.setBorder(BorderFactory.createTitledBorder("Résultat  (glissez vers un slot)"));
        scrollResultat = new JScrollPane(
            new JLabel("— cliquez sur Traitement —", SwingConstants.CENTER));
        scrollResultat.setPreferredSize(new Dimension(310, 220));
        pRes.add(scrollResultat, BorderLayout.CENTER);
        imagesRow.add(pRes);

        centre.add(imagesRow);
        centre.add(Box.createVerticalStrut(6));

        // Histogrammes
        JPanel histosRow = new JPanel(new GridLayout(1, 2, 8, 0));

        JPanel pHistoOrig = new JPanel(new BorderLayout());
        pHistoOrig.setBorder(BorderFactory.createTitledBorder("Histogramme original"));
        pHistoOrig.add(creerChartPanel(Histogramme.Histogramme256(imageOriginale)), BorderLayout.CENTER);
        histosRow.add(pHistoOrig);

        panelHistoResultat = new JPanel(new BorderLayout());
        panelHistoResultat.setBorder(BorderFactory.createTitledBorder("Histogramme résultat"));
        JLabel lblVide = new JLabel("—", SwingConstants.CENTER);
        lblVide.setPreferredSize(new Dimension(310, 170));
        panelHistoResultat.add(lblVide, BorderLayout.CENTER);
        histosRow.add(panelHistoResultat);

        centre.add(histosRow);
        add(centre, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(760, 620));
        setLocationRelativeTo(getParent());
    }

    // -----------------------------------------------------------------------

    private JPanel buildParamPanel()
    {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        switch (type) {
            case SIMPLE:
                p.add(new JLabel("Seuil :"));
                spSeuil = new JSpinner(new SpinnerNumberModel(128, 0, 255, 1));
                spSeuil.setPreferredSize(new Dimension(62, 24));
                p.add(spSeuil);
                break;
            case DOUBLE:
                p.add(new JLabel("Seuil 1 :"));
                spSeuil1 = new JSpinner(new SpinnerNumberModel(85, 0, 255, 1));
                spSeuil1.setPreferredSize(new Dimension(62, 24));
                p.add(spSeuil1);
                p.add(new JLabel("   Seuil 2 :"));
                spSeuil2 = new JSpinner(new SpinnerNumberModel(170, 0, 255, 1));
                spSeuil2.setPreferredSize(new Dimension(62, 24));
                p.add(spSeuil2);
                break;
            case AUTOMATIQUE:
                p.add(new JLabel("Cet opérateur ne nécessite aucun paramètre"));
                break;
        }
        return p;
    }

    // -----------------------------------------------------------------------

    private int[][] calculer()
    {
        switch (type) {
            case SIMPLE:
                return Seuillage.seuillageSimple(imageOriginale, (Integer) spSeuil.getValue());
            case DOUBLE:
                return Seuillage.seuillageDouble(imageOriginale,
                    (Integer) spSeuil1.getValue(), (Integer) spSeuil2.getValue());
            case AUTOMATIQUE:
                return Seuillage.seuillageAutomatique(imageOriginale);
            default:
                return null;
        }
    }

    private void lancer()
    {
        progressBar.setIndeterminate(true);
        progressBar.setString("Calcul en cours…");
        btnTransfert.setEnabled(false);

        new SwingWorker<int[][], Void>() {
            @Override
            protected int[][] doInBackground() throws Exception {
                return calculer();
            }
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                try {
                    resultat = get();

                    scrollResultat.setViewportView(
                        creerScrollImage(resultat, true).getViewport().getView());
                    scrollResultat.revalidate();

                    panelHistoResultat.removeAll();
                    panelHistoResultat.add(
                        creerChartPanel(Histogramme.Histogramme256(resultat)),
                        BorderLayout.CENTER);
                    panelHistoResultat.revalidate();
                    panelHistoResultat.repaint();

                    progressBar.setValue(100);
                    progressBar.setString("Terminé ✓");
                    btnTransfert.setEnabled(true);
                } catch (InterruptedException ex) {
                    progressBar.setString("Annulé");
                } catch (ExecutionException ex) {
                    progressBar.setString("Erreur ✗");
                    Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(JDialogSeuillage.this,
                        cause.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // -----------------------------------------------------------------------

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
            lbl.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    JComponent c = (JComponent) e.getSource();
                    c.getTransferHandler().exportAsDrag(c, e, TransferHandler.COPY);
                }
            });
        }

        JScrollPane sp = new JScrollPane(lbl);
        sp.setPreferredSize(new Dimension(310, 220));
        return sp;
    }

    private ChartPanel creerChartPanel(int[] histo)
    {
        XYSeries serie = new XYSeries("histogramme");
        for (int i = 0; i < 256; i++) serie.add(i, histo[i]);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serie);

        JFreeChart chart = ChartFactory.createHistogram(
            null, "Niveaux de gris", "Nb pixels",
            dataset, PlotOrientation.VERTICAL, false, false, false);

        XYPlot plot = (XYPlot) chart.getXYPlot();
        plot.getRenderer().setSeriesPaint(0, Color.RED);
        plot.getDomainAxis().setRange(0, 255);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(310, 170));
        return cp;
    }
}
