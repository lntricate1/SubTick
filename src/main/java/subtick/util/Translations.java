package subtick.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import subtick.Settings;
import subtick.SubTick;
import subtick.TickHandler;
import subtick.TickPhase;
import subtick.queues.TickingQueue;

public class Translations
{
  // private final static Map<String, String> translations = new HashMap<>();
  private final static Map<String, Map<String, String>> translations = new HashMap<>();

  public static void update(String lang)
  {
    InputStream langFile = Translations.class.getClassLoader().getResourceAsStream("assets/subtick/lang/%s.json".formatted(lang));
    if(langFile == null)
      return;

    String jsonData;
    try
    {
      jsonData = IOUtils.toString(langFile, StandardCharsets.UTF_8);
    }
    catch(IOException e)
    {
      return;
    }

    Gson gson = new GsonBuilder().setLenient().create();
    Map<String, String> map = gson.fromJson(jsonData, new TypeToken<Map<String, String>>(){}.getType());
    Map<String, String> map1 = new HashMap<>();
    for(Map.Entry<String, String> entry : map.entrySet())
      if(entry.getKey().startsWith("carpet."))
        map1.put(entry.getKey().substring(7), entry.getValue());
      else
        map1.put(entry.getKey(), entry.getValue());
    translations.put(lang, map1);
  }

  public static String tr(String key)
  {
    String lang = CarpetSettings.language.equals("none") ? "en_us" : CarpetSettings.language;
    return translations.containsKey(lang) ?
      translations.get(lang).getOrDefault(key, key) : key;
  }

  public static Map<String, String> getTranslationFromResourcePath(String lang)
  {
    // gnembon why??
    update("en_us");
    update(lang);
    return translations.containsKey(lang) ?
      Map.copyOf(translations.get(lang)) :
      new HashMap<String, String>();
  }

  public static String[] tr(String key, Level level)
  {
    String t = (key.contains(".err") ? Settings.subtickErrorFormat : Settings.subtickTextFormat) + " ";
    String tr = t + tr(key);

    t = "\0" + t;
    if(level != null)
      tr = tr.replace("{dim}", "\0" + dim(level) + t);

    return tr.split("\0");
  }

  public static String[] tr(String key, TickHandler handler, Integer n)
  {
    String t = (key.contains(".err") ? Settings.subtickErrorFormat : Settings.subtickTextFormat) + " ";
    String tr = t + tr(key);

    t = "\0" + t;
    tr = tr.replace("{dim}", "\0" + dim(handler.level) + t);
    tr = tr.replace("{phase}", "\0" + phase(handler.current_phase) + t);
    if(n != null)
      tr = tr.replace("{n}", "\0" + n(n) + t);

    return tr.split("\0");
  }

  public static String[] tr(String key, Level level, TickPhase phase, Integer n)
  {
    String t = (key.contains(".err") ? Settings.subtickErrorFormat : Settings.subtickTextFormat) + " ";
    String tr = t + tr(key);

    t = "\0" + t;
    if(level != null)
      tr = tr.replace("{dim}", "\0" + dim(level) + t);
    if(phase != null)
      tr = tr.replace("{phase}", "\0" + phase(phase) + t);
    if(n != null)
      tr = tr.replace("{n}", "\0" + n(n) + t);

    return tr.split("\0");
  }

  public static String[] tr(String key, Level level, TickingQueue queue, Integer n)
  {
    String t = t(key.contains(".err"));
    String tr = t + tr(key);

    t = "\0" + t;
    if(level != null)
      tr = tr.replace("{dim}", "\0" + dim(level) + t);
    if(queue != null)
    {
      tr = tr.replace("{queue}", "\0" + queue(queue) + t);
      tr = tr.replace("{queues}", "\0" + queues(queue) + t);
    }
    if(n != null)
      tr = tr.replace("{n}", "\0" + n(n) + t);

    return tr.split("\0");
  }

  public static void m(CommandSourceStack source, String key, Level level)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, level));
  }

  public static void m(CommandSourceStack source, String key, TickHandler handler)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, handler, null));
  }

  public static void m(CommandSourceStack source, String key, Level level, TickPhase phase)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, level, phase, null));
  }

  public static void m(CommandSourceStack source, String key, Level level, TickPhase phase, int n)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, level, phase, n));
  }

  public static void m(CommandSourceStack source, String key, Level level, TickingQueue queue)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, level, queue, null));
  }

  public static void m(CommandSourceStack source, String key, Level level, TickingQueue queue, int n)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, level, queue, n));
  }

  public static String t(boolean err)
  {
    return err ? Settings.subtickErrorFormat + " " : Settings.subtickTextFormat + " ";
  }

  public static String n(int x)
  {
    return Settings.subtickNumberFormat + " " + x;
  }

  public static String phase(TickPhase phase)
  {
    return Settings.subtickPhaseFormat + " " + phase.getName();
  }

  public static String queue(TickingQueue queue)
  {
    return Settings.subtickPhaseFormat + " " + queue.getName();
  }

  public static String queues(TickingQueue queue)
  {
    return Settings.subtickPhaseFormat + " " + queue.getNamePlural();
  }

  public static String dim(Level level)
  {
    ResourceLocation location = level.dimension().location();
    return Settings.subtickDimensionFormat + " " + location.getPath().substring(0, 1).toUpperCase() + location.getPath().substring(1)
      + "\0^" + Settings.subtickDimensionFormat + " " + location.toString();
  }
}
