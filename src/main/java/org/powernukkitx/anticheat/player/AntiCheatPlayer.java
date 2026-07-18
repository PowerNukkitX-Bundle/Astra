package org.powernukkitx.anticheat.player;

import org.powernukkitx.Player;
import org.powernukkitx.PlayerHandle;
import org.powernukkitx.block.Block;
import org.powernukkitx.block.BlockID;
import org.powernukkitx.block.BlockLiquid;
import org.powernukkitx.block.customblock.CustomBlock;
import org.powernukkitx.blockentity.BlockEntity;
import org.powernukkitx.blockentity.BlockEntitySpawnable;
import org.powernukkitx.event.player.PlayerInteractEvent;
import org.powernukkitx.inventory.HumanInventory;
import org.powernukkitx.item.Item;
import org.powernukkitx.level.Level;
import org.powernukkitx.math.BlockFace;
import org.powernukkitx.math.BlockVector3;
import org.powernukkitx.math.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.AbilitiesIndex;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.powernukkitx.anticheat.AntiCheatPlugin;
import org.powernukkitx.anticheat.util.ViolationId;

/**
 * @author Kaooot
 */
public class AntiCheatPlayer {

    @Getter
    private final Player serverPlayer;
    private final AntiCheatPlugin plugin;

    private final Map<String, PlayerTimeStats> timeStatistics =
        new Object2ObjectOpenHashMap<>();
    private static final Map<BlockFace, LevelEvent> BLOCK_FACE_MAP =
        new Object2ObjectOpenHashMap<>();

    private PlayerHandle playerHandle;
    private PlayerBlockActionData lastPlayerBlockAction;
    private Block intendedToBreakBlock;
    private BlockFace intendedToBreakBlockFace;
    private int clientStartedBreakTick;
    private int lastClientPredictedBreakTick;
    private int destroyTicks;
    private long clientStartDestroyFrame;
    private long clientPredictDestroyFrame;

    @Getter
    private int actorAttackAttempts;
    private int currentCpsTick;

    @Getter
    private int chatAttempts;
    private int currentChatTick;
    @Getter
    private int chatMessageCount;

    static {
        for (final BlockFace value : BlockFace.values()) {
            BLOCK_FACE_MAP.put(value, switch (value) {
                case DOWN -> LevelEvent.PARTICLE_BREAK_BLOCK_DOWN;
                case UP -> LevelEvent.PARTICLE_BREAK_BLOCK_UP;
                case NORTH -> LevelEvent.PARTICLE_BREAK_BLOCK_NORTH;
                case SOUTH -> LevelEvent.PARTICLE_BREAK_BLOCK_SOUTH;
                case EAST -> LevelEvent.PARTICLE_BREAK_BLOCK_EAST;
                case WEST -> LevelEvent.PARTICLE_BREAK_BLOCK_WEST;
            });
        }
    }

