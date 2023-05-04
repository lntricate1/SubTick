package subtick;

public class TickingMode
{
  private final String commandKey, name, nameMultiple;

  public TickingMode(String commandKey, String name, String nameMultiple)
  {
    this.commandKey = commandKey;
    this.name = name;
    this.nameMultiple = nameMultiple;
  }

  public String getCommandKey()
  {
    return commandKey;
  }

  public String getName(int count)
  {
    return count == 1 ? name : nameMultiple;
  }
}
