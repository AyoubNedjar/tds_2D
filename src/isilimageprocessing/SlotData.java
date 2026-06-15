package isilimageprocessing;

import java.awt.datatransfer.DataFlavor;

/** Données transférées lors d'un glisser-déposer entre slots ou depuis un dialogue. */
public class SlotData {
    public static final DataFlavor FLAVOR = new DataFlavor(SlotData.class, "SlotData");

    public final int[][] matrix;
    public final String  label;

    public SlotData(int[][] matrix, String label) {
        this.matrix = matrix;
        this.label  = (label != null) ? label : "—";
    }
}
