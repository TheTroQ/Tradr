package me.dratii.tradr;

//import baritone.api.IBaritone;
import net.kyori.adventure.audience.Audience;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.ArrayList;

public class Globals {
    public static boolean enabled = false;
    public static boolean finished = true;
    public static boolean openVillager = false;
    public static ArrayList<VillagerEntity> tradedVillagers = new ArrayList<>();
    public static ArrayList<VillagerEntity> availableVillagers = new ArrayList<>();
    //public static IBaritone baritoneAPI;
    public static Audience player;
}
