package dev.mrsterner.guardvillagers;


import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.Arrays;
import java.util.List;

public class GuardVillagersConfig extends MidnightConfig {

    @Entry
        public static boolean useSteveModel = false;
    @Entry
        public static boolean GuardsRunFromPolarBears = true;
    @Entry
        public static boolean GuardFormation = true;
    @Entry
        public static boolean ClericHealing = true;
    @Entry
        public static boolean armorerRepairGuardArmor = true;
    @Entry
        public static boolean GuardsOpenDoors = true;
    @Entry
        public static boolean AttackAllMobs = false;

    @Entry
        public static List<String> guardAttackMobBlacklist = Arrays.asList("minecraft:creeper");


    @Entry
        public static boolean giveGuardStuffHOTV = true;

    @Entry
        public static boolean setGuardPatrolHotv = true;
    @Entry
        public static boolean GuardAlwaysShield = true;

    @Entry
        public static boolean FriendlyFire = false;

    @Entry
        public static boolean guardArrowsHurtVillagers = false;

    @Entry
        public static boolean RaidAnimals = false;

    @Entry
        public static boolean WitchesVillager = true;

    @Entry
        public static boolean BlackSmithHealing = true;

    @Entry
        public static double GuardVillagerHelpRange = 50.0D;

    @Entry
        public static boolean ConvertVillagerIfHaveHOTV = false;


    @Entry
        public static int amountOfHealthRegenerated = 1;




}
