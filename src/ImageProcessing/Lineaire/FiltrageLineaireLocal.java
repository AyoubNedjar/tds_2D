package ImageProcessing.Lineaire;

/**
 * ============================================================
 *  FILTRAGE LINEAIRE LOCAL
 * ============================================================
 *
 *  C'EST QUOI UNE IMAGE ICI ?
 *  ----------------------------
 *  Une image = un tableau de tableaux d'entiers : int[][]
 *  Chaque case = 1 pixel
 *  Valeur d'un pixel : entre 0 (noir) et 255 (blanc)
 *
 *  Exemple image 3x3 :
 *    { {200, 198, 15 },
 *      {202, 199, 197},
 *      {195, 201, 200} }
 *
 *  IMPORTANT : on travaille pixel par pixel sur 1 seule matrice.
 *  Pour une image couleur (RGB), on appelle ces methodes 3 fois :
 *    → filtreMoyenneur(matriceRouge, 3)
 *    → filtreMoyenneur(matriceVerte, 3)
 *    → filtreMoyenneur(matriceBleu,  3)
 *  Chaque canal est lisse independamment. Les resultats ne
 *  s'additionnent PAS — chaque canal garde sa propre matrice.
 *
 *  C'EST QUOI UN MASQUE ?
 *  -----------------------
 *  Le masque = une grille de poids (double[][])
 *  Chaque poids dit "combien ce voisin compte" dans le calcul.
 *
 *  Exemple masque 3x3 moyenneur (tous egaux a 1/9) :
 *    { {1/9, 1/9, 1/9},
 *      {1/9, 1/9, 1/9},
 *      {1/9, 1/9, 1/9} }
 *  → chaque voisin compte pareil → on fait juste la moyenne
 *
 *  Exemple masque gaussien (centre plus important) :
 *    { {1/16, 2/16, 1/16},
 *      {2/16, 4/16, 2/16},
 *      {1/16, 2/16, 1/16} }
 *  → le pixel central a poids 4 → influence 4x plus que les coins
 *  → flou plus doux, contours moins degrades qu'avec le moyenneur
 *
 *  Exemple masque detection contours :
 *    { {-1, -1, -1},
 *      {-1,  8, -1},
 *      {-1, -1, -1} }
 *  → zone uniforme : 200x8 - (200x8) = 0 → noir
 *  → bord d'objet  : 200x8 - (voisins differents) = valeur elevee → blanc
 *  → seules les differences entre voisins ressortent
 *
 *  POURQUOI 1/9 ?
 *  ---------------
 *  Masque 3x3 = 9 cases.
 *  Pour faire la moyenne de 9 nombres, on divise par 9.
 *  Multiplier chaque voisin par 1/9 puis additionner
 *  = faire la somme et diviser par 9 → meme resultat.
 *
 *  REGLE D'OR : la somme de tous les poids doit faire 1
 *  pour ne pas changer la luminosite globale de l'image.
 *  9 x (1/9) = 1 ✓
 *
 *  C'EST QUOI LE RAYON ?
 *  ----------------------
 *  Le rayon = distance entre le centre et le bord du masque.
 *  Masque 3x3 → rayon = 3/2 = 1 (division entiere Java)
 *  Masque 5x5 → rayon = 5/2 = 2
 *  Ca sert dans la boucle : on va de -rayon a +rayon
 *  pour visiter tous les voisins autour du pixel central.
 *
 *  LIEN ENTRE LES DEUX METHODES :
 *  --------------------------------
 *  filtreMoyenneur est un CAS PARTICULIER de filtreMasqueConvolution.
 *  filtreMoyenneur cree le masque 1/n² puis appelle filtreMasqueConvolution.
 *  filtreMasqueConvolution fait le vrai calcul pixel par pixel.
 * ============================================================
 */
public class FiltrageLineaireLocal {

