package classes;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;

public class Logger
{
    public static void logMessage(String output)
    {
        log(LogLevel.message, output);
    }

    public static void logDebug(String output)
    {
        log(LogLevel.debug, output);
    }

    public static void logWarning(String output)
    {
        log(LogLevel.warning, output);
    }

    public static void logError(String output)
    {
        log(LogLevel.error, output);
    }

    public static void logFatal(String output)
    {
        log(LogLevel.fatal, output);
    }

    public static void log(LogLevel logLevel, String output)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        output = String.format("[%s %s]: %s\n", logLevel.name().toUpperCase(Locale.ROOT), sdf.format(calendar.getTime()), output);
        System.out.println(output);

        try
        {
            Path logLocation = Paths.get(System.getProperty("user.dir"), "Log.log");
            if (!Files.exists(logLocation)) Files.createFile(logLocation);
            Files.write(logLocation, output.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }
        catch (Exception e)
        {
            System.err.println("Failed to write to Log");
        }
    }

    public static String generateIdentifier()
    {
        int[][] Ranges = {{48, 57}, {60, 91}, {97, 125}};
        Random test = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 20; i++)
        {
            int set = test.nextInt(Ranges.length);
            builder.append((char) (test.nextInt(Ranges[set][1] - Ranges[set][0]) + Ranges[set][0]));
        }
        return builder.toString();
    }
}
