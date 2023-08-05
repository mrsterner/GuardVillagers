package dev.sterner.guardvillagers;

import eu.midnightdust.lib.config.MidnightConfig;

import java.util.ArrayList;
import java.util.List;

public class GuardVillagersConfig extends MidnightConfig {

    @Entry
    public static int reputationRequirementToBeAttacked = -100;
    @Entry
    public static int reputationRequirement = 15;
    @Entry
    public static boolean guardEntitysRunFromPolarBears = false;
    @Entry
    public static boolean guardEntitysOpenDoors = true;
    @Entry
    public static boolean guardEntityFormation = true;
    @Entry
    public static boolean clericHealing = true;
    @Entry
    public static boolean armorerRepairGuardEntityArmor = true;
    @Entry
    public static boolean attackAllMobs = false;
    @Entry
    public static boolean guardAlwaysShield = false;
    @Entry
    public static boolean friendlyFire = true;
    @Entry
    public static List<String> mobBlackList = new ArrayList<>();
    @Entry
    public static float amountOfHealthRegenerated = 1F;
    @Entry
    public static boolean followHero = true;
    @Entry
    public static double healthModifier = 20D;
    @Entry
    public static double speedModifier = 0.5D;
    @Entry
    public static double followRangeModifier = 20D;
    @Entry
    public static boolean giveGuardStuffHotv = false;
    @Entry
    public static boolean setGuardPatrolHotv = false;
    @Entry
    public static float chanceToDropEquipment = 100F;
    @Entry
    public static boolean useSteveModel = false;
    @Entry
    public static boolean raidAnimals = false;
    @Entry
    public static boolean witchesVillager = true;
    @Entry
    public static boolean blackSmithHealing = true;
    @Entry
    public static boolean convertVillagerIfHaveHotv = false;
    @Entry
    public static double guardVillagerHelpRange = 50;
    @Entry
    public static boolean illagersRunFromPolarBears = true;
    @Entry
    public static boolean villagersRunFromPolarBears = true;
    @Entry
    public static boolean guardArrowsHurtVillagers = true;
}
