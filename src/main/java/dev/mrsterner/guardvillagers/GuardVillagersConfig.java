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
    public int amountOfHealthRegenerated = 1;


    public List<String> MobBlackList = Arrays.asList("creeper");



    public static GuardVillagersConfig get() {
        return AutoConfig.getConfigHolder(GuardVillagersConfig.class).getConfig();
    }
}
