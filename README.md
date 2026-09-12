# Bulk Enchant

Mod Fabric pour Minecraft **1.21.8** (Fabric Loader **0.18.4**, Fabric API `0.136.1+1.21.8`) qui permet
d'enchanter ou de désenchanter tout son inventaire d'un coup.

## Fonctionnalités

### 1. Table d'enchantement — tout enchanter
Comme d'habitude, on pose un objet + du lapis, et les 3 options apparaissent. En cliquant
normalement, seul l'objet posé est enchanté (comportement vanilla inchangé).

**Maj + clic** (Shift-click) sur une des 3 options applique cet enchantement à **tous les
objets compatibles de l'inventaire principal** (les 36 emplacements : réserve + barre
d'accès rapide), en une seule fois. Le coût en lapis et en niveaux d'XP est multiplié par
le nombre d'objets réellement enchantés (chaque objet subit son propre tirage d'enchantement,
comme le ferait la table normalement pour son type — une épée et une pioche ne recevront donc
pas forcément le même enchantement, mais tous consomment le même "palier" choisi).

Un petit indice texte ("Maj+clic : tout enchanter") s'affiche dans l'interface pour rappeler
le raccourci.

### 2. Meule — tout désenchanter
Un nouveau bouton **"Tout désenchanter"** apparaît dans l'interface de la meule. Il retire les
enchantements (sauf les malédictions, comme la meule vanilla) de tous les objets enchantés de
l'inventaire principal, et donne l'expérience correspondante d'un coup — exactement comme si
vous aviez passé chaque objet dans la meule un par un.

## Structure du projet

Projet Fabric standard (généré à la main, pas via un template) :

```
bulkenchant/
├── build.gradle, settings.gradle, gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/...
└── src/main/
    ├── java/com/bulkenchant/
    │   ├── BulkEnchantMod.java            (point d'entrée commun)
    │   ├── BulkEnchantNetworking.java     (paquets client -> serveur)
    │   ├── api/                           (interfaces exposées par les mixins)
    │   ├── mixin/                         (logique serveur : table + meule)
    │   ├── mixin/client/                  (Maj+clic dans l'écran de la table)
    │   └── client/                        (bouton "Tout désenchanter")
    └── resources/
        ├── fabric.mod.json
        ├── bulkenchant.mixins.json / .client.mixins.json
        └── assets/bulkenchant/lang/{fr_fr,en_us}.json
```

## Compiler / lancer en développement

Il faut un **JDK 21** (Minecraft 1.21.x l'exige). Le wrapper Gradle fourni télécharge le
reste tout seul (Gradle 9.7.1, Fabric Loom, Minecraft, les mappings Yarn, Fabric API).

```bash
./gradlew build
```

Pour tester directement (lance un serveur ou un client de développement) :

```bash
./gradlew runServer
./gradlew runClient
```

Le premier lancement télécharge Minecraft et décompile les mappings : ça peut prendre
plusieurs minutes.

## Installer le mod compilé

Après `./gradlew build`, le fichier à déposer dans le dossier `mods/` d'une instance Fabric
1.21.8 (avec Fabric API installé) se trouve dans `build/libs/bulkenchant-1.0.0.jar`.

## Vérifications faites pendant le développement

Ce mod a été écrit en consultant les **vraies sources décompilées** de Minecraft 1.21.8
(via `./gradlew genSources`) pour les classes `EnchantmentScreenHandler`,
`GrindstoneScreenHandler`, `EnchantmentScreen`, `GrindstoneScreen`, `ItemStack`,
`EnchantmentHelper` et `PlayerEntity`, afin que les mixins visent les bons champs/méthodes.
Le projet a ensuite été **compilé et lancé avec un vrai serveur de développement** : les
deux mixins serveur (table d'enchantement, meule) s'appliquent sans erreur au démarrage.

Le mixin côté client (Maj+clic dans l'écran de la table) n'a en revanche pas pu être testé
en jeu dans cet environnement (pas d'affichage graphique disponible ici) — vérifiez ce point
en jeu, et signalez tout souci pour correction.

## Personnaliser

- `gradle.properties` : `maven_group`, `archives_base_name`, `mod_version`.
- `fabric.mod.json` : `authors`, `description`.
- Les textes affichés en jeu sont dans `src/main/resources/assets/bulkenchant/lang/`.