    /**
     * METHODE 1 — filtreMasqueConvolution (le moteur generique)
     * -----------------------------------------------------------
     * Applique N'IMPORTE QUEL masque de convolution sur l'image.
     * Les coefficients du masque peuvent etre tous differents.
     * C'est la methode principale — filtreMoyenneur l'appelle.
     *
     * CALCUL POUR CHAQUE PIXEL (i, j) :
     *   somme = 0
     *   Pour chaque voisin (ni, nj) dans le carre nxn autour de (i,j) :
     *     somme += pixel[ni][nj] × masque[k+rayon][l+rayon]
     *   resultat[i][j] = somme (clampee entre 0 et 255)
     *
     * @param image   matrice de pixels originale (int[][], valeurs 0-255)
     * @param masque  grille de poids (double[][], taille nxn, n impair)
     * @return        nouvelle matrice filtree (meme taille que image)
     */
    public static int[][] filtreMasqueConvolution(int[][] image, double[][] masque) {

        int hauteur = image.length;
        int largeur = image[0].length;
        int tailleMasque = masque.length;

        // rayon = combien de cases on se deplace depuis le centre
        // masque 3x3 → rayon = 1 → on va de -1 a +1 (9 voisins)
        // masque 5x5 → rayon = 2 → on va de -2 a +2 (25 voisins)
        int rayon = tailleMasque / 2;

        // nouvelle matrice pour stocker le resultat
        // on ne modifie JAMAIS l'image originale directement
        int[][] resultat = new int[hauteur][largeur];

        // boucle sur chaque pixel de l'image
        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {

                double somme = 0;

                // boucle sur chaque voisin dans le masque
                // k et l representent le deplacement par rapport au pixel central
                // ex: rayon=1 → k = -1, 0, +1  et  l = -1, 0, +1
                for (int k = -rayon; k <= rayon; k++) {
                    for (int l = -rayon; l <= rayon; l++) {

                        // position reelle du voisin dans l'image
                        int ni = i + k;
                        int nj = j + l;

                        // on ignore les voisins en dehors de l'image
                        // (pixels sur les bords n'ont pas tous leurs voisins)
                        if (ni >= 0 && ni < hauteur && nj >= 0 && nj < largeur) {

                            // valeur du voisin × son poids dans le masque
                            // k+rayon convertit -1,0,+1 en 0,1,2
                            // car les indices de tableau commencent a 0
                            somme += image[ni][nj] * masque[k + rayon][l + rayon];
                        }
                    }
                }

                // clamp : on force le resultat entre 0 et 255
                // necessaire car avec des poids negatifs la somme
                // peut sortir de la plage 0-255
                resultat[i][j] = Math.min(255, Math.max(0, (int) Math.round(somme)));
            }
        }

        return resultat;
    }

    /**
     * METHODE 2 — filtreMoyenneur (le raccourci)
     * -----------------------------------------------------------
     * Cree un masque ou tous les coefficients valent 1/(taille²)
     * puis appelle filtreMasqueConvolution avec ce masque.
     *
     * C'est exactement faire la moyenne des voisins !
     *   tailleMasque=3 → masque 3x3, chaque coeff = 1/9
     *   tailleMasque=5 → masque 5x5, chaque coeff = 1/25
     *
     * EFFET VISUEL :
     *   - Supprime le bruit (pixels parasites disparaissent)
     *   - Rend l'image plus floue
     *   - Plus tailleMasque est grand, plus le flou est fort
     *
     * @param image        matrice de pixels originale (int[][], valeurs 0-255)
     * @param tailleMasque taille du masque carre (doit etre impair : 3, 5, 7...)
     * @return             nouvelle matrice lissee (meme taille que image)
     */
    public static int[][] filtreMoyenneur(int[][] image, int tailleMasque) {

        // coefficient = 1 / nombre total de cases
        // masque 3x3 → 9 cases  → coeff = 1/9   ≈ 0.111
        // masque 5x5 → 25 cases → coeff = 1/25  = 0.04
        double coeff = 1.0 / (tailleMasque * tailleMasque);

        // creation du masque : toutes les cases ont le meme poids
        double[][] masque = new double[tailleMasque][tailleMasque];
        for (int i = 0; i < tailleMasque; i++) {
            for (int j = 0; j < tailleMasque; j++) {
                masque[i][j] = coeff;
            }
        }

        // on delegue tout le calcul a filtreMasqueConvolution
        // filtreMoyenneur ne fait rien d'autre que preparer le masque
        return filtreMasqueConvolution(image, masque);
    }
}