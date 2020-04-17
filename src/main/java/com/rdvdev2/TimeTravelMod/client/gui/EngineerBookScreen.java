package com.rdvdev2.TimeTravelMod.client.gui;

import com.rdvdev2.TimeTravelMod.ModItems;
import com.rdvdev2.TimeTravelMod.ModRegistries;
import com.rdvdev2.TimeTravelMod.ModTimeMachines;
import com.rdvdev2.TimeTravelMod.api.timemachine.upgrade.TimeMachineUpgrade;
import com.rdvdev2.TimeTravelMod.common.timemachine.CreativeTimeMachine;
import com.rdvdev2.TimeTravelMod.common.timemachine.TimeMachine;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractPressableButtonWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class EngineerBookScreen extends Screen {

    private int tickCount;

    @Override
    public void tick() {
        tickCount++;
        if (tickCount >= 40) tickCount = 0;
    }

    private ArrayList<TimeMachineData> timeMachineData;
    private DocsPanel panel;
    private ItemRenderer itemRenderer;
    private static HashMap<Integer, Integer> yLevels = new HashMap<>();

    public EngineerBookScreen(Iterator<com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine> timeMachines) {
        super(new LiteralText(""));

        timeMachineData = new ArrayList<>();
        
        int i = 0;
        while (timeMachines.hasNext()) {
            com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine tm = timeMachines.next();
            TimeMachineData d = new TimeMachineData();
            d.id = i++;
            d.name = tm.getName();
            d.description = tm.getDescription();
            d.tier = tm.getTier();
            d.cooldown = tm.getCooldownTime() / 20;
            if (tm instanceof CreativeTimeMachine) {
                d.tier++; // Ensure it's the last one in the list
                timeMachineData.add(d);
                continue;
            }
            d.basicBlocksPos = tm.getBasicBlocksPos(Direction.SOUTH);
            d.basicBlocks = tm.getBasicBlocks();
            d.coreBlocksPos = tm.getCoreBlocksPos(Direction.SOUTH);
            d.coreBlocks = tm.getCoreBlocks();
            d.controllerBlockPos = new BlockPos(0, 0, 0);
            d.controllerBlocks = tm.getControllerBlocks();
            d.upgrades = tm.getCompatibleUpgrades();
            d.generateBoundingBox(); // For the relocation method
            d.relocateBlocks(); // Relocate blocks
            d.generateBoundingBox(); // Regenerate for the blockTypeMap generator
            d.generateBlockTypeMap(); // Generate the blockTypeMap
            timeMachineData.add(d);
        }
        Collections.sort(timeMachineData);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init(MinecraftClient minecraft, int width, int height) {
        super.init(minecraft, width, height);
        this.itemRenderer = this.minecraft.getItemRenderer();
        this.panel = new DocsPanel(this.minecraft, this.width, this.height, 0, 0);
        this.children.add(0, this.panel);
    }

    @Override
    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        if (this.panel != null) {
            this.panel.render(p_render_1_, p_render_2_, p_render_3_);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        yLevels.clear();
    }

    private static class TimeMachineData implements Comparable<TimeMachineData> {

        public int id;
        public TranslatableText name;
        public TranslatableText description;
        public int tier;
        public int cooldown; // in seconds
        public List<BlockPos> basicBlocksPos;
        public BlockState[] basicBlocks;
        public List<BlockPos> coreBlocksPos;
        public BlockState[] coreBlocks;
        public BlockPos controllerBlockPos;
        public BlockState[] controllerBlocks;
        public TimeMachineUpgrade[] upgrades;

        public TimeMachine.TimeMachineComponentType[][][] blockTypeMap;
        public Box boundingBox;

        public void relocateBlocks() {
            BlockPos translocation = new BlockPos(0 - boundingBox.x1, 0 - boundingBox.y1, 0 - boundingBox.z1);
            for (int i = 0; i < basicBlocksPos.size(); i++) basicBlocksPos.set(i, basicBlocksPos.get(i).add(translocation));
            for (int i = 0; i < coreBlocksPos.size(); i++) coreBlocksPos.set(i, coreBlocksPos.get(i).add(translocation));
            controllerBlockPos = controllerBlockPos.add(translocation);
        }

        public void generateBlockTypeMap() {
            blockTypeMap = new TimeMachine.TimeMachineComponentType[(int)boundingBox.y2+1][(int)boundingBox.x2+1][(int)boundingBox.z2+1];
            for(int y = 0; y <= boundingBox.y2; y++)
                for(int x = 0; x <= boundingBox.x2; x++)
                    nextPos: for(int z = 0; z <= boundingBox.z2; z++) {
                        for(BlockPos pos: basicBlocksPos) if (pos.equals(new BlockPos(x, y, z))) {
                            blockTypeMap[y][x][z] = TimeMachine.TimeMachineComponentType.BASIC; continue nextPos;
                        }
                        for(BlockPos pos: coreBlocksPos) if (pos.equals(new BlockPos(x, y, z))) {
                            blockTypeMap[y][x][z] = TimeMachine.TimeMachineComponentType.CORE; continue nextPos;
                        }
                        if (controllerBlockPos.equals(new BlockPos(x, y, z))) {
                            blockTypeMap[y][x][z] = TimeMachine.TimeMachineComponentType.CONTROLPANEL;
                        } else blockTypeMap[y][x][z] = TimeMachine.TimeMachineComponentType.AIR;
                    }
        }

        public void generateBoundingBox() {
            int minX = 100, minY = 100, minZ = 100;
            int maxX = -100, maxY = -100, maxZ = -100;

            List<BlockPos> allPos = new ArrayList<>(basicBlocksPos.size()+coreBlocksPos.size()+1);
            allPos.addAll(basicBlocksPos);
            allPos.addAll(coreBlocksPos);
            allPos.add(controllerBlockPos);

            for(BlockPos pos: allPos) {
                if (pos.getX() < minX) minX = pos.getX(); else if (pos.getX() > maxX) maxX = pos.getX();
                if (pos.getY() < minY) minY = pos.getY(); else if (pos.getY() > maxY) maxY = pos.getY();
                if (pos.getZ() < minZ) minZ = pos.getZ(); else if (pos.getZ() > maxZ) maxZ = pos.getZ();
            }

            boundingBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        @Override
        public int compareTo(TimeMachineData o) {
            return Integer.compare(this.tier, o.tier);
        }
    }

    class DocsPanel extends ScrollPanel {

        private int previousTick = tickCount;
        private int contentHeight = 0;

        public DocsPanel(MinecraftClient client, int width, int height, int top, int left) {
            super(client, width, height, top, left);
        }

        @Override
        protected int getScrollAmount() {
            return font.fontHeight * 3;
        }

        @Override
        protected int getContentHeight() {
            return Math.max(contentHeight, height);
        }

        @Override
        protected void drawPanel(int entryRight, int relativeY, Tessellator tess, int mouseX, int mouseY) {
            buttons.clear();
            int padding = 4;
            int right = this.left + this.width - 6;
            right -= 2;
            int relativeYdiff = 0 - relativeY;
            relativeY += padding;
            relativeY += drawCenteredString(ModItems.ENGINEER_BOOK.getName().asString(), width / 2, relativeY, 0xFFD900);
            relativeY += 2;
            relativeY += drawSplitString(new TranslatableText("gui.tmengineerbook.introduction").asString(),left + padding, relativeY, (right - padding) - left, 0xFFFFFF);
            relativeY += 8;
            relativeY += drawCenteredString(new TranslatableText("gui.tmengineerbook.tms").asString(), width / 2, relativeY, 0xFFD900);
            relativeY += 2;
            relativeY += drawSplitString(new TranslatableText("gui.tmengineerbook.tmsintroduction").asString(),left + padding, relativeY, (right - padding) - left, 0xFFFFFF);
            relativeY += 2;
            for(TimeMachineData data: timeMachineData) {
                int tier;
                boolean hasBuilding;
                if (data.name.getKey().equals(ModTimeMachines.CREATIVE.getName().getKey())) {
                    tier = data.tier - 1;
                    hasBuilding = false;
                } else {
                    tier = data.tier;
                    hasBuilding = true;
                }
                relativeY += drawString(data.name.setStyle(new Style().setBold(true)).asFormattedString(), left + padding, relativeY, 0xFFFFFF);
                relativeY += drawString(new TranslatableText("gui.tmengineerbook.tmstats", tier, data.cooldown).asString(), left + padding, relativeY, 0xC98300);
                relativeY += 2;
                relativeY += drawSplitString(data.description.asFormattedString(), left + padding, relativeY, (right - padding) - left, 0xFFFFFF);
                if (hasBuilding) {
                    relativeY += 4;
                    relativeY += drawString(new TranslatableText("gui.tmengineerbook.howto").setStyle(new Style().setUnderline(true)).asFormattedString(), left + padding, relativeY, 0xFFFFFF);
                    relativeY += 2;
                    drawString(new TranslatableText("gui.tmengineerbook.layer", yLevels.getOrDefault(data.id, 0)).asString(), left + padding, relativeY + 6, 0xFFFFFF);
                    int _width = font.getStringWidth(new TranslatableText("gui.tmengineerbook.layer", yLevels.getOrDefault(data.id, 0)).asString());
                    addButton(new yNavButton(left + padding + _width + padding, relativeY, data.id, 0, ((int) data.boundingBox.y2)));
                    addButton(new yNavButton(left + padding + _width + padding + 20 + padding, relativeY, data.id, 1, ((int) data.boundingBox.y2)));
                    buttons.forEach(b -> {
                        if (b instanceof yNavButton && ((yNavButton) b).id == data.id) {
                            b.render(mouseX, mouseY, 0);
                        }
                    });
                    relativeY += 20;
                    relativeY += 2;
                    TimeMachine.TimeMachineComponentType[][] layer = data.blockTypeMap[yLevels.get(data.id)];
                    for (int z = 0; z <= data.boundingBox.z2; z++) {
                        int drawY = z * 22;
                        for (int x = 0; x <= data.boundingBox.x2; x++) {
                            int drawX = x * 22;
                            ItemStack drawItem = getDrawItem(data, layer, z, x);
                            itemRenderer.renderGuiItem(drawItem, left + padding + drawX, relativeY + drawY);
                        }
                    }
                    for (int z = 0; z <= data.boundingBox.z2; z++) {
                        int drawY = z * 22;
                        for (int x = 0; x <= data.boundingBox.x2; x++) {
                            int drawX = x * 22;
                            if (mouseX >= left + padding + drawX && mouseX <= left + padding + drawX + 20 && mouseY >= relativeY + drawY && mouseY <= relativeY + drawY + 20) {
                                ItemStack drawItem = getDrawItem(data, layer, z, x);
                                renderComponentHoverEffect(drawItem.toHoverableText(), mouseX, mouseY);
                            }
                        }
                    }
                    relativeY += data.boundingBox.z2 * 22 + 20;
                }
                if (data.upgrades != null && data.upgrades.length != 0) {
                    relativeY += 2;
                    relativeY += drawString(new TranslatableText("gui.tmengineerbook.compatibleupgrades").setStyle(new Style().setUnderline(true)).asFormattedString(), left + padding, relativeY, 0xFFFFFF);
                    for (TimeMachineUpgrade upgrade : data.upgrades) {
                        relativeY += 2;
                        relativeY += drawString(upgrade.getName().asFormattedString(), left + padding, relativeY,0xFFFFFF);
                    }
                }
                relativeY += 8;
            }
            relativeY += drawCenteredString(new TranslatableText("gui.tmengineerbook.upgrades").asString(), width / 2, relativeY, 0xFFD900);
            relativeY += 2;
            Collection<TimeMachineUpgrade> upgrades = ModRegistries.UPGRADES.stream().collect(Collectors.toCollection(ArrayList::new));
            if (upgrades.isEmpty()) {
                relativeY += drawString(new TranslatableText("gui.tmengineerbook.noupgrades").asString(), left + padding, relativeY, 0xFFFFFF);
                relativeY += 2;
            } else for (TimeMachineUpgrade upgrade : upgrades) {
                relativeY += drawString(upgrade.getName().setStyle(new Style().setBold(true)).asFormattedString(), left + padding, relativeY, 0xFFFFFF);
                relativeY += 2;
                relativeY += drawSplitString(upgrade.getDescription().asFormattedString(), left + padding, relativeY, (right - padding) - left, 0xFFFFFF);
                relativeY += 2;
                if (upgrade.getCompatibleTMs() != null && upgrade.getCompatibleTMs().length != 0) {
                    relativeY += drawString(new TranslatableText("gui.tmengineerbook.compatibletms").setStyle(new Style().setUnderline(true)).asFormattedString(), left + padding, relativeY, 0xFFFFFF);
                    for (com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine tm : upgrade.getCompatibleTMs()) {
                        relativeY += 2;
                        relativeY += drawString(tm.getName().asFormattedString(), left + padding, relativeY,0xFFFFFF);
                    }
                    relativeY += 2;
                }
            }
            relativeY += 6;

            contentHeight = relativeY + relativeYdiff;
        }

        private HashMap<Integer, Integer> currentBasicBlock = new HashMap<>(timeMachineData.size()-1);
        private HashMap<Integer, Integer> curentCoreBlock = new HashMap<>(timeMachineData.size()-1);
        private HashMap<Integer, Integer> currentControlPanelBlock = new HashMap<>(timeMachineData.size()-1);

        private ItemStack getDrawItem(TimeMachineData data, TimeMachine.TimeMachineComponentType[][] layer, int z, int x) {
            int index;
            switch (layer[x][z]) {
                case BASIC:
                    if (tickCount < previousTick || !currentBasicBlock.containsKey(data.id)) {
                        index = currentBasicBlock.getOrDefault(data.id, -1) + 1;
                        if (index >= data.basicBlocks.length) index = 0;
                        currentBasicBlock.put(data.id, index);
                    } else {
                        index = currentBasicBlock.get(data.id);
                    }
                    return new ItemStack(data.basicBlocks[index].getBlock());
                case CORE:
                    if (tickCount < previousTick || !curentCoreBlock.containsKey(data.id)) {
                        index = curentCoreBlock.getOrDefault(data.id, -1) + 1;
                        if (index >= data.coreBlocks.length) index = 0;
                        curentCoreBlock.put(data.id, index);
                    } else {
                        index = curentCoreBlock.get(data.id);
                    }
                    return new ItemStack(data.coreBlocks[index].getBlock());
                case CONTROLPANEL:
                    if (tickCount < previousTick || !currentControlPanelBlock.containsKey(data.id)) {
                        index = currentControlPanelBlock.getOrDefault(data.id, -1) + 1;
                        if (index >= data.controllerBlocks.length) index = 0;
                        currentControlPanelBlock.put(data.id, index);
                    } else {
                        index = currentControlPanelBlock.get(data.id);
                    }
                    return new ItemStack(data.controllerBlocks[index].getBlock());
                default:
                case AIR:
                    return ItemStack.EMPTY;
            }
        }

        private int drawCenteredString(String text, int x, int y, int color) {
            super.drawCenteredString(font, text, x, y, color);
            return font.fontHeight;
        }

        private int drawSplitString(String text, int x, int y, int width, int color) {
            font.drawTrimmed(text, x, y, width, color);
            return font.wrapStringToWidthAsList(text, width).size() * 9;
        }

        public int drawString(String text, int x, int y, int color) {
            drawString(font, text, x, y, color);
            return font.fontHeight;
        }

        class yNavButton extends AbstractPressableButtonWidget {

            private final int mode;
            private final int id;
            private final int maxY;

            public yNavButton(int x, int y, int id, int mode, int maxY) { // mode 0 -> decrease, 1 -> increase
                super(x, y, 20, 20, mode == 0 ? "-" : "+");
                this.id = id;
                this.mode = mode;
                this.maxY = maxY;
                yLevels.putIfAbsent(id, 0);
                this.active = (mode != 0 || yLevels.get(id) != 0) && (mode != 1 || yLevels.get(id) != maxY);
            }

            @Override
            public void onPress() {
                if((mode == 0 && yLevels.get(id) == 0) || (mode == 1 && yLevels.get(id) == maxY)) return;
                int yValue = yLevels.get(id);
                if (mode == 0) {
                    yValue--;
                } else if (mode == 1) {
                    yValue++;
                }
                yLevels.put(id, yValue);
            }
        }
    }
}