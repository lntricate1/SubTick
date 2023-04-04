package subtick.g4mespeed;

import com.g4mesoft.GSExtensionInfo;
import com.g4mesoft.GSExtensionUID;
import com.g4mesoft.GSIExtension;
import com.g4mesoft.core.GSVersion;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.core.server.GSServerController;
import com.g4mesoft.packet.GSIPacket;
import com.g4mesoft.registry.GSSupplierRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import subtick.SubTick;

public class SubtickExtension implements GSIExtension
{
  public static final String NAME = SubTick.MOD_NAME;
  // "STCK" in unicode as hex
  public static final GSExtensionUID UID = new GSExtensionUID(0x83846775);
  public static final GSVersion VERSION = decodeVersionString();

  public static final GSExtensionInfo INFO = new GSExtensionInfo(NAME, UID, VERSION);

  @Override
  public GSExtensionInfo getInfo()
  {
    return INFO;
  }

  @Override
  public String getTranslationPath()
  {
    return "";
  }

  @Override public void init()
  {}

  @Environment(EnvType.CLIENT)
  private SubtickModule serverModule;

  @Override
  public void addServerModules(GSServerController controller)
  {
    if(serverModule == null)
      serverModule = new SubtickModule();
    controller.addModule(serverModule);
  }

  @Override
  public void addClientModules(GSClientController controller)
  {}

  @Override
  public void registerPackets(GSSupplierRegistry<Integer, GSIPacket> registry)
  {}

  private static GSVersion decodeVersionString()
  {
    int majorVersion = 1, minorVersion = 0, patchVersion = 0;

    String[] args = SubTick.MOD_VERSION.split("\\.");
    if (args.length == 3) {
      try {
        majorVersion = Integer.parseInt(args[0]);
        minorVersion = Integer.parseInt(args[1]);
        patchVersion = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
      }
    }

    return new GSVersion(majorVersion, minorVersion, patchVersion);
  }
}
