# Bulk Enchant

Mod Fabric pour Minecraft **1.21.8** (Fabric Loader **0.18.4**, Fabric API `0.136.1+1.21.8`) qui permet
d'enchanter ou de désenchanter tout son inventaire d'un coup.

**100% côté client** : rien à installer sur le serveur, fonctionne même sur un serveur vanilla
ou un serveur que tu ne gères pas toi-même. Le mod ne fait qu'automatiser les clics normaux
(comme si tu cliquais toi-même très vite, un objet après l'autre) — voir la section
"Comment ça marche" plus bas pour les implications.

## Fonctionnalités

### 1. Table d'enchantement — tout enchanter
Comme d'habitude, on pose un objet + du lapis, et les 3 options apparaissent. En cliquant
normalement, seul l'objet posé est enchanté (comportement vanilla inchangé).

**Maj + clic** (Shift-click) sur une des 3 options lance l'enchantement automatique de
**tous les objets de l'inventaire principal (27 emplacements)**, un par un, avec le palier
choisi. Chaque objet reçoit son propre tirage d'enchantement (comme le ferait la table
normalement pour son type). Le lapis est puisé automatiquement dans les emplacements 2 à 8
de la barre d'accès rapide au fur et à mesure des besoins.

Un petit indice texte ("Maj+clic : tout enchanter") s'affiche dans l'interface pour rappeler
le raccourci.

### 2. Meule — tout désenchanter
Un nouveau bouton **"Tout désenchanter"** apparaît dans l'interface de la meule. Il passe
automatiquement chacun des 27 objets de l'inventaire principal dans la meule (retire les
enchantements sauf les malédictions, donne l'expérience correspondante), un par un.

## Comment ça marche (important)

Ce mod **n'a aucune logique côté serveur**. Il simule, depuis le client, exactement les
mêmes actions qu'un joueur ferait à la souris (déplacer un objet dans la table/meule, cliquer
le bouton d'enchantement, récupérer le résultat), une par tick de jeu (~1 objet toutes les
0.05s, donc les 27 objets défilent en un peu plus d'une seconde). Comme ce sont des actions
100% vanilla, n'importe quel serveur les accepte sans rien savoir de ce mod.

Limites à connaître :
- Un peu plus lent qu'un vrai traitement "en masse" instantané, puisque chaque objet suit le
  circuit normal (poser / cliquer / récupérer).
- Un serveur avec un système anti-triche strict contre les actions automatisées / trop rapides
  pourrait bloquer ou sanctionner ce comportement — à utiliser en connaissance de cause,
  notamment sur des serveurs communautaires avec règlement.
- Suppose que les 27 objets à traiter sont dans les 27 emplacements de la réserve principale
  (lignes 2 à 4 de l'inventaire), et que le lapis est disponible dans les emplacements 2 à 8
  de la barre d'accès rapide (les emplacements 0 et 1 ne sont pas touchés).

## Structure du projet

Projet Fabric standard (généré à la main, pas via un template) :

```
bulkenchant/
├── build.gradle, settings.gradle, gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/...
├── .github/workflows/build.yml        (compilation automatique via GitHub Actions)
└── src/main/
    ├── java/com/bulkenchant/
    │   ├── client/
    │   │   ├── BulkEnchantClientMod.java   (point d'entrée client, bouton meule)
    │   │   └── BulkEnchantMacro.java       (moteur de la macro : clics automatiques)
    │   └── mixin/client/
    │       └── EnchantmentScreenMixin.java (Maj+clic dans l'écran de la table)
    └── resources/
        ├── fabric.mod.json
        ├── bulkenchant.client.mixins.json
        └── assets/bulkenchant/lang/{fr_fr,en_us}.json
```

## Compiler / lancer en développement

Il faut un **JDK 21** (Minecraft 1.21.x l'exige). Le wrapper Gradle fourni télécharge le
reste tout seul (Gradle 9.7.1, Fabric Loom, Minecraft, les mappings Yarn, Fabric API).

```bash
./gradlew build
```

Pour tester directement :

```bash
./gradlew runClient
```

Le premier lancement télécharge Minecraft et décompile les mappings : ça peut prendre
plusieurs minutes.

## Installer le mod compilé

Après `./gradlew build`, le fichier à déposer dans le dossier `mods/` (côté client uniquement)
se trouve dans `build/libs/bulkenchant-1.0.0.jar`. Il faut aussi **Fabric API** dans le même
dossier `mods/`.

Le dépôt GitHub compile aussi automatiquement le mod à chaque push (onglet **Actions**) et
publie le `.jar` en artefact téléchargeable.

## Vérifications faites pendant le développement

Ce mod a été écrit en consultant les **vraies sources décompilées** de Minecraft 1.21.8
(via `./gradlew genSources`) pour les classes `EnchantmentScreenHandler`,
`GrindstoneScreenHandler`, `EnchantmentScreen`, `GrindstoneScreen`, `ScreenHandler`,
`ClientPlayerInteractionManager` et `ItemStack`, pour obtenir les bons index d'emplacements
et les bonnes signatures de méthodes. Le projet a été compilé et lancé avec un vrai client de
développement Fabric : le mixin `EnchantmentScreenMixin` s'applique sans erreur au runtime
(vérifié en forçant le chargement de la classe `EnchantmentScreen` au démarrage).

Le déroulé complet de la macro (clics enchaînés sur 27 emplacements en jeu réel) n'a en
revanche pas pu être testé manuellement dans cet environnement (pas d'interaction souris/jeu
disponible ici) — teste-le en jeu et signale tout comportement inattendu.

## Personnaliser

- `gradle.properties` : `maven_group`, `archives_base_name`, `mod_version`.
- `fabric.mod.json` : `authors`, `description`.
- Les textes affichés en jeu sont dans `src/main/resources/assets/bulkenchant/lang/`.
- Les emplacements ciblés (objets / lapis) sont des constantes en haut de
  `BulkEnchantMacro.java`, à ajuster si ta disposition d'inventaire diffère.
