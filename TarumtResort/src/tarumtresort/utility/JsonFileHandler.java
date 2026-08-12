package tarumtresort.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

public class JsonFileHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonFileHandler() { 
    }

    public static <T> void save(T data, Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, GSON.toJson(data));
    }

    public static <T> T load(Path file, Class<T> type) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        return GSON.fromJson(Files.readString(file), type);
    }

    public static <T extends Comparable<T>> void saveList(LinkedListInterface<T> list, Path file) throws IOException {
        Object[] snapshot = new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
            snapshot[i] = list.get(i);
        }
        save(snapshot, file);
    }

    public static <T extends Comparable<T>> LinkedList<T> loadList(Path file, Class<T> elementType) throws IOException {
        LinkedList<T> result = new LinkedList<>();
        if (!Files.exists(file)) {
            return result;
        }
        JsonArray array = JsonParser.parseString(Files.readString(file)).getAsJsonArray();
        for (JsonElement element : array) {
            result.addBack(GSON.fromJson(element, elementType));
        }
        return result;
    }
}
