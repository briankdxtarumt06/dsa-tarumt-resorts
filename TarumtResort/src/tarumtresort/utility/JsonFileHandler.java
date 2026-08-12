package tarumtresort.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.IOError;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

public class JsonFileHandler {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeHierarchyAdapter(LinkedListInterface.class, new LinkedListInterfaceAdapter())
            .create();

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
        // exception handling
        String content = Files.readString(file).trim();
        if (content.isEmpty() || "null".equals(content)) {
            return null;
        }
        try {
            return GSON.fromJson(content, type);
        } catch (JsonParseException e) {
            return null;
        }
    }

    public static <T extends Comparable<T>> void saveList(LinkedListInterface<T> list, Path file) throws IOException {
        Object[] snapshot = new Object[list == null ? 0 : list.size()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = list.get(i);
        }
        save(snapshot, file);
    }

    public static <T extends Comparable<T>> LinkedList<T> loadList(Path file, Class<T> elementType) throws IOException {
        LinkedList<T> result = new LinkedList<>();
        try {
            // exception handling
            if (!Files.exists(file)) {
                return result;
            }
            String content = Files.readString(file).trim();
            if (content.isEmpty() || "null".equals(content)) {
                return result;
            }
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonArray()) {
                return result;
            }
            JsonArray array = parsed.getAsJsonArray();
            for (JsonElement element : array) {
                if (element.isJsonNull()) {
                    continue;
                }
                result.addBack(GSON.fromJson(element, elementType));
            }
        } catch (IOError | RuntimeException e) {
            return new LinkedList<>();
        }
        return result;
    }

    // Gson adapter for java.time.LocalDateTime
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toString());
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString());
        }
    }

    // Gson adapter for LinkedListInterface (serialized as a JSON array)
    private static class LinkedListInterfaceAdapter
            implements JsonSerializer<LinkedListInterface<?>>, JsonDeserializer<LinkedListInterface<?>> {

        @Override
        public JsonElement serialize(LinkedListInterface<?> src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();
            if (src != null) {
                for (int i = 0; i < src.size(); i++) {
                    array.add(context.serialize(src.get(i)));
                }
            }
            return array;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public LinkedListInterface<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            LinkedList result = new LinkedList();
            if (!json.isJsonArray()) {
                return result;
            }
            Type elementType = String.class;
            if (typeOfT instanceof ParameterizedType) {
                elementType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
            }
            for (JsonElement element : json.getAsJsonArray()) {
                if (element.isJsonNull()) {
                    continue;
                }
                result.addBack(context.deserialize(element, elementType));
            }
            return result;
        }
    }
}