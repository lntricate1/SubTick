package subtick.mixins.carpet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import carpet.CarpetSettings;
import carpet.commands.TickCommand;
import carpet.settings.SettingsManager;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;

@Mixin(TickCommand.class)
public class TickCommandMixin
{
  @Shadow static int setTps(CommandSourceStack source, float tps){return 0;}
  @Shadow static int queryTps(CommandSourceStack source){return 0;}
  @Shadow static int setWarp(CommandSourceStack source, int advance, String tail_command){return 0;}
  @Shadow static int healthReport(CommandSourceStack source, int ticks){return 0;}
  @Shadow static int healthEntities(CommandSourceStack source, int ticks){return 0;}

  @Inject(method = "register", at = @At(value = "HEAD"), cancellable = true, remap = false)
  private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci)
  {
    dispatcher.register(
      literal("tick")
        .requires((p) -> SettingsManager.canUseCommand(p, CarpetSettings.commandTick))
        .then(literal("rate")
          .executes((c) -> queryTps(c.getSource()))
          .then(argument("rate", floatArg(0.1F, 500.0F))
            .suggests((c, b) -> suggest(new String[]{"20.0"}, b))
            .executes((c) -> setTps(c.getSource(), getFloat(c, "rate")))
          )
        )
        .then(literal("warp")
          .executes((c) -> setWarp(c.getSource(), 0, null))
          .then(argument("tail command", greedyString())
            .executes((c) -> setWarp(c.getSource(), getInteger(c, "ticks"), getString(c, "tail command")))
          )
        )
        .then(literal("health")
          .executes((c) -> healthReport(c.getSource(), 100))
          .then(argument("ticks", integer(20, 24000))
            .executes((c) -> healthReport(c.getSource(), getInteger(c, "ticks")))
          )
        )
        .then(literal("entities")
          .executes((c) -> healthEntities(c.getSource(), 100))
          .then(argument("ticks", integer(20, 24000))
            .executes((c) -> healthEntities(c.getSource(), getInteger(c, "ticks")))
          )
        )
    );
    ci.cancel();
  }
}
