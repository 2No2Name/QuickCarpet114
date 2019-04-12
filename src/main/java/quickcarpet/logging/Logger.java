package quickcarpet.logging;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TextComponent;
import quickcarpet.QuickCarpet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Logger
{
    // The set of subscribed and online players.
    private Map<String, String> subscribedOnlinePlayers;

    // The set of subscribed and offline players.
    private Map<String,String> subscribedOfflinePlayers;

    // The logName of this log. Gets prepended to logged messages.
    private String logName;

    private String default_option;

    private String[] options;

    public Logger(String logName, String def, String [] options)
    {
        subscribedOnlinePlayers = new HashMap<>();
        subscribedOfflinePlayers = new HashMap<>();
        this.logName = logName;
        this.default_option = def;
        this.options = options;
    }

    public String getDefault()
    {
        return default_option;
    }
    public String [] getOptions()
    {
        if (options == null)
        {
            return new String[0];
        }
        return options;
    }
    public String getLogName()
    {
        return logName;
    }

    /**
     * Subscribes the player with the given logName to the logger.
     */
    public void addPlayer(String playerName, String option)
    {
        if (playerFromName(playerName) != null)
        {
            subscribedOnlinePlayers.put(playerName, option);
        }
        else
        {
            subscribedOfflinePlayers.put(playerName, option);
        }
        LoggerRegistry.setAccess(this);
    }

    /**
     * Unsubscribes the player with the given logName from the logger.
     */
    public void removePlayer(String playerName)
    {
        subscribedOnlinePlayers.remove(playerName);
        subscribedOfflinePlayers.remove(playerName);
        LoggerRegistry.setAccess(this);
    }

    /**
     * Returns true if there are any online subscribers for this log.
     */
    public boolean hasOnlineSubscribers()
    {
        return subscribedOnlinePlayers.size() > 0;
    }

    /**
     * serves messages to players fetching them from the promise
     * will repeat invocation for players that share the same option
     */
    @FunctionalInterface
    public interface lMessage { TextComponent[] get(String playerOption, PlayerEntity player);}
    public void log(lMessage messagePromise)
    {
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            PlayerEntity player = playerFromName(en.getKey());
            if (player != null)
            {
                TextComponent [] messages = messagePromise.get(en.getValue(),player);
                if (messages != null)
                    sendPlayerMessage(player, messages);
            }
        }
    }

    /**
     * guarantees that each message for each option will be evaluated once from the promise
     * and served the same way to all other players subscribed to the same option
     */
    @FunctionalInterface
    public interface lMessageIgnorePlayer { TextComponent [] get(String playerOption);}
    public void log(lMessageIgnorePlayer messagePromise)
    {
        Map<String, TextComponent[]> cannedMessages = new HashMap<>();
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            PlayerEntity player = playerFromName(en.getKey());
            if (player != null)
            {
                String option = en.getValue();
                if (!cannedMessages.containsKey(option))
                {
                    cannedMessages.put(option,messagePromise.get(option));
                }
                TextComponent [] messages = cannedMessages.get(option);
                if (messages != null)
                    sendPlayerMessage(player, messages);
            }
        }
    }
    /**
     * guarantees that message is evaluated once, so independent from the player and chosen option
     */
    public void log(Supplier<TextComponent[]> messagePromise)
    {
        TextComponent [] cannedMessages = null;
        for (Map.Entry<String,String> en : subscribedOnlinePlayers.entrySet())
        {
            PlayerEntity player = playerFromName(en.getKey());
            if (player != null)
            {
                if (cannedMessages == null) cannedMessages = messagePromise.get();
                sendPlayerMessage(player, cannedMessages);
            }
        }
    }

    public void sendPlayerMessage(PlayerEntity player, TextComponent ... messages)
    {
        Arrays.stream(messages).forEach(player::appendCommandFeedback);
    }

    /**
     * Gets the {@code EntityPlayer} instance for a player given their UUID. Returns null if they are offline.
     */
    protected PlayerEntity playerFromName(String name)
    {
        return QuickCarpet.minecraft_server.getPlayerManager().getPlayer(name);
    }

    // ----- Event Handlers ----- //

    public void onPlayerConnect(PlayerEntity player)
    {
        // If the player was subscribed to the log and offline, move them to the set of online subscribers.
        String playerName = player.getName().getString();
        if (subscribedOfflinePlayers.containsKey(playerName))
        {
            subscribedOnlinePlayers.put(playerName, subscribedOfflinePlayers.get(playerName));
            subscribedOfflinePlayers.remove(playerName);
        }
        LoggerRegistry.setAccess(this);
    }

    public void onPlayerDisconnect(PlayerEntity player)
    {
        // If the player was subscribed to the log, move them to the set of offline subscribers.
        String playerName = player.getName().getString();
        if (subscribedOnlinePlayers.containsKey(playerName))
        {
            subscribedOfflinePlayers.put(playerName, subscribedOnlinePlayers.get(playerName));
            subscribedOnlinePlayers.remove(playerName);
        }
        LoggerRegistry.setAccess(this);
    }

    public String getAcceptedOption(String arg)
    {
        if (options != null && Arrays.asList(options).contains(arg)) return arg;
        return null;
    }
}
