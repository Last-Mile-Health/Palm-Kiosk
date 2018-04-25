package org.lastmilehealth.kiosk;

import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Thamizhan on 23/02/17.
 */

public class LogUtils {

    private static final String LOG_FILENAME = "exit_log.txt";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy - hh:mm aa", Locale.getDefault());

    private static File logFile(Context context) {
        return new File(ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_DOWNLOADS)[0].getAbsolutePath(), LOG_FILENAME);
    }

    static void writeLog(Context context) {
        File logFile = logFile(context);
        String logText = SIMPLE_DATE_FORMAT.format(new Date());

        // create new file if not exist
        try {
            if (logFile.exists())
                logFile.createNewFile();

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
            bufferedWriter.append(logText);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String[] readLogs(Context context) {
        File logFile = logFile(context);
        ArrayList<String> logs = new ArrayList<>();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(logFile));
            for (String logLine = bufferedReader.readLine(); logLine != null; logLine = bufferedReader.readLine())
                logs.add(logLine);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logs.toArray(new String[logs.size()]);
    }

    static void clearLogs(Context context) {
        File logFile = logFile(context);
        logFile.delete();
    }
}
