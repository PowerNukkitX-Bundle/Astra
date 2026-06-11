package org.powernukkitx.anticheat.player;

import cn.nukkit.Player;
import cn.nukkit.PlayerHandle;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.block.customblock.CustomBlock;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySpawnable;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.inventory.HumanInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.powernukkitx.anticheat.AntiCheatPlugin;

/**
 * @author Kaooot
 */
public class AntiCheatPlayer {

    @Getter
    private final Player serverPlayer;
    private final AntiCheatPlugin plugin;

    private final Map<String, PlayerTimeStats> timeStatistics =
        new Object2ObjectOpenHashMap<>();
    private final Map<BlockFace, LevelEvent> blockFaceMap = new Object2ObjectOpenHashMap<>();

    private PlayerHandle playerHandle;
    private PlayerBlockActionData lastPlayerBlockAction;
    private Block intendedToBreakBlock;
    private BlockFace intendedToBreakBlockFace;
    private int clientStartedBreakTick;
    private int lastClientPredictedBreakTick;
    private int destroyTicks;

    public AntiCheatPlayer(Player serverPlayer, AntiCheatPlugin plugin) {
        this.serverPlayer = serverPlayer;
        this.plugin = plugin;
        for (final BlockFace value : BlockFace.values()) {
            this.blockFaceMap.put(value, switch (value) {
                case DOWN -> LevelEvent.PARTICLE_BREAK_BLOCK_DOWN;
                case UP -> LevelEvent.PARTICLE_BREAK_BLOCK_UP;
                case NORTH -> LevelEvent.PARTICLE_BREAK_BLOCK_NORTH;
                case SOUTH -> LevelEvent.PARTICLE_BREAK_BLOCK_SOUTH;
                case EAST -> LevelEvent.PARTICLE_BREAK_BLOCK_EAST;
                case WEST -> LevelEvent.PARTICLE_BREAK_BLOCK_WEST;
            });
        }
        this.plugin.getServer().getScheduler().scheduleRepeatingTask(() -> {
            this.tickBlockBreaking();
        }, 1);
    }

    public Vector3f getPosition() {
        return this.serverPlayer.getPosition().toNetwork();
    }

    public Level getLevel() {
        return this.serverPlayer.getLevel();
    }

    public String getName() {
        return this.serverPlayer.getName();
    }

    public UUID getUniqueId() {
        return this.serverPlayer.getUniqueId();
    }

    public void updateTimeStatistic(String identifier, long serverTick) {
        this.updateTimeStatistic(identifier, -1, serverTick);
    }

    public void updateTimeStatistic(String identifier, long clientFrame, long serverTick) {
        this.updateTimeStatistic(identifier, System.currentTimeMillis(), clientFrame,
            serverTick);
    }

    public void updateTimeStatistic(String identifier, long timeInMS, long clientFrame,
                                    long serverTick) {
        final PlayerTimeStats stats = this.timeStatistics.computeIfAbsent(identifier, s ->
            new PlayerTimeStats(timeInMS, clientFrame, serverTick));
        stats.setTimeInMS(timeInMS);
        stats.setClientFrame(clientFrame);
        stats.setServerTick(serverTick);
    }

    public PlayerTimeStats getTimeStatistic(String identifier) {
        return this.timeStatistics.computeIfAbsent(identifier,
            s -> new PlayerTimeStats(0L, 0L, 0L));
    }

    public void processBlockBreak(PlayerAuthInputPacket packet) {
        for (final PlayerBlockActionData playerAction : packet.getPlayerActions()) {
            this.handleBlockBreak(playerAction);
        }
    }

    private void handleBlockBreak(PlayerBlockActionData data) {
        System.out.println(data);
        // ignore sword block breaking actions when in creative
        if (this.serverPlayer.getInventory().getItemInMainHand().isSword() &&
            this.serverPlayer.isCreative()) {
            return;
        }
        this.handleBlockIntentChange(data.getBlockPosition(), data.getFace());
        switch (data.getAction()) {
            case START_DESTROY_BLOCK -> this.startDestroyBlock(data);
            case CONTINUE_DESTROY_BLOCK -> this.continueDestroyBlock(data);
            case PREDICT_DESTROY_BLOCK -> this.predictDestroyBlock(data);
            case ABORT_DESTROY_BLOCK -> this.abortDestroyBlock(data);
        }
        this.lastPlayerBlockAction = data;
    }

    private void startDestroyBlock(PlayerBlockActionData data) {
        this.startDestroyBlock(data.getBlockPosition(), data.getFace());
    }

