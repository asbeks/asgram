package org.telegram.messenger.asgram;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Editorial additions must be explicitly named in the main channel's ASGRAM line. */
public final class AsgramDirectory {
    public static final class Entry {
        public final String username,title,category;
        public Entry(String username,String title,String category){this.username=username;this.title=title;this.category=category;}
    }
    public static List<String> declaredChannels(String about) {
        LinkedHashSet<String> result=new LinkedHashSet<>();
        if(about==null)return new ArrayList<>();
        for(String line:about.split("\\r?\\n")) {
            if(!line.trim().matches("(?i)^ASGRAM\\s*:.*"))continue;
            Matcher matcher=Pattern.compile("@([a-zA-Z][a-zA-Z0-9_]{4,31})(?![a-zA-Z0-9_])").matcher(line);
            while(matcher.find()&&result.size()<8) {
                String name=matcher.group(1);
                if(!name.equalsIgnoreCase("ABDII_SABAA")&&!name.equalsIgnoreCase("abdii_sabaa_university"))result.add(name.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return new ArrayList<>(result);
    }
    private AsgramDirectory(){}
}