    public AntiCheatPlayer(Player serverPlayer, AntiCheatPlugin plugin) {
        this.serverPlayer = serverPlayer;
        this.plugin = plugin;
        this.plugin.getServer().getScheduler().scheduleRepeatingTask(() -> {
            this.tickBlockBreaking();
            this.tickCpsCounter();
            this.tickChatCounter();
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

    public float distance(Vector3i blockPos) {
        return blockPos.distance(this.serverPlayer.getFloorX(),
            (int) (this.serverPlayer.getBaseOffset() + this.serverPlayer.getFloorY()),
            this.serverPlayer.getFloorZ());
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
        for (final PlayerBlockActionData playerAction : packet.getPlayerBlockActions()) {
            this.handleBlockBreak(playerAction, packet);
        }
    }

    public void sendViolationWarning(ViolationId id, String message) {
        final String idValue = id.name().toLowerCase();
        final PlayerTimeStats stats = this.getTimeStatistic(idValue);
        if (System.currentTimeMillis() < stats.getTimeInMS() + this.plugin.getMainConfig()
            .getViolationWarningInterval()) {
            return;
        }
        this.updateTimeStatistic(idValue, System.currentTimeMillis(), -1, -1);

        final String additionalInfo = "§fTick: " + this.plugin.getServer().getTick() +
            "§f, TPS: §6" + this.plugin.getServer().getTicksPerSecond() +
            "§f, TpsAvg: §6" + this.plugin.getServer().getTicksPerSecondAverage() +
            "§f, Ping: §6" + this.serverPlayer.getPing() + " ms";
        message += " §7(" + additionalInfo + "§7)";

        this.plugin.getLogger().warning(message);

        final boolean shouldKick = this.plugin.getMainConfig().getKickValueOverrides()
            .getBoolean(id);

        if (shouldKick) {
            this.serverPlayer.kick("AntiCheat violation ID: " + id.name(), false);
        }

        if (!this.plugin.getMainConfig().isBroadcastViolationWarnings()) {
            return;
        }
        final String permission = this.plugin.getMainConfig()
            .getViolationBroadcastReceivePermission();
        for (final Player player : this.plugin.getServer().getOnlinePlayers().values()) {
            if (!player.hasPermission(permission)) {
                continue;
            }
            player.sendMessage("§7[§cAstra§7] §f" + message);
        }
    }

    private void handleBlockBreak(PlayerBlockActionData data, PlayerAuthInputPacket packet) {
        // ignore sword block breaking actions when in creative
        if (this.serverPlayer.getInventory() == null ||
            (this.serverPlayer.getInventory().getItemInMainHand().isSword() &&
                this.serverPlayer.isCreative())) {
            return;
        }
        final Vector3i blockPos = data.getBlockPosition();
        final long lastInvalidCreativeDestroyAction = this.getTimeStatistic(
            PlayerTimeStats.LAST_INVALID_CREATIVE_DESTROY_ACTION
        ).getTimeInMS();
        if (System.currentTimeMillis() - lastInvalidCreativeDestroyAction < 100L) {
            this.sendDestroyCorrection(blockPos);
            return;
        }
        if (!this.serverPlayer.getAdventureSettings().get(AbilitiesIndex.MINE)) {
            this.sendDestroyCorrection(blockPos);
            this.sendViolationWarning(
                ViolationId.INVALID_MINE_ABILITY_STATE,
                this.getName() + " failed mine ability state change"
            );
            return;
        }
        final float maxDistance = this.serverPlayer.isCreative() ?
            this.plugin.getMainConfig().getMaxBlockBreakDistanceCreative() :
            this.plugin.getMainConfig().getMaxBlockBreakDistance();
        final float distance = this.distance(blockPos);
        if (distance > maxDistance) {
            this.sendDestroyCorrection(blockPos);
            final float inappropriateDistance = maxDistance * 1.5f;
            if (distance > inappropriateDistance) {
                this.sendViolationWarning(
                    ViolationId.INAPPROPRIATE_BLOCK_INTERACTION_RANGE,
                    this.getName() + " failed inappropriate block interaction, " +
                        "range: " + inappropriateDistance
                );
            }
            return;
        }
        this.handleBlockIntentChange(blockPos, data.getFacing(), packet);
        switch (data.getPlayerActionType()) {
            case START_DESTROY_BLOCK -> this.startDestroyBlock(data, packet);
            case CONTINUE_DESTROY_BLOCK -> this.continueDestroyBlock(data, packet);
            case PREDICT_DESTROY_BLOCK -> this.predictDestroyBlock(data, packet);
            case ABORT_DESTROY_BLOCK -> this.abortDestroyBlock(data);
        }
        this.lastPlayerBlockAction = data;
    }

    private void startDestroyBlock(PlayerBlockActionData data, PlayerAuthInputPacket packet) {
        this.startDestroyBlock(data.getBlockPosition(), data.getFacing(), packet);
    }

    private void startDestroyBlock(Vector3i blockPos, int face, PlayerAuthInputPacket packet) {
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
        this.clientStartDestroyFrame = packet.getClientTick();
    }

    /**
     * Handles a PlayerActionType::ContinueDestroyBlock action which is interpreted as a new start
     * destroy block action when the client breaks blocks consecutively.
     *
     * @param data the {@link org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData}
     */
    private void continueDestroyBlock(PlayerBlockActionData data, PlayerAuthInputPacket packet) {
        if (this.lastPlayerBlockAction != null &&
            this.lastPlayerBlockAction.getPlayerActionType()
                .equals(PlayerActionType.PREDICT_DESTROY_BLOCK)) {
            //  this.resetBlockDestructionData();
            this.startDestroyBlock(data, packet);
            this.clientStartedBreakTick = this.lastClientPredictedBreakTick;
            this.destroyTicks = this.plugin.getServer().getTick() -
                this.lastClientPredictedBreakTick;
            this.clientStartDestroyFrame = this.clientPredictDestroyFrame;
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
                this.BLOCK_FACE_MAP.get(this.intendedToBreakBlockFace),
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
    private void predictDestroyBlock(PlayerBlockActionData data, PlayerAuthInputPacket packet) {
        this.lastClientPredictedBreakTick = this.plugin.getServer().getTick();
        final Vector3i blockPos = data.getBlockPosition();
        final Vector3 pos = Vector3.fromNetwork(blockPos.toFloat());
        this.sendAbortBreakParticles(blockPos);
        this.clientPredictDestroyFrame = packet.getClientTick();
        final long frames = this.clientPredictDestroyFrame - this.clientStartDestroyFrame;
        final int serverPredictedBreakTime = this.getBreakTimeTicks(pos);
        if (serverPredictedBreakTime - frames > serverPredictedBreakTime *
            this.plugin.getMainConfig().getBlockBreakProgressOffset()) {
            this.sendDestroyCorrection(this.getLevel().getBlock(pos));
            return;
        }
        this.breakBlock(pos.asBlockVector3(), BlockFace.fromIndex(data.getFacing()));
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
    private void handleBlockIntentChange(Vector3i blockPos, int face,
                                         PlayerAuthInputPacket packet) {
        final PlayerBlockActionData lastAction = this.lastPlayerBlockAction;
        final BlockVector3 lastBreakPos = lastAction == null ? null :
            BlockVector3.fromNetwork(lastAction.getBlockPosition());
        if (lastBreakPos != null && (lastBreakPos.getX() != blockPos.getX() ||
            lastBreakPos.getY() != blockPos.getY() || lastBreakPos.getZ() != blockPos.getZ())) {
            final int breakTime = this.getBreakTimeTicks(lastBreakPos.asVector3());
            final boolean canBreak = breakTime -
                (this.plugin.getServer().getTick() - this.clientStartedBreakTick) <= 1;
            if (canBreak &&
                lastAction.getPlayerActionType().equals(PlayerActionType.START_DESTROY_BLOCK)) {
                this.breakBlock(
                    lastBreakPos,
                    BlockFace.fromIndex(lastAction.getFacing())
                );
            }
            this.abortDestroyBlock(lastBreakPos.toNetwork());
            this.startDestroyBlock(blockPos, face, packet);
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

    private void sendDestroyCorrection(Vector3i blockPos) {
        this.sendDestroyCorrection(
            this.getLevel().getBlock(BlockVector3.fromNetwork(blockPos).asVector3())
        );
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

    private void tickCpsCounter() {
        if (this.currentCpsTick % 20 == 0) {
            final int value = this.actorAttackAttempts;
            final int maxCPS = this.plugin.getMainConfig().getMaxClicksPerSecond();
            final boolean exceeded = value > maxCPS;
            if (exceeded) {
                this.sendViolationWarning(
                    ViolationId.EXCEEDED_MAX_CPS, this.getName() +
                        " exceeded the maximum Clicks Per Second, value: " + value +
                        ", max: " + maxCPS
                );
            }
            this.actorAttackAttempts = 0;
            this.currentCpsTick = 0;
        }
        this.currentCpsTick++;
    }

    public void increaseActorAttackAttempts() {
        this.actorAttackAttempts++;
    }

    public void increaseChatAttempts() {
        this.chatAttempts++;
    }

    public void increaseChatMessageCount() {
        this.chatMessageCount++;
    }

    private void tickChatCounter() {
        if (this.currentChatTick % 20 == 0) {
            this.chatAttempts = 0;
        }
        if (this.currentChatTick % 1200 == 0) {
            this.chatMessageCount = 0;
        }
        this.currentChatTick++;
    }
}