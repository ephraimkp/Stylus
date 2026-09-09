# Stylus Notes

Appli de prise de notes manuscrites avec rejet de paume **logiciel**, pensée pour
un stylet capacitif non détecté comme `TOOL_TYPE_STYLUS` par Android.

## Installation

1. Ouvrir ce dossier dans Android Studio (`File > Open`).
2. Laisser Gradle synchroniser (première fois : peut prendre quelques minutes).
3. Lancer sur un appareil ou un émulateur (`Run > Run 'app'`).

## Comment ça marche

Toute la logique de rejet de paume est dans `DrawingView.kt` (commentée en détail).
Trois heuristiques combinées, réglables depuis le bouton "Rejet de paume" dans l'écran
de dessin :

1. **Taille de contact** (`getSize()`) : rejette tout point de contact trop large
   pour être une pointe de stylet.
2. **Premier tracé gagnant** : dès qu'un tracé est en cours, tout nouveau doigt/paume
   posé est ignoré tant que le premier n'est pas levé.
3. **Zone d'écriture** (optionnelle) : ignore une bande en haut de l'écran, utile si
   tu tiens la tablette avec la paume qui repose systématiquement en haut.

## Réglage à faire toi-même au premier lancement

Le curseur de sensibilité (bouton "Rejet de paume") doit être ajusté à ton stylet et
ton appareil : commence permissif, et resserre-le jusqu'à ce que ta paume ne
dessine plus mais que la pointe du stylet reste fluide. `getSize()` est normalisé
par le fabricant de l'écran, donc ça varie d'un appareil à l'autre — pas de valeur
universelle.

## Limite connue

Si la paume touche l'écran **avant** la pointe du stylet (main posée puis on
écrit), l'heuristique 2 bloque tout, y compris le stylet, tant que la paume n'est
pas levée. C'est un compromis volontaire : mieux vaut devoir lever la main entre
deux mots que de voir des gribouillis. Si ça gêne, active plutôt la "zone
d'écriture" (heuristique 3) et pose ta paume hors de cette zone.

## Vérifier si ton stylet est finalement détecté

Ajoute temporairement un `Log.d("stylus", event.getToolType(index).toString())`
dans `isAcceptablePointer()` : si tu vois `2` (TOOL_TYPE_STYLUS) au lieu de `1`
(TOOL_TYPE_FINGER), ton stylet EST reconnu et l'app bascule automatiquement sur le
signal fiable — tu peux alors désactiver les heuristiques 1/3.
