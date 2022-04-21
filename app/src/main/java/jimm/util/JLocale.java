/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/util/ResourceBundle.java
 Version: 0.7.1m  Date: 21.04.2022
 Author(s): Manuel Linsmayer, Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/

package jimm.util;

import android.util.Log;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import jimm.comm.Config;
import protocol.net.TcpSocket;
import ru.net.jimm.JimmApplication;

public class JLocale {
    // List of available language packs
    public static String[] langAvailable;
    public static String[] langAvailableName;
    // Resource hashtable
    static private Hashtable resources = new Hashtable();
    private static String currentLanguage;

    public static void loadLanguageList() {
        Config config = new Config().load("/langlist.txt");
        langAvailable = config.getKeys();
        langAvailableName = config.getValues();
    }
    
    // Get user interface language/localization for current session
    public static String getCurrUiLanguage() {
        return currentLanguage;
    }
    public static String getLanguageCode() {
        String country = getCurrUiLanguage();
        int separatorIndex = country.indexOf('_');
        if (-1 != separatorIndex) {
            country = country.substring(0, separatorIndex);
        }
        return country.toLowerCase();
    }
    public static String getSystemLanguage() {
        String lang = System.getProperty("microedition.locale");
        lang = ((null == lang) ? "" : lang).toUpperCase();
        for (int i = 0; i < langAvailable.length; ++i) {
            if (-1 != lang.indexOf(langAvailable[i])) {
                return langAvailable[i];
            }
        }
        return langAvailable[0];
    }
    public static boolean isCyrillic(String language) {
        return (-1 != language.indexOf("RU")) // Russian
                || (-1 != language.indexOf("TT")) // Tatar
                || "UA".equals(language) // Ukrainian
                || "AD".equals(language)
                || "BE".equals(language); // Belarusian
    }
    
    // Set user interface language/localization for current session
    public static void setCurrUiLanguage(String currUiLanguage) {
        String language = JLocale.langAvailable[0];
        for (int i = 0; i < JLocale.langAvailable.length; ++i) {
            if (langAvailable[i].equals(currUiLanguage)) {
                language = langAvailable[i];
                break;
            }
        }
        currentLanguage = language;
        loadLang();
    }

    @Deprecated
    private static void loadLang(String lang) {
        InputStream istream = null;
        try {
            //resources = new Hashtable();
            //istream = jimm.Jimm.getResourceAsStream("/" + lang + ".lng");
            Log.d("loadLang", "lang: " + lang);
            istream = JimmApplication.application.getAssets().open(lang + ".lng");
            DataInputStream dos = new DataInputStream(istream);
            int size = dos.readShort();
            Log.d("loadLang", "size: " + size);
            for (int j = 0; j < size; ++j) {
                String s = dos.readUTF();
                String s2 = dos.readUTF();
                Log.d("loadLang", "load: " + s + " => " + s2);
                resources.put(s, s2);
            }
            dos.close();
        } catch (Exception ignored) {
            Log.d("loadLang", ignored.getLocalizedMessage());
        }
        TcpSocket.close(istream);
    }
    private static void loadLang() {
        loadLang(currentLanguage);
        if (resources.isEmpty()) {
            loadLang("EN");
        }
    }
    
    // Get string from active language pack
    public static String getString(String key) {
        if (null == key) return null;

        String packageName = JimmApplication.application.getPackageName();
        int resId = JimmApplication.application.getResources().getIdentifier(key, "string", packageName);
        String byKey = key;
        try {
            byKey = JimmApplication.application.getString(resId);
        }catch (Exception ignored){}

        return byKey;

        // TODO: j2me
        //String value = (String) resources.get(key);
        //return (null == value) ? key : value;
    }
    
    public static String getEllipsisString(String key) {
        return getString(key) + "...";
    }
}

