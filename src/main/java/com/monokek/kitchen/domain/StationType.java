package com.monokek.kitchen.domain;

/**
 * What a kitchen station actually prepares — lets a screen (like the POS's
 * "Comptoir Bar") find "the bar station" reliably instead of hardcoding an
 * id that only happens to match in whichever database it was written
 * against, or guessing from a substring of the station's display name.
 */
public enum StationType {
    KITCHEN, BAR, GRILL, PASTRY, OTHER
}
