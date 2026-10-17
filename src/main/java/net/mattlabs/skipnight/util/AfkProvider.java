package net.mattlabs.skipnight.util;

import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Soft-dependency wrapper around EssentialsX's AFK status.
 * <p>
 * EssentialsX is declared as {@code softdepend} in plugin.yml and as a
 * {@code provided} (compile-only) Maven dependency: SkipNight must keep
 * working when EssentialsX is not installed, and must never bundle
 * EssentialsX's classes into its own jar.
 * <p>
 * All lookups here are defensive on purpose - a server admin can install,
 * remove, or reload EssentialsX independently of SkipNight, so this can't
 * assume the plugin instance obtained at startup is still valid later.
 */
public final class AfkProvider {

    private final boolean essentialsPresent;
    private final Logger logger;

    public AfkProvider(Logger logger) {
        this.logger = logger;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        this.essentialsPresent = plugin instanceof IEssentials;
        if (!essentialsPresent) {
            logger.info("EssentialsX not found, disabling AFK exclusion from votes.");
        }
    }

    public boolean isEssentialsPresent() {
        return essentialsPresent;
    }

    /**
     * @return true only if EssentialsX is present, tracks this player, and
     * currently reports them as AFK. Fails safe (returns false, i.e. "count
     * this player normally") on any lookup problem, so a player is never
     * silently excluded from a vote due to an integration hiccup.
     */
    public boolean isAfk(Player player) {
        if (!essentialsPresent) return false;
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (!(plugin instanceof IEssentials essentials) || !plugin.isEnabled()) return false;
            IUser user = essentials.getUser(player.getUniqueId());
            return user != null && user.isAfk();
        } catch (Exception e) {
            // EssentialsX has changed internal APIs across versions before, and
            // will likely do so again. A failure here must never take the vote
            // system down with it - log once per occurrence and treat the
            // player as active for this check.
            logger.warning("Failed to read EssentialsX AFK status for "
                    + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
