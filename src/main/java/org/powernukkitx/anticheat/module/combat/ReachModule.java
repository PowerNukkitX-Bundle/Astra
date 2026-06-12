package org.powernukkitx.anticheat.module.combat;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import java.util.Optional;
import org.powernukkitx.anticheat.AntiCheatPlugin;
import org.powernukkitx.anticheat.module.Module;
import org.powernukkitx.anticheat.module.ModuleType;
import org.powernukkitx.anticheat.player.AntiCheatPlayer;
import org.powernukkitx.anticheat.util.MathUtil;
import org.powernukkitx.anticheat.util.ViolationId;

/**
 * @author Kaooot
 */
public class ReachModule extends Module {

    public ReachModule(AntiCheatPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final Optional<AntiCheatPlayer> optional = this.plugin.getPlayerRegistry()
            .getPlayer(player.getUniqueId());
        if (optional.isEmpty()) {
            return;
        }
        final float maxRange = player.isCreative() ?
            this.plugin.getMainConfig().getMaxActorInteractionRangeCreative() :
            this.plugin.getMainConfig().getMaxActorInteractionRange();
        final AntiCheatPlayer antiCheatPlayer = optional.get();
        final float distance = antiCheatPlayer.distance(
            event.getEntity().asBlockVector3().toNetwork()
        );
        if (distance > maxRange) {
            antiCheatPlayer.sendViolationWarning(
                ViolationId.EXCEEDED_ACTOR_INTERACTION_RANGE, player.getName() +
                    " exceeded the allowed actor interaction range, value: " +
                    MathUtil.round(distance) + ", max: " + maxRange
            );
        }
        if (distance > this.plugin.getMainConfig().getActorInteractionRangeLimit()) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        final Optional<AntiCheatPlayer> optional = this.plugin.getPlayerRegistry()
            .getPlayer(damager.getUniqueId());
        if (optional.isEmpty()) {
            return;
        }
        final AntiCheatPlayer player = optional.get();
        final float maxRange = damager.isCreative() ?
            this.plugin.getMainConfig().getMaxActorAttackRange() :
            this.plugin.getMainConfig().getMaxActorAttackRangeCreative();
        final float distance = player.distance(event.getEntity().asBlockVector3().toNetwork());
        if (distance > maxRange) {
            player.sendViolationWarning(
                ViolationId.EXCEEDED_ACTOR_ATTACK_RANGE, player.getName() +
                    " exceeded the allowed actor attack range, value: " +
                    MathUtil.round(distance) + ", max: " + maxRange
            );
        }
        if (distance > this.plugin.getMainConfig().getActorAttackRangeLimit()) {
            event.setCancelled();
        }
    }

    @Override
    public String getName() {
        return "Reach";
    }

    @Override
    public ModuleType getType() {
        return ModuleType.COMBAT;
    }
}