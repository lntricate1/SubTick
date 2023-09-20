package subtick.mixins.carpet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import carpet.CarpetSettings;
import carpet.commands.TickCommand;
//#if MC >= 11900
//$$ import carpet.utils.CommandHelper;
//$$ import net.minecraft.commands.CommandBuildContext;
//#else
import carpet.settings.SettingsManager;
//#endif
import net.minecraft.commands.CommandSourceStack;
import subtick.Settings;
import subtick.SubTick;
import subtick.TickHandler;
import subtick.TickPhase;
import subtick.util.Translations;

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
  // @Shadow static int setTps(CommandSourceStack source, float tps){return 0;}
  // @Shadow static int queryTps(CommandSourceStack source){return 0;}
  // @Shadow static int setWarp(CommandSourceStack source, int advance, String tail_command){return 0;}
  // @Shadow static int healthReport(CommandSourceStack source, int ticks){return 0;}
  // @Shadow static int healthEntities(CommandSourceStack source, int ticks){return 0;}
  //
  // @Inject(method = "register", at = @At(value = "HEAD"), cancellable = true, remap = false)
  // private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
  //   //#if MC >= 11900
  //   //$$ CommandBuildContext commandBuildContext,
  //   //#endif
  //   CallbackInfo ci)
  // {
  //   dispatcher.register(
  //     literal("tick")
  //       //#if MC >= 11900
  //       //$$ .requires((p) -> CommandHelper.canUseCommand(p, CarpetSettings.commandTick))
  //       //#else
  //       .requires((p) -> SettingsManager.canUseCommand(p, CarpetSettings.commandTick))
  //       //#endif
  //       .then(literal("rate")
  //         .executes((c) -> queryTps(c.getSource()))
  //         .then(argument("rate", floatArg(0.1F, 500.0F))
  //           .suggests((c, b) -> suggest(new String[]{"20.0"}, b))
  //           .executes((c) -> setTps(c.getSource(), getFloat(c, "rate")))
  //         )
  //       )
  //       .then(literal("warp")
  //         .executes((c) -> setWarp(c.getSource(), 0, null))
  //         .then(argument("ticks", integer(0))
  //           .suggests((c, b) -> suggest(new String[]{"3600", "72000"}, b))
  //           .executes((c) -> setWarp(c.getSource(), getInteger(c, "ticks"), null))
  //           .then(argument("tail command", greedyString())
  //             .executes((c) -> setWarp(c.getSource(), getInteger(c, "ticks"), getString(c, "tail command")))
  //           )
  //         )
  //       )
  //       .then(literal("health")
  //         .executes((c) -> healthReport(c.getSource(), 100))
  //         .then(argument("ticks", integer(20, 24000))
  //           .executes((c) -> healthReport(c.getSource(), getInteger(c, "ticks")))
  //         )
  //       )
  //       .then(literal("entities")
  //         .executes((c) -> healthEntities(c.getSource(), 100))
  //         .then(argument("ticks", integer(20, 24000))
  //           .executes((c) -> healthEntities(c.getSource(), getInteger(c, "ticks")))
  //         )
  //       )
  //   );
  //   dispatcher.getRoot().addChild(literal("tick").build());
  //   ci.cancel();
  // }

  @Inject(method = "freezeStatus", at = @At("HEAD"), cancellable = true, remap = false)
  private static void freezeStatus(CommandSourceStack c, CallbackInfoReturnable<Integer> cir)
  {
    cir.setReturnValue(subtick.commands.TickCommand.when(c));
  }

  @Inject(method = "setFreeze", at = @At("HEAD"), cancellable = true, remap = false)
  private static void setFreeze(CommandSourceStack c, boolean isDeep, boolean freeze, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
  {
    if(freeze)
      cir.setReturnValue(subtick.commands.TickCommand.freeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
    else
      cir.setReturnValue(subtick.commands.TickCommand.unfreeze(c));
  }

  @Inject(method = "toggleFreeze", at = @At("HEAD"), cancellable = true, remap = false)
  private static void toggleFreeze(CommandSourceStack c, boolean isDeep, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
  {
    cir.setReturnValue(subtick.commands.TickCommand.toggleFreeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
  }

  @Inject(method = "step", at = @At("HEAD"), cancellable = true, remap = false)
  private static void step(CommandSourceStack c, int ticks, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
  {
    cir.setReturnValue(subtick.commands.TickCommand.step(c, ticks, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
  }
}
