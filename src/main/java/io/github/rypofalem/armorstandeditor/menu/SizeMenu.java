package io.github.rypofalem.armorstandeditor.menu;

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import io.github.rypofalem.armorstandeditor.Debug;
import io.github.rypofalem.armorstandeditor.PlayerEditor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SizeMenu extends ASEHolder {

    public ArmorStandEditorPlugin plugin = ArmorStandEditorPlugin.instance();
    Inventory menuInv;
    private Debug debug;
    private PlayerEditor pe;
    private ArmorStand as;
    static String name = "Size Menu";

    private final Map<String, Double> presetScaleOptions = new HashMap<>();
    private final Map<String, Double> increaseScaleOptions = new HashMap<>();
    private final Map<String, Double> decreaseScaleOptions = new HashMap<>();

    private static final int PRESET_COUNT = 6;
    private static final double MINIMUM_STEP = 0.01d;
    private static final double COARSE_STEP_DIVISOR = 5d;
    private static final double FINE_STEP_DIVISOR = 4d;

    private String backToMenuDisplayName = "";
    private String resetDisplayName = "";

    public SizeMenu(PlayerEditor pe, ArmorStand as) {
        this.pe = pe;
        this.as = as;
        this.debug = new Debug(pe.plugin);
        name = pe.plugin.getLang().getMessage("sizeMenu", "menutitle");
        menuInv = Bukkit.createInventory(pe.getManager().getSizeMenuHolder(), 27, name);
    }

    private void fillInventory() {
        menuInv.clear();
        presetScaleOptions.clear();
        increaseScaleOptions.clear();
        decreaseScaleOptions.clear();

        ItemStack blankSlot = createIcon(new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1), "blankslot");
        ItemStack backToMenu = createIcon(new ItemStack(Material.RED_WOOL, 1), "backtomenu");
        ItemStack resetIcon = createIcon(new ItemStack(Material.NETHER_STAR, 1), "reset");
        backToMenuDisplayName = getDisplayName(backToMenu);
        resetDisplayName = getDisplayName(resetIcon);

        double minScale = roundScaleValue(plugin.getMinScaleValue());
        double maxScale = roundScaleValue(plugin.getMaxScaleValue());
        if (maxScale < minScale) {
            maxScale = minScale;
        }

        double[] presetValues = generatePresetValues(minScale, maxScale);
        ItemStack[] presetIcons = buildPresetIcons(presetValues);

        double coarseStep = calculateCoarseStep(minScale, maxScale);
        double fineStep = calculateFineStep(coarseStep);

        ItemStack increaseCoarse = createScaleIncreaseIcon(new ItemStack(Material.ORANGE_CONCRETE, 1), coarseStep, true);
        ItemStack increaseFine = createScaleIncreaseIcon(new ItemStack(Material.ORANGE_CONCRETE, 1), fineStep, false);
        ItemStack decreaseCoarse = createScaleDecreaseIcon(new ItemStack(Material.GREEN_CONCRETE, 1), coarseStep, true);
        ItemStack decreaseFine = createScaleDecreaseIcon(new ItemStack(Material.GREEN_CONCRETE, 1), fineStep, false);

        ItemStack[] items = {
            backToMenu, blankSlot, getPresetIcon(presetIcons, 0, blankSlot), getPresetIcon(presetIcons, 1, blankSlot),
            getPresetIcon(presetIcons, 2, blankSlot), getPresetIcon(presetIcons, 3, blankSlot),
            getPresetIcon(presetIcons, 4, blankSlot), getPresetIcon(presetIcons, 5, blankSlot), blankSlot,
            resetIcon, blankSlot, blankSlot, increaseCoarse, increaseFine, blankSlot, blankSlot, decreaseFine, decreaseCoarse,
            blankSlot, blankSlot, blankSlot, blankSlot, blankSlot, blankSlot, blankSlot, blankSlot, blankSlot
        };

        menuInv.setContents(items);
    }

    private ItemStack getPresetIcon(ItemStack[] icons, int index, ItemStack fallback) {
        return icons.length > index ? icons[index] : fallback;
    }

    private ItemStack[] buildPresetIcons(double[] presets) {
        ArrayList<ItemStack> icons = new ArrayList<>();
        Material[] palette = {
            Material.LIGHT_BLUE_CONCRETE,
            Material.CYAN_CONCRETE,
            Material.BLUE_CONCRETE,
            Material.PURPLE_CONCRETE,
            Material.MAGENTA_CONCRETE,
            Material.PINK_CONCRETE
        };

        for (int i = 0; i < presets.length; i++) {
            Material material = palette[Math.min(i, palette.length - 1)];
            icons.add(createScalePresetIcon(new ItemStack(material, 1), presets[i]));
        }
        return icons.toArray(new ItemStack[0]);
    }

    private ItemStack createScalePresetIcon(ItemStack icon, double targetScale) {
        String formattedValue = formatScaleValue(targetScale);
        ItemStack stack = createIcon(icon, "scalepreset", formattedValue);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            presetScaleOptions.put(meta.getDisplayName(), targetScale);
        }
        return stack;
    }

    private ItemStack createScaleIncreaseIcon(ItemStack icon, double step, boolean coarse) {
        String formattedValue = formatAdjustmentValue(step, true, coarse);
        ItemStack stack = createIcon(icon, "scaleincrease", formattedValue);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            increaseScaleOptions.put(meta.getDisplayName(), step);
        }
        return stack;
    }

    private ItemStack createScaleDecreaseIcon(ItemStack icon, double step, boolean coarse) {
        String formattedValue = formatAdjustmentValue(step, false, coarse);
        ItemStack stack = createIcon(icon, "scaledecrease", formattedValue);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            decreaseScaleOptions.put(meta.getDisplayName(), step);
        }
        return stack;
    }

    public void handleAttributeScaling(String itemName, Player player) {
        if (itemName == null || player == null) return;

        if (presetScaleOptions.containsKey(itemName)) {
            handleAbsoluteScaleChange(player, presetScaleOptions.get(itemName));
        } else if (increaseScaleOptions.containsKey(itemName)) {
            handleRelativeScaleChange(player, increaseScaleOptions.get(itemName));
        } else if (decreaseScaleOptions.containsKey(itemName)) {
            handleRelativeScaleChange(player, -decreaseScaleOptions.get(itemName));
        } else if (itemName.equals(backToMenuDisplayName)) {
            handleBackToMenu(player);
        } else if (itemName.equals(resetDisplayName)) {
            handleReset(player);
        }
    }

    private void handleAbsoluteScaleChange(Player player, double absoluteValue) {
        if (applyScale(player, absoluteValue, true)) {
            playChimeSound(player);
            player.closeInventory();
        }
    }

    private void handleRelativeScaleChange(Player player, double delta) {
        if (applyScale(player, delta, false)) {
            playChimeSound(player);
            player.closeInventory();
        }
    }

    private void handleBackToMenu(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
        player.closeInventory();
        pe.openMenu();
    }

    private void handleReset(Player player) {
        if (applyScale(player, 1.0, true)) {
            playChimeSound(player);
            player.closeInventory();
        }
    }

    private void playChimeSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
    }

    private boolean applyScale(Player player, double value, boolean absolute) {
        if (!as.isValid() || !player.hasPermission("asedit.togglesize")) {
            return false;
        }

        double minScale = plugin.getMinScaleValue();
        double maxScale = plugin.getMaxScaleValue();

        double currentScale = as.getAttribute(Attribute.SCALE).getBaseValue();
        double newScale = absolute ? value : currentScale + value;
        double roundedScale = roundScaleValue(newScale);

        if (roundedScale > maxScale + MINIMUM_STEP) {
            pe.getPlayer().sendMessage(plugin.getLang().getMessage("scalemaxwarn", "warn"));
            return false;
        }

        if (roundedScale < minScale - MINIMUM_STEP) {
            pe.getPlayer().sendMessage(plugin.getLang().getMessage("scaleminwarn", "warn"));
            return false;
        }

        roundedScale = Math.min(maxScale, Math.max(minScale, roundedScale));
        debug.log("Result of the scale calculation: " + roundedScale);
        as.getAttribute(Attribute.SCALE).setBaseValue(roundedScale);
        return true;
    }

    public void openMenu() {
        if (pe.getPlayer().hasPermission("asedit.togglesize")) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getDisplayName() + "' has opened the Sizing Attribute Menu");
            pe.getPlayer().openInventory(menuInv);
        }
    }

    private ItemStack createIcon(ItemStack icon, String path) {
        return createIcon(icon, path, null);
    }

    private ItemStack createIcon(ItemStack icon, String path, String option) {
        ItemMeta meta = icon.getItemMeta();
        assert meta != null;
        meta.getPersistentDataContainer().set(ArmorStandEditorPlugin.instance().getIconKey(), PersistentDataType.STRING, "ase " + option);
        meta.setDisplayName(getIconName(path, option));
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(getIconDescription(path, option));
        meta.setLore(loreList);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private String getIconName(String path, String option) {
        return pe.plugin.getLang().getMessage(path, "iconname", option);
    }

    private String getIconDescription(String path, String option) {
        return pe.plugin.getLang().getMessage(path + ".description", "icondescription", option);
    }

    private double[] generatePresetValues(double minScale, double maxScale) {
        if (maxScale <= minScale) {
            return new double[]{roundScaleValue(minScale)};
        }

        double[] values = new double[PRESET_COUNT];
        double step = (maxScale - minScale) / (PRESET_COUNT - 1);

        for (int i = 0; i < PRESET_COUNT; i++) {
            double value = minScale + (step * i);
            if (i == PRESET_COUNT - 1) {
                value = maxScale;
            }
            values[i] = roundScaleValue(value);
        }

        return values;
    }

    private double calculateCoarseStep(double minScale, double maxScale) {
        double range = Math.max(maxScale - minScale, MINIMUM_STEP);
        double step = range / COARSE_STEP_DIVISOR;
        return roundScaleValue(Math.max(step, MINIMUM_STEP));
    }

    private double calculateFineStep(double coarseStep) {
        return roundScaleValue(Math.max(coarseStep / FINE_STEP_DIVISOR, MINIMUM_STEP));
    }

    private double roundScaleValue(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private String formatScaleValue(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String formatAdjustmentValue(double step, boolean increase, boolean coarse) {
        String prefix = increase ? "+" : "-";
        String modifier = coarse ? "Coarse" : "Fine";
        return prefix + formatScaleValue(step) + " (" + modifier + ")";
    }

    private ItemStack getDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null ? meta.getDisplayName() : "";
    }
}