    private void startDestroyBlock(Vector3i blockPos, int face) {
        final BlockFace blockFace = BlockFace.fromIndex(face);
        if (this.hasBlockBreakIntent()) {
            return;
        }

        // PlayerInteractEvent call to update the previous interact tick (internal), check if the
        // interaction should be allowed and correct the client if that's not the case
        final Vector3 pos = Vector3.fromNetwork(blockPos.toFloat());
        final Block block = this.getLevel().getBlock(pos);
        final HumanInventory inventory = this.serverPlayer.getInventory();
        final Item itemInHand = inventory.getItemInMainHand();
        final PlayerInteractEvent playerInteractEvent = new PlayerInteractEvent(
            this.serverPlayer,
            itemInHand,
            block,
            blockFace,
            block.isAir() ? PlayerInteractEvent.Action.LEFT_CLICK_AIR :
                PlayerInteractEvent.Action.LEFT_CLICK_BLOCK
        );
        this.serverPlayer.getServer().getPluginManager().callEvent(playerInteractEvent);
        this.getPlayerHandle().setInteract();
        if (playerInteractEvent.isCancelled()) {
            this.sendDestroyCorrection(block);
            return;
        }

        block.onTouch(pos, itemInHand, blockFace, 0, 0, 0, this.serverPlayer,
            playerInteractEvent.getAction());

        // used to handle fire extinguishing which is instantaneous
        final Block facedBlock = block.getSide(blockFace);
        if (facedBlock.getId().equals(Block.FIRE) ||
            facedBlock.getId().equals(BlockID.SOUL_FIRE)) {
            this.getLevel().setBlock(block, Block.get(BlockID.AIR), true);
            this.getLevel().addLevelSoundEvent(block, SoundEvent.EXTINGUISH_FIRE);
            return;
        }

        // handle sweet berry bush
        if (block.getId().equals(BlockID.SWEET_BERRY_BUSH) && block.isDefaultState()) {
            final Item oldItem = playerInteractEvent.getItem();
            final Item i = this.getLevel().useBreakOn(block, oldItem, this.serverPlayer, true);
            if (this.serverPlayer.isSurvival() || this.serverPlayer.isAdventure()) {
                this.serverPlayer.getFoodData().exhaust(0.005);
                if (!i.equals(oldItem) || i.getCount() != oldItem.getCount()) {
                    inventory.setItemInMainHand(i);
                    inventory.sendHeldItem(this.serverPlayer.getViewers().values());
                }
            }
            return;
        }

        // whether block changes are allowed
        final boolean isBlockChangeAllowed = block.isBlockChangeAllowed(this.serverPlayer);
        if (!isBlockChangeAllowed) {
            return;
        }

        // particle display eligibility
        if (this.serverPlayer.isSurvival() || this.serverPlayer.isAdventure()) {
            final int breakTime = this.getBreakTimeTicks(pos);
            // send the break particles and update the anti xray obfuscation
            if (breakTime > 0) {
                this.sendBreakParticles(LevelEvent.BLOCK_START_BREAK, breakTime, blockPos);

                if (this.getLevel().isAntiXrayEnabled() &&
                    this.getLevel().getAntiXraySystem().isPreDeObfuscate()) {
                    this.getLevel().getAntiXraySystem().deObfuscateBlock(
                        this.serverPlayer,
                        blockFace,
                        block
                    );
                }
            }
        }
        this.destroyTicks = 0;
        this.intendedToBreakBlock = block;
        this.intendedToBreakBlockFace = blockFace;
        this.clientStartedBreakTick = this.plugin.getServer().getTick();
    }

    /**
     * Handles a PlayerActionType::ContinueDestroyBlock action which is interpreted as a new start
     * destroy block action when the client breaks blocks consecutively.
     *
     * @param data the {@link org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData}
     */
    private void continueDestroyBlock(PlayerBlockActionData data) {
        if (this.lastPlayerBlockAction != null &&
            this.lastPlayerBlockAction.getAction()
                .equals(PlayerActionType.PREDICT_DESTROY_BLOCK)) {
            //  this.resetBlockDestructionData();
            this.startDestroyBlock(data);
            this.clientStartedBreakTick = this.lastClientPredictedBreakTick;
            this.destroyTicks = this.plugin.getServer().getTick() -
                this.lastClientPredictedBreakTick;
        }
    }

