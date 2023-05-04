package subtick;

public class TickingMode
{
  private final String name, nameMultiple;

  public TickingMode(String name, String nameMultiple)
  {
    this.name = name;
    this.nameMultiple = nameMultiple;
  }

  public String getName(int count)
  {
    return count == 1 ? name : nameMultiple;
  }
}
