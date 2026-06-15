package isilimageprocessing;

/** Callback utilisé par les dialogues pour envoyer un résultat vers un slot. */
public interface SlotReceiver {
    void accept(int[][] matrice, String label);
}
