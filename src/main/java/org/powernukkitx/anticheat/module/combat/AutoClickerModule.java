package org.powernukkitx.anticheat.module.combat;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.server.PacketReceiveEvent;
import java.util.Optional;
import org.cloudburstmc.protocol.bedrock.data.payload.inventory.transaction.ItemUseOnActorActionType;
import org.cloudburstmc.protocol.bedrock.data.payload.inventory.transaction.data.InventoryTransactionDataType;
import org.cloudburstmc.protocol.bedrock.data.payload.inventory.transaction.data.ItemUseOnActorInventoryTransaction;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.powernukkitx.anticheat.AntiCheatPlugin;
import org.powernukkitx.anticheat.module.Module;
import org.powernukkitx.anticheat.module.ModuleType;
import org.powernukkitx.anticheat.player.AntiCheatPlayer;
import org.powernukkitx.anticheat.util.ViolationId;

/**
 * @author Kaooot
 */
public class AutoClickerModule extends Module {

    public AutoClickerModule(AntiCheatPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onInventoryTransaction(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof InventoryTransactionPacket packet)) {
            return;
        }
        if (!packet.getTransaction().getType()
            .equals(InventoryTransactionDataType.ITEM_USE_ON_ACTOR)) {
            return;
        }
        final ItemUseOnActorInventoryTransaction transaction = (ItemUseOnActorInventoryTransaction)
            packet.getTransaction();
        if (!transaction.getActionType().equals(ItemUseOnActorActionType.ATTACK)) {
            return;
        }
        final Optional<AntiCheatPlayer> optional = this.plugin.getPlayerRegistry()
            .getPlayer(event.getPlayer().getUniqueId());
        if (optional.isEmpty()) {
            return;
        }
        final AntiCheatPlayer player = optional.get();
        if (player.getLevel().getEntity(transaction.getRuntimeId()) == null ||
            event.getPlayer().getId() == transaction.getRuntimeId()) {
            player.sendViolationWarning(
                ViolationId.INVALID_ATTACK_ACTOR,
                player.getName() + " tried to attack an invalid actor"
            );
            return;
        }
        final int value = player.getActorAttackAttempts() + 1;
        if (value > this.plugin.getMainConfig().getCpsLimit()) {
            event.setCancelled();
        }
        player.increaseActorAttackAttempts();
    }

    @Override
    public String getName() {
        return "AutoClicker";
    }

    @Override
    public ModuleType getType() {
        return ModuleType.COMBAT;
    }
}