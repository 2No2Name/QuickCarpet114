package quickcarpet.utils;

import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.StringTextComponent;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TextFormat;
import net.minecraft.text.event.ClickEvent;
import net.minecraft.text.event.HoverEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Messenger {

    public static final Logger LOG = LogManager.getLogger();

    /*
     messsage: "desc me ssa ge"
     desc contains:
     i = italic
     s = strikethrough
     u = underline
     b = bold
     o = obfuscated

     w = white
     y = yellow
     m = magenta (light purple)
     r = red
     c = cyan (aqua)
     l = lime (green)
     t = light blue (blue)
     f = dark gray
     g = gray
     d = gold
     p = dark purple (purple)
     n = dark red (brown)
     q = dark aqua
     e = dark green
     v = dark blue (navy)
     k = black

     / = action added to the previous component
     */

    private static TextComponent _applyStyleToTextComponent(TextComponent comp, String style)
    {
        //could be rewritten to be more efficient
        comp.getStyle().setItalic(style.indexOf('i')>=0);
        comp.getStyle().setStrikethrough(style.indexOf('s')>=0);
        comp.getStyle().setUnderline(style.indexOf('u')>=0);
        comp.getStyle().setBold(style.indexOf('b')>=0);
        comp.getStyle().setObfuscated(style.indexOf('o')>=0);
        comp.getStyle().setColor(TextFormat.WHITE);
        if (style.indexOf('w')>=0) comp.getStyle().setColor(TextFormat.WHITE); // not needed
        if (style.indexOf('y')>=0) comp.getStyle().setColor(TextFormat.YELLOW);
        if (style.indexOf('m')>=0) comp.getStyle().setColor(TextFormat.LIGHT_PURPLE);
        if (style.indexOf('r')>=0) comp.getStyle().setColor(TextFormat.RED);
        if (style.indexOf('c')>=0) comp.getStyle().setColor(TextFormat.AQUA);
        if (style.indexOf('l')>=0) comp.getStyle().setColor(TextFormat.GREEN);
        if (style.indexOf('t')>=0) comp.getStyle().setColor(TextFormat.BLUE);
        if (style.indexOf('f')>=0) comp.getStyle().setColor(TextFormat.DARK_GRAY);
        if (style.indexOf('g')>=0) comp.getStyle().setColor(TextFormat.GRAY);
        if (style.indexOf('d')>=0) comp.getStyle().setColor(TextFormat.GOLD);
        if (style.indexOf('p')>=0) comp.getStyle().setColor(TextFormat.DARK_PURPLE);
        if (style.indexOf('n')>=0) comp.getStyle().setColor(TextFormat.DARK_RED);
        if (style.indexOf('q')>=0) comp.getStyle().setColor(TextFormat.DARK_AQUA);
        if (style.indexOf('e')>=0) comp.getStyle().setColor(TextFormat.DARK_GREEN);
        if (style.indexOf('v')>=0) comp.getStyle().setColor(TextFormat.DARK_BLUE);
        if (style.indexOf('k')>=0) comp.getStyle().setColor(TextFormat.BLACK);
        return comp;
    }
    public static String heatmap_color(double actual, double reference)
    {
        String color = "e";
        if (actual > 0.5D*reference) color = "y";
        if (actual > 0.8D*reference) color = "r";
        if (actual > reference) color = "m";
        return color;
    }
    public static String creatureTypeColor(EntityCategory type)
    {
        switch (type)
        {
            case MONSTER:
                return "n";
            case CREATURE:
                return "e";
            case AMBIENT:
                return "f";
            case WATER_CREATURE:
                return "v";
        }
        return "w";
    }

    private static TextComponent _getChatComponentFromDesc(String message, TextComponent previous_message)
    {
        if (message.equalsIgnoreCase(""))
        {
            return new StringTextComponent("");
        }
        String parts[] = message.split("\\s", 2);
        String desc = parts[0];
        String str = "";
        if (parts.length > 1) str = parts[1];
        if (desc.charAt(0) == '/') // deprecated
        {
            if (previous_message != null)
                previous_message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message));
            return previous_message;
        }
        if (desc.charAt(0) == '?')
        {
            if (previous_message != null)
                previous_message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
            return previous_message;
        }
        if (desc.charAt(0) == '!')
        {
            if (previous_message != null)
                previous_message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
            return previous_message;
        }
        if (desc.charAt(0) == '^')
        {
            if (previous_message != null)
                previous_message.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c(message.substring(1))));
            return previous_message;
        }
        TextComponent txt = new StringTextComponent(str);
        return _applyStyleToTextComponent(txt, desc);
    }
    public static TextComponent tp(String desc, Vec3d pos) { return tp(desc, pos.x, pos.y, pos.z); }
    public static TextComponent tp(String desc, BlockPos pos) { return tp(desc, pos.getX(), pos.getY(), pos.getZ()); }
    public static TextComponent tp(String desc, double x, double y, double z) { return tp(desc, (float)x, (float)y, (float)z);}
    public static TextComponent tp(String desc, float x, float y, float z)
    {
        return _getCoordsTextComponent(desc, x, y, z, false);
    }
    public static TextComponent tp(String desc, int x, int y, int z)
    {
        return _getCoordsTextComponent(desc, (float)x, (float)y, (float)z, true);
    }

    /// to be continued
    public static TextComponent dbl(String style, double double_value)
    {
        return c(String.format("%s %.1f",style,double_value),String.format("^w %f",double_value));
    }
    public static TextComponent dbls(String style, double ... doubles)
    {
        StringBuilder str = new StringBuilder(style + " [ ");
        String prefix = "";
        for (double dbl : doubles)
        {
            str.append(String.format("%s%.1f", prefix, dbl));
            prefix = ", ";
        }
        str.append(" ]");
        return c(str.toString());
    }
    public static TextComponent dblf(String style, double ... doubles)
    {
        StringBuilder str = new StringBuilder(style + " [ ");
        String prefix = "";
        for (double dbl : doubles)
        {
            str.append(String.format("%s%f", prefix, dbl));
            prefix = ", ";
        }
        str.append(" ]");
        return c(str.toString());
    }
    public static TextComponent dblt(String style, double ... doubles)
    {
        List<Object> components = new ArrayList<>();
        components.add(style+" [ ");
        String prefix = "";
        for (double dbl:doubles)
        {

            components.add(String.format("%s %s%.1f",style, prefix, dbl));
            components.add("?"+dbl);
            components.add("^w "+dbl);
            prefix = ", ";
        }
        //components.remove(components.size()-1);
        components.add(style+"  ]");
        return c(components.toArray(new Object[0]));
    }

    private static TextComponent _getCoordsTextComponent(String style, float x, float y, float z, boolean isInt)
    {
        String text;
        String command;
        if (isInt)
        {
            text = String.format("%s [ %d, %d, %d ]",style, (int)x,(int)y, (int)z );
            command = String.format("!/tp %d %d %d",(int)x,(int)y, (int)z);
        }
        else
        {
            text = String.format("%s [ %.1f, %.1f, %.1f]",style, x, y, z);
            command = String.format("!/tp %.3f %.3f %.3f",x, y, z);
        }
        return c(text, command);
    }

    //message source
    public static void m(ServerCommandSource source, Object ... fields)
    {
        source.sendFeedback(Messenger.c(fields),true);
    }
    public static void m(PlayerEntity player, Object ... fields)
    {
        player.appendCommandFeedback(Messenger.c(fields));
    }

    /*
    composes single line, multicomponent message, and returns as one chat messagge
     */
    public static TextComponent c(Object ... fields)
    {
        TextComponent message = new StringTextComponent("");
        TextComponent previous_component = null;
        for (Object o: fields)
        {
            if (o instanceof TextComponent)
            {
                message.append((TextComponent)o);
                previous_component = (TextComponent)o;
                continue;
            }
            String txt = o.toString();
            TextComponent comp = _getChatComponentFromDesc(txt,previous_component);
            if (comp != previous_component) message.append(comp);
            previous_component = comp;
        }
        return message;
    }

    //simple text

    public static TextComponent s(@Nonnull String text)
    {
        return s(text,"");
    }
    public static TextComponent s(@Nonnull String text, @Nonnull String style)
    {
        TextComponent message = new StringTextComponent(text);
        _applyStyleToTextComponent(message, style);
        return message;
    }




    public static void send(PlayerEntity player, Collection<TextComponent> lines)
    {
        lines.forEach(player::appendCommandFeedback);
    }
    public static void send(ServerCommandSource source, Collection<TextComponent> lines)
    {
        lines.stream().forEachOrdered((s) -> source.sendFeedback(s, false));
    }


    public static void print_server_message(MinecraftServer server, String message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message);
        server.appendCommandFeedback(new StringTextComponent(message));
        TextComponent txt = c("gi "+message);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.appendCommandFeedback(txt);
        }
    }
    public static void print_server_message(MinecraftServer server, TextComponent message)
    {
        if (server == null)
            LOG.error("Message not delivered: "+message.getString());
        server.appendCommandFeedback(message);
        for (PlayerEntity entityplayer : server.getPlayerManager().getPlayerList())
        {
            entityplayer.appendCommandFeedback(message);
        }
    }
}
