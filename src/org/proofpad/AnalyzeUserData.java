package org.proofpad;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class UUIDConverter implements Converter {

    @Override
    public boolean canConvert(Class aClass) {
        return aClass.equals(UUID.class);
    }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        String val = hierarchicalStreamReader.getValue();
        return UUID.fromString(val);
    }
}

public class AnalyzeUserData {
    public static void main(String args[]) throws IOException {
        List<UserData> data = new ArrayList<UserData>();
        Map<String, Integer> useMap = new HashMap<String, Integer>();
        for (int i = 1; i <= 12; i++) {
            String filename = "/Users/calebegg/Code/ppuserdata/" + i + ".xml";
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String contents = br.readLine();
            XStream xs = new XStream(new StaxDriver());
            xs.registerConverter(new UUIDConverter());
            UserData ud = (UserData) xs.fromXML(contents);
            data.add(ud);
            Set<String> uses = new HashSet<String>();
            for (UserData.Use use : ud.uses) {
                uses.add(use.key + (use.keyboard ? "_k" : "_m"));
            }
            for (String key : uses) {
                int count = useMap.containsKey(key) ? useMap.get(key) : 0;
                count += 1;
                useMap.put(key, count);
            }
        }
        for (Map.Entry<String, Integer> entry : useMap.entrySet()) {
            if (entry.getValue() > 2) {
                System.out.println(entry);
            }
        }
    }
}