    private void tickBlockBreaking() {
        if (!this.hasBlockBreakIntent()) {
            return;
        }
        this.destroyTicks++;
        final Vector3 blockPos = this.intendedToBreakBlock.getVector3();
        final int breakTime = this.getBreakTimeTicks(blockPos);
        if (breakTime > 0) {
            this.sendBreakParticles(
                LevelEvent.BLOCK_UPDATE_BREAK,
                breakTime,
                blockPos.asBlockVector3().toNetwork()
            );
            this.sendCrackParticles(
                this.blockFaceMap.get(this.intendedToBreakBlockFace),
                this.intendedToBreakBlock
            );
        }
        if (this.destroyTicks >= breakTime) {
            this.breakBlock(
                this.intendedToBreakBlock.asBlockVector3(), this.intendedToBreakBlockFace
            );
        }
    }

    /**
     * Handles a client block destruction prediction and validates it against the server predicted
     * break time
     */
    private void predictDestroyBlock(PlayerBlockActionData data) {
        this.lastClientPredictedBreakTick = this.plugin.getServer().getTick();
        final Vector3i blockPos = data.getBlockPosition();
        final Vector3 pos = Vector3.fromNetwork(blockPos.toFloat());
        if (this.intendedToBreakBlock == null) {
            this.sendDestroyCorrection(this.getLevel().getBlock(pos));
            return;
        }
        this.sendAbortBreakParticles(blockPos);
        final int serverPredictedBreakTime = this.getBreakTimeTicks(pos);
        if (this.destroyTicks < serverPredictedBreakTime) {
            this.sendDestroyCorrection(this.getLevel().getBlock(pos));
            return;
        }
        this.breakBlock(pos.asBlockVector3(), BlockFace.fromIndex(data.getFace()));
    }

    private void breakBlock(BlockVector3 blockPos, BlockFace face) {
        if (this.intendedToBreakBlock == null || (
            blockPos.getX() != this.intendedToBreakBlock.getFloorX() ||
                blockPos.getY() != this.intendedToBreakBlock.getFloorY() ||
                blockPos.getZ() != this.intendedToBreakBlock.getFloorZ())) {
            this.sendDestroyCorrection(this.getLevel().getBlock(blockPos.asVector3()));
            return;
        }
        if (!this.serverPlayer.spawned || !this.serverPlayer.isAlive()) {
            return;
        }

        final HumanInventory inventory = this.serverPlayer.getInventory();
        Item handItem = inventory.getItemInMainHand();
        final Item clone = handItem.clone();
        final Block block = this.getLevel().getBlock(blockPos.asVector3());
        final boolean canInteract = this.serverPlayer.canInteract(blockPos.add(0.5, 0.5, 0.5),
            this.serverPlayer.isCreative() ? 13 : 7);
        if (canInteract) {
            handItem = this.getLevel().useBreakOn(
                blockPos.asVector3(),
                face,
                handItem,
                this.serverPlayer,
                true
            );
            if (handItem != null && this.serverPlayer.isSurvival()) {
                this.serverPlayer.getFoodData().exhaust(0.005);
                if (handItem.equals(clone) && handItem.getCount() == clone.getCount()) {
                    this.resetBlockDestructionData();
                    return;
                }

                if (Objects.equals(clone.getId(), handItem.getId()) || handItem.isNull()) {
                    inventory.setItemInMainHand(handItem, false);
                } else {
                    this.plugin.getLogger().debug(
                        "Tried to set item " + handItem.getId() + " but " + this.getName() +
                            " had item " + clone.getId() + " in their hand slot"
                    );
                }
                inventory.sendHeldItem(this.serverPlayer.getViewers().values());
            } else if (handItem == null) {
                this.getLevel().sendBlocks(new Player[]{this.serverPlayer},
                    new Block[]{this.getLevel().getBlock(blockPos.asVector3())},
                    UpdateBlockPacket.FLAG_ALL_PRIORITY, 0);
            }
            this.resetBlockDestructionData();
            return;
        }

        inventory.sendContents(this.serverPlayer);
        inventory.sendHeldItem(this.serverPlayer);

        if (blockPos.distanceSquared(this.serverPlayer) < 100) {
            final Block target = this.getLevel().getBlock(blockPos.asVector3());
            this.getLevel().sendBlocks(new Player[]{this.serverPlayer}, new Block[]{target},
                UpdateBlockPacket.FLAG_ALL_PRIORITY);

            final BlockEntity blockEntity =
                this.getLevel().getBlockEntity(blockPos.asVector3());
            if (blockEntity instanceof BlockEntitySpawnable) {
                ((BlockEntitySpawnable) blockEntity).spawnTo(this.serverPlayer);
            }
        }
        this.sendDestroyCorrection(block);
    }

    private void abortDestroyBlock(PlayerBlockActionData data) {
        this.abortDestroyBlock(data.getBlockPosition());
    }

    private void abortDestroyBlock(Vector3i blockPos) {
        this.sendAbortBreakParticles(blockPos);
        this.resetBlockDestructionData();
    }

