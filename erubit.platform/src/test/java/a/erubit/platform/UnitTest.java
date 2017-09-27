package a.erubit.platform;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


import static org.junit.Assert.*;

public class UnitTest {
    private String readFile(File file) throws IOException {
        StringBuilder fileContents = new StringBuilder((int)file.length());
        String lineSeparator = System.getProperty("line.separator");

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine()).append(lineSeparator);
            }
            return fileContents.toString();
        }
    }

    @Test
    public void checkRawJson() throws Exception {
        File rawDir = new File("app/src/main/res/raw");
        File[] files = rawDir.listFiles((dir, name) -> name.matches(".*.json"));

        ArrayList<String> ids = new ArrayList<>(100);

        int c = 0;
        for (File file: files) {
            String json = readFile(file);
            JsonObject jo = new JsonParser().parse(json).getAsJsonObject();

            assertNotNull(file.getName() + " not a valid json", jo);

            String id = jo.get("id").getAsString();

            assertNotNull(file.getName() + " 'id' missed", id);
            assertFalse(file.getName() + " id repeated", ids.contains(id));

            ids.add(id);
            c++;
        }

        System.out.println(c + " json files checked.");
    }
}