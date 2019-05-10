package quickcarpet.patches;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.network.packet.EntityPositionS2CPacket;
import net.minecraft.client.network.packet.EntitySetHeadYawS2CPacket;
import net.minecraft.client.network.packet.PlayerListS2CPacket;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import quickcarpet.utils.IServerPlayerEntity;
import quickcarpet.utils.Messenger;

public class ServerPlayerEntityFake extends ServerPlayerEntity
{
    public Runnable fixStartingPosition = () -> {};
    
    public static ServerPlayerEntityFake createFake(String username, MinecraftServer server, double d0, double d1, double d2, double yaw, double pitch, DimensionType dimension, GameMode gamemode)
    {
        //prolly half of that crap is not necessary, but it works
        ServerWorld worldIn = server.getWorld(dimension);
        ServerPlayerInteractionManager interactionManagerIn = new ServerPlayerInteractionManager(worldIn);
        GameProfile gameprofile = server.getUserCache().findByName(username);
        if (gameprofile == null)
        {
            return null;
        }
        if (gameprofile.getProperties().containsKey("textures"))
        {
            gameprofile = SkullBlockEntity.loadProperties(gameprofile);
        }
        ServerPlayerEntityFake instance = new ServerPlayerEntityFake(server, worldIn, gameprofile, interactionManagerIn);
        instance.fixStartingPosition = () -> instance.setPositionAndAngles(d0, d1, d2, (float) yaw, (float) pitch);
        server.getPlayerManager().onPlayerConnect(new ClientConnectionFake(NetworkSide.SERVERBOUND), instance);
        if (instance.dimension != dimension) //player was logged in in a different dimension
        {
            ServerWorld old_world = server.getWorld(instance.dimension);
            instance.dimension = dimension;
            old_world.removePlayer(instance);
            instance.removed = false;
            worldIn.spawnEntity(instance);
            instance.setWorld(worldIn);
            server.getPlayerManager().sendWorldInfo(instance, old_world);
            instance.networkHandler.requestTeleport(d0, d1, d2, (float) yaw, (float) pitch);
            instance.interactionManager.setWorld(worldIn);
        }
        instance.setHealth(20.0F);
        instance.removed = false;
        instance.networkHandler.requestTeleport(d0, d1, d2, (float) yaw, (float) pitch);
        instance.stepHeight = 0.6F;
        interactionManagerIn.setGameMode(gamemode);
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(instance, (byte) (instance.headYaw * 256 / 360)), instance.dimension);
        server.getPlayerManager().sendToDimension(new EntityPositionS2CPacket(instance), instance.dimension);
        instance.getServerWorld().method_14178().updateCameraPosition(instance);
        return instance;
    }
    
    public static ServerPlayerEntityFake createShadow(MinecraftServer server, ServerPlayerEntity player)
    {
        player.getServer().getPlayerManager().remove(player);
        player.networkHandler.disconnect(new TranslatableComponent("multiplayer.disconnect.duplicate_login"));
        ServerWorld worldIn = server.getWorld(player.dimension);
        ServerPlayerInteractionManager interactionManagerIn = new ServerPlayerInteractionManager(worldIn);
        GameProfile gameprofile = player.getGameProfile();
        ServerPlayerEntityFake playerShadow = new ServerPlayerEntityFake(server, worldIn, gameprofile, interactionManagerIn);
        server.getPlayerManager().onPlayerConnect(new ClientConnectionFake(NetworkSide.SERVERBOUND), playerShadow);
        
        playerShadow.setHealth(player.getHealth());
        playerShadow.networkHandler.requestTeleport(player.x, player.y, player.z, player.yaw, player.pitch);
        interactionManagerIn.setGameMode(player.interactionManager.getGameMode());
        ((IServerPlayerEntity) playerShadow).getActionPack().copyFrom(((IServerPlayerEntity) player).getActionPack());
        playerShadow.stepHeight = 0.6F;
        
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(playerShadow, (byte) (player.headYaw * 256 / 360)), playerShadow.dimension);
        server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, playerShadow));
        player.getServerWorld().method_14178().updateCameraPosition(playerShadow);
        return playerShadow;
    }
    
    private ServerPlayerEntityFake(MinecraftServer server, ServerWorld worldIn, GameProfile profile, ServerPlayerInteractionManager interactionManagerIn)
    {
        super(server, worldIn, profile, interactionManagerIn);
    }
    
    @Override
    public void kill()
    {
        this.server.method_18858(new ServerTask(this.server.getTicks(), () -> {
            this.networkHandler.onDisconnected(Messenger.s("Killed"));
        }));
    }
    
    @Override
    public void tick()
    {
        if (this.getServer().getTicks() % 10 == 0)
        {
            this.networkHandler.syncWithPlayerPosition();
            this.getServerWorld().method_14178().updateCameraPosition(this);
        }
        super.tick();
        this.method_14226();
    }
    
    @Override
    public void onDeath(DamageSource cause)
    {
        super.onDeath(cause);
        setHealth(20);
        kill();
    }
}