    private void resetBlockDestructionData() {
        this.intendedToBreakBlock = null;
        this.intendedToBreakBlockFace = null;
        this.destroyTicks = 0;
    }

    /**
     * Handles the block break intent change when the targeted position changes and determines
     * whether the old block should break or not. The breaking action on the previous block is
     * aborted and the action will be moved to the block that's currently targeted by the client.
     *
     * @param blockPos the block position which is targeted
     * @param face     the relevant block face
     */
    private void handleBlockIntentChange(Vector3i blockPos, int face) {
        final PlayerBlockActionData lastAction = this.lastPlayerBlockAction;
        final BlockVector3 lastBreakPos = lastAction == null ? null :
            BlockVector3.fromNetwork(lastAction.getBlockPosition());
        if (lastBreakPos != null && (lastBreakPos.getX() != blockPos.getX() ||
            lastBreakPos.getY() != blockPos.getY() || lastBreakPos.getZ() != blockPos.getZ())) {
            final int breakTime = this.getBreakTimeTicks(lastBreakPos.asVector3());
            final boolean canBreak = breakTime -
                (this.plugin.getServer().getTick() - this.clientStartedBreakTick) <= 1;
            if (canBreak &&
                lastAction.getAction().equals(PlayerActionType.START_DESTROY_BLOCK)) {
                this.breakBlock(
                    lastBreakPos,
                    BlockFace.fromIndex(lastAction.getFace())
                );
            }
            this.abortDestroyBlock(lastBreakPos.toNetwork());
            this.startDestroyBlock(blockPos, face);
        }
    }

    private float getBreakTime(Vector3 pos) {
        // creative mode is still client authoritative
        if (this.serverPlayer.isCreative()) {
            return 0f;
        }
        final Block block = this.getLevel().getBlock(pos);
        final Item item = this.serverPlayer.getInventory().getItemInMainHand();
        return (float) (block instanceof CustomBlock customBlock ?
            customBlock.breakTime(item, this.serverPlayer) :
            block.calculateBreakTime(item, this.serverPlayer));
    }

    private int getBreakTimeTicks(Vector3 pos) {
        return (int) Math.floor(this.getBreakTime(pos) * 20);
    }

    private PlayerHandle getPlayerHandle() {
        if (this.playerHandle != null) {
            return this.playerHandle;
        }
        try {
            final Field field = this.serverPlayer.getClass().getDeclaredField("playerHandle");
            field.setAccessible(true);
            this.playerHandle = (PlayerHandle) field.get(this.serverPlayer);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return this.playerHandle;
    }

    private void sendDestroyCorrection(Block block) {
        this.abortDestroyBlock(block.asBlockVector3().toNetwork());
        this.serverPlayer.getInventory().sendHeldItem(this.serverPlayer);
        this.getLevel().sendBlocks(
            new Player[]{this.serverPlayer},
            new Block[]{block},
            UpdateBlockPacket.FLAG_ALL_PRIORITY,
            0
        );
        if (block.getLevelBlockAtLayer(1) instanceof BlockLiquid) {
            this.getLevel().sendBlocks(
                new Player[]{this.serverPlayer},
                new Block[]{block.getLevelBlockAtLayer(1)},
                UpdateBlockPacket.FLAG_ALL_PRIORITY,
                1
            );
        }
    }

    private boolean hasBlockBreakIntent() {
        return this.intendedToBreakBlock != null;
    }

    private void sendBreakParticles(LevelEvent type, int breakTime, Vector3i blockPos) {
        final LevelEventPacket packet = new LevelEventPacket();
        packet.setType(type);
        packet.setPosition(blockPos.toFloat());
        packet.setData(65535 / breakTime);

        this.getLevel().addChunkPacket(blockPos.getX() >> 4, blockPos.getZ() >> 4, packet);
    }

    private void sendCrackParticles(LevelEvent type, Block block) {
        final LevelEventPacket packet = new LevelEventPacket();
        packet.setType(type);
        packet.setPosition(block.asBlockVector3().toNetwork().toFloat());
        packet.setData(block.getBlockState().blockStateHash());

        this.getLevel().addChunkPacket(block.getChunkX(), block.getChunkZ(), packet);
    }

    private void sendAbortBreakParticles(Vector3i blockPos) {
        final LevelEventPacket packet = new LevelEventPacket();
        packet.setType(LevelEvent.BLOCK_STOP_BREAK);
        packet.setPosition(blockPos.toFloat());

        this.getLevel().addChunkPacket(blockPos.getX() >> 4, blockPos.getZ() >> 4, packet);
    }
}