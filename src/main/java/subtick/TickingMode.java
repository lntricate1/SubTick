package subtick;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;

public class TickingMode
{
  public static final TickingMode DEFAULT = new TickingMode("default", "Default");

  public static final Dynamic2CommandExceptionType INVALID_MODE_EXCEPTION = new Dynamic2CommandExceptionType((phase, key) -> new LiteralMessage("Invalid mode '" + key + "' for phase " + phase));

  private final String commandKey;
  private final String name;

  public TickingMode(String commandKey, String name)
  {
    this.commandKey = commandKey;
    this.name = name;
  }

  public String getCommandKey()
  {
    return commandKey;
  }

  public String getName()
  {
    return name;
  }

  public static TickingMode byCommandKey(String phaseKey, String key) throws CommandSyntaxException
  {
    return byCommandKey(TickPhase.byCommandKey(phaseKey), key);
  }

  public static TickingMode byCommandKey(TickPhase phase, String key) throws CommandSyntaxException
  {
    TickingMode mode = phase.getMode(key);
    if (mode == null)
      throw INVALID_MODE_EXCEPTION.create(phase, key);
    return mode;
  }
}
