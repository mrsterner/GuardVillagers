package dev.mrsterner.guardvillagers;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(name = GuardVillagers.MODID)
public class GuardVillagersConfig implements ConfigData {
    public boolean useSteveModel = false;
    @ConfigEntry.Gui.RequiresRestart
    public boolean GuardsRunFromPolarBears = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean GuardFormation = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean ClericHealing = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean armorerRepairGuardArmor = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean GuardsOpenDoors = true;

    @ConfigEntry.Gui.RequiresRestart
    public boolean AttackAllMobs = false;

    @ConfigEntry.Gui.RequiresRestart
    public boolean giveGuardStuffHOTV = true;

    @ConfigEntry.Gui.RequiresRestart
    public boolean setGuardPatrolHotv = true;
    @ConfigEntry.Gui.RequiresRestart
    public boolean GuardAlwaysShield = true;

    @ConfigEntry.Gui.RequiresRestart
    public boolean FriendlyFire = false;

    @ConfigEntry.Gui.RequiresRestart
    public boolean guardArrowsHurtVillagers = false;

    @ConfigEntry.Gui.RequiresRestart
    public boolean RaidAnimals = false;

    @ConfigEntry.Gui.RequiresRestart
    public boolean WitchesVillager = true;

    @ConfigEntry.Gui.RequiresRestart
    public boolean BlackSmithHealing = true;



    @ConfigEntry.Gui.RequiresRestart
    public double GuardVillagerHelpRange = 50.0D;

    @ConfigEntry.Gui.RequiresRestart
    public boolean ConvertVillagerIfHaveHOTV = false;






    @ConfigEntry.Gui.RequiresRestart
    public int amountOfHealthRegenerated = 1;


    public List<String> MobBlackList = Arrays.asList("minecraft:creeper");



    public static GuardVillagersConfig get() {
        return AutoConfig.getConfigHolder(GuardVillagersConfig.class).getConfig();
    }
}
