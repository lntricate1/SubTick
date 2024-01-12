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
import subtick.Settings;
import subtick.TickPhase;
import subtick.queues.TickingQueue;

//#if MC >= 11900
//$$ import java.util.Collections;
//$$ import net.minecraft.resources.ResourceLocation;
//$$ import net.minecraft.world.level.Level;
//$$ import subtick.TickHandler;
//#endif

public class Translations
{
  //#if MC >= 11900
  //$$ public static Map<String, String> getTranslationFromResourcePath(String lang)
  //$$ {
  //$$   InputStream langFile = Translations.class.getClassLoader().getResourceAsStream("assets/subtick/lang/%s.json".formatted(lang));
  //$$   if(langFile == null)
  //$$   {
  //$$     return Collections.emptyMap();
  //$$   }
  //$$   String jsonData;
  //$$   try
  //$$   {
  //$$     jsonData = IOUtils.toString(langFile, StandardCharsets.UTF_8);
  //$$   }
  //$$   catch(IOException e)
  //$$   {
  //$$     return Collections.emptyMap();
  //$$   }
  //$$   Gson gson = new GsonBuilder().setLenient().create();
  //$$   return gson.fromJson(jsonData, new TypeToken<Map<String, String>>(){}.getType());
  //$$ }
  //$$
  //$$ // compat
  //$$ public static String tr(String key)
  //$$ {
  //$$   return carpet.utils.Translations.tr(key);
  //$$ }
  //#else
  private final static Map<String, Map<String, String>> translations = new HashMap<>();

  public static Map<String, String> getTranslationFromResourcePath(String lang)
  {
    // gnembon why??
    update("en_us");
    update(lang);
    return translations.containsKey(lang) ?
      Map.copyOf(translations.get(lang)) :
      new HashMap<String, String>();
  }

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
  //#endif

  public static String[] tr(String key, TickPhase phase, Integer n)
  {
    String t = (key.contains(".err") ? Settings.subtickErrorFormat : Settings.subtickTextFormat) + " ";
    String tr = t + tr(key);

    t = "\0" + t;
    if(phase != null)
      tr = tr.replace("{dim}", "\0" + dim(phase) + t).replace("{phase}", "\0" + phase(phase) + t);
    if(n != null)
      tr = tr.replace("{n}", "\0" + n(n) + t);

    return tr.split("\0");
  }

  public static String[] tr(String key, TickPhase phase)
  {
    String t = (key.contains(".err") ? Settings.subtickErrorFormat : Settings.subtickTextFormat) + " ";
    String tr = t + tr(key);

    t = "\0" + t;
    if(phase != null)
      tr = tr.replace("{dim}", "\0" + dim(phase) + t).replace("{phase}", "\0" + phase(phase) + t);

    return tr.split("\0");
  }


  public static String[] tr(String key, TickingQueue queue, Integer n)
  {
    String t = t(key.contains(".err"));
    String tr = t + tr(key);

    t = "\0" + t;
    if(queue != null)
    {
      tr = tr.replace("{queue}", "\0" + queue(queue) + t);
      tr = tr.replace("{queues}", "\0" + queues(queue) + t);
    }
    if(n != null)
      tr = tr.replace("{n}", "\0" + n(n) + t);

    return tr.split("\0");
  }

  public static void m(CommandSourceStack source, String key)
  {
    Messenger.m(source, t(key.contains(".err")) + tr("subtick.feedback." + key));
  }

  public static void m(CommandSourceStack source, String key, TickPhase phase)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, phase));
  }

  public static void m(CommandSourceStack source, String key, TickPhase phase, int n)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, phase, n));
  }

  public static void m(CommandSourceStack source, String key, TickingQueue queue)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, queue, null));
  }

  public static void m(CommandSourceStack source, String key, TickingQueue queue, int n)
  {
    Messenger.m(source, (Object[])tr("subtick.feedback." + key, queue, n));
  }

  public static String t(boolean err)
  {
    return err ? Settings.subtickErrorFormat + " " : Settings.subtickTextFormat + " ";
  }

  public static String n(int x)
  {
    return Settings.subtickNumberFormat + " " + x;
  }

  public static String queue(TickingQueue queue)
  {
    return Settings.subtickPhaseFormat + " " + queue.getName();
  }

  public static String queues(TickingQueue queue)
  {
    return Settings.subtickPhaseFormat + " " + queue.getNamePlural();
  }

  public static String phase(TickPhase phase)
  {
    return Settings.subtickPhaseFormat + " " + phase.getPhaseName();
  }

  public static String dim(TickPhase phase)
  {
    String path = phase.getPath();
    return Settings.subtickDimensionFormat + " " + path.substring(0, 1).toUpperCase() + path.substring(1)
      + "\0^" + Settings.subtickDimensionFormat + " " + path;
  }
}
