package subtick;

public class TickingMode
{
  private final String name, nameMultiple;

  public TickingMode(String name, String nameMultiple)
  {
    this.name = name;
    this.nameMultiple = nameMultiple;
  }

  @Deprecated
  public String getName(int count)
  {
    return count == 1 ? name : nameMultiple;
  }

  public String getName()
  {
    return name;
  }

  public String getNamePlural()
  {
    return nameMultiple;
  }
}
