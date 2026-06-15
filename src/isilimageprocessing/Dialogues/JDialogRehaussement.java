package isilimageprocessing.Dialogues;

import CImage.CImageNG;
import CImage.Exceptions.CImageNGException;
import CImage.Observers.JLabelBeanCImage;
import ImageProcessing.Histogramme.Histogramme;
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
 * Dialogue non-modal pour le rehaussement d'histogramme.
 * Affiche l'image originale / résultat et leurs histogrammes côte à côte.
 */
public class JDialogRehaussement extends JDialog
{
    public enum Type { LINEAIRE, LINEAIRE_SAT, GAMMA, EGALISATION, NEGATIF }

    private final Type         type;
    private final int[][]      imageOriginale;
    private final SlotReceiver receiver;

    private int[][]      resultat;
    private JProgressBar progressBar;
    private JButton      btnTransfert;
    private JScrollPane  scrollResultat;
    private JPanel       panelHistoResultat;

    // Spinners selon le type
    private JSpinner spSmin;
    private JSpinner spSmax;
    private JSpinner spGamma;

    public JDialogRehaussement(Frame parent, Type type,
                                int[][] imageOriginale,
                                SlotReceiver receiver)
    {
        super(parent, getTitre(type), false);
        this.type           = type;
        this.imageOriginale = imageOriginale;
        this.receiver       = receiver;
        buildUI();
    }

    // -----------------------------------------------------------------------

    private static String getTitre(Type t)
    {
        switch (t) {
            case LINEAIRE:     return "Transformation linéaire (sans saturation)";
            case LINEAIRE_SAT: return "Transformation linéaire avec saturation";
            case GAMMA:        return "Correction Gamma";
            case EGALISATION:  return "Égalisation de l'histogramme";
            case NEGATIF:      return "Négatif";
            default:           return "Rehaussement";
        }
    }

    // -----------------------------------------------------------------------

    private void buildUI()
    {
        setLayout(new BorderLayout(6, 6));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ---- NORD : paramètres + boutons + barre ----
        JPanel nord = new JPanel(new BorderLayout(4, 4));

        JPanel ligne1 = new JPanel(new BorderLayout(6, 0));
        ligne1.add(buildParamPanel(), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        JButton btnCourbe = new JButton("Courbe tonale");
        btnCourbe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { afficherCourbeTonale(); }
        });

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

        btns.add(btnCourbe);
        btns.add(btnTraitement);
        btns.add(btnTransfert);
        btns.add(btnTerminer);
        ligne1.add(btns, BorderLayout.EAST);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Prêt");

        nord.add(ligne1,    BorderLayout.CENTER);
        nord.add(progressBar, BorderLayout.SOUTH);
        add(nord, BorderLayout.NORTH);

        // ---- CENTRE : images + histogrammes ----
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));

        // Images côte à côte
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

        // Histogrammes côte à côte
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
        setMinimumSize(new Dimension(760, 650));
        setLocationRelativeTo(getParent());
    }

    // -----------------------------------------------------------------------

    /** Construit le panel de paramètres selon le type. */
    private JPanel buildParamPanel()
    {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        switch (type) {
            case LINEAIRE:
                p.add(new JLabel("Paramètres calculés automatiquement  (smin = min image, smax = max image)"));
                break;
            case LINEAIRE_SAT:
                p.add(new JLabel("smin :"));
                spSmin = new JSpinner(new SpinnerNumberModel(0, 0, 254, 1));
                spSmin.setPreferredSize(new Dimension(62, 24));
                p.add(spSmin);
                p.add(new JLabel("   smax :"));
                spSmax = new JSpinner(new SpinnerNumberModel(255, 1, 255, 1));
                spSmax.setPreferredSize(new Dimension(62, 24));
                p.add(spSmax);
                break;
            case GAMMA:
                p.add(new JLabel("Gamma (γ) :"));
                spGamma = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
                spGamma.setPreferredSize(new Dimension(72, 24));
                p.add(spGamma);
                p.add(new JLabel("   (γ > 1 éclaircit, γ < 1 assombrit)"));
                break;
            case EGALISATION:
                p.add(new JLabel("Aucun paramètre — histogramme aplati automatiquement"));
                break;
            case NEGATIF:
                p.add(new JLabel("Aucun paramètre — inversion des niveaux de gris  (255 − i)"));
                break;
        }
        return p;
    }

    /** Calcule la courbe tonale selon le type et les valeurs des spinners. */
    private int[] calculerCourbeTonale()
    {
        switch (type) {
            case LINEAIRE:
                return Histogramme.creeCourbeTonaleLineaireSaturation(
                    Histogramme.minimum(imageOriginale),
                    Histogramme.maximum(imageOriginale));
            case LINEAIRE_SAT:
                return Histogramme.creeCourbeTonaleLineaireSaturation(
                    (Integer) spSmin.getValue(),
                    (Integer) spSmax.getValue());
            case GAMMA:
                return Histogramme.creeCourbeTonaleGamma(
                    ((Number) spGamma.getValue()).doubleValue());
            case EGALISATION:
                return Histogramme.creeCourbeTonaleEgalisation(imageOriginale);
            case NEGATIF:
                return Histogramme.creeCourbeTonaleNegatif();
            default:
                return null;
        }
    }

    // -----------------------------------------------------------------------

    /** Affiche la courbe tonale dans une fenêtre popup. */
    private void afficherCourbeTonale()
    {
        int[] courbe = calculerCourbeTonale();
        if (courbe == null) return;

        XYSeries serie = new XYSeries("Courbe tonale");
        for (int i = 0; i < 256; i++) serie.add(i, courbe[i]);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serie);

        JFreeChart chart = ChartFactory.createXYLineChart(
            "Courbe tonale", "Entrée (0–255)", "Sortie (0–255)",
            dataset, PlotOrientation.VERTICAL, false, false, false);

        XYPlot plot = (XYPlot) chart.getXYPlot();
        plot.getRenderer().setSeriesPaint(0, Color.BLUE);
        plot.getDomainAxis().setRange(0, 255);
        plot.getRangeAxis().setRange(0, 255);

        JFrame popup = new JFrame("Courbe tonale — " + getTitle());
        popup.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(420, 380));
        popup.add(cp);
        popup.pack();
        popup.setLocationRelativeTo(this);
        popup.setVisible(true);
    }

    /** Lance le calcul en arrière-plan. */
    private void lancer()
    {
        progressBar.setIndeterminate(true);
        progressBar.setString("Calcul en cours…");
        btnTransfert.setEnabled(false);

        final int[] courbe = calculerCourbeTonale();

        new SwingWorker<int[][], Void>() {
            @Override
            protected int[][] doInBackground() throws Exception {
                return Histogramme.rehaussement(imageOriginale, courbe);
            }
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                try {
                    resultat = get();

                    // Mettre à jour l'image résultat
                    scrollResultat.setViewportView(
                        creerScrollImage(resultat, true).getViewport().getView());
                    scrollResultat.revalidate();

                    // Mettre à jour l'histogramme résultat
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
                    JOptionPane.showMessageDialog(JDialogRehaussement.this,
                        cause.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // -----------------------------------------------------------------------

    /** Crée un JScrollPane contenant un JLabelBeanCImage (draggable ou non). */
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

    /**
     * Crée un ChartPanel avec un histogramme à barres rouges,
     * axe X de 0 à 255, axe Y = nombre de pixels.
     */
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
