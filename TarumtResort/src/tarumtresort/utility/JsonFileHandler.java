package tarumtresort.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

public class JsonFileHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(new LinkedListTypeAdapterFactory()).create();

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

    // save list of entities to json file
    public static <T extends Comparable<T>> void saveList(LinkedListInterface<T> list, Path file) throws IOException {
        Object[] snapshot = new Object[list == null ? 0 : list.size()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = list.get(i);
        }
        save(snapshot, file);
    }

    // load list of entities from json file
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

    /**
     * Saves a list of entities where one nested field holds a list of other
     * entities: only their ids are written to disk (no duplicate data).
     * Shared by any entity type with a nested entity list.
     *
     * @param list        the entity list to save
     * @param file        target json file
     * @param fieldName   name of the nested field in the entity class
     * @param listGetter  extracts the nested entity list from an entity
     * @param idGetter    extracts the id from a nested entity
     */
    public static <T extends Comparable<T>, E extends Comparable<E>> void saveListWithNestedIds(
            LinkedListInterface<T> list, 
            Path file,
            String fieldName,
            Function<T, LinkedListInterface<E>> listGetter,
            Function<E, String> idGetter) throws IOException {

        List<JsonObject> objects = new ArrayList<>();
        // iterate through list of entity
        for (int i = 0; i < list.size(); i++) {
            T entity = list.get(i);
            JsonObject json = GSON.toJsonTree(entity).getAsJsonObject();

            // replace the nested entity list with its plain id list
            JsonArray ids = new JsonArray();
            LinkedListInterface<E> nested = listGetter.apply(entity); // get the nested list of entity
            if (nested != null) {
                // iterate through nested list of entity
                for (int j = 0; j < nested.size(); j++) {
                    E nestedEntity = nested.get(j);
                    if (nestedEntity != null) {
                        ids.add(idGetter.apply(nestedEntity)); // get the id of entity inside nested list
                    }
                }
            }
            json.remove(fieldName);
            json.add(fieldName, ids);

            objects.add(json);
        }
        save(objects, file);
    }

    /**
     * Loads a list of entities whose nested id list is resolved back into
     * full entities, fetching each entity from its json file via the resolver.
     *
     * @param file         source json file
     * @param entityType   the entity class
     * @param fieldName    name of the nested field in the entity class
     * @param idResolver   resolves an id to the full nested entity
     * @param listSetter   attaches the resolved entity list back onto the entity
     */
    public static <T extends Comparable<T>, E extends Comparable<E>> LinkedList<T> loadListWithNestedIds(
            Path file, 
            Class<T> entityType,
            String fieldName,
            Function<String, E> idResolver,
            BiConsumer<T, LinkedListInterface<E>> listSetter) throws IOException {

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
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject json = element.getAsJsonObject();

                // extract the nested id list and resolve each id to a full entity
                LinkedListInterface<E> nested = new LinkedList<>();
                JsonElement nestedElement = json.remove(fieldName);
                if (nestedElement != null && nestedElement.isJsonArray()) {
                    for (JsonElement idElement : nestedElement.getAsJsonArray()) {
                        if (idElement.isJsonNull()) {
                            continue;
                        }
                        E entity = idResolver.apply(idElement.getAsString()); // load nested entity using entity id
                        if (entity != null) {
                            nested.addBack(entity);
                        }
                    }
                }

                T entity = GSON.fromJson(json, entityType);
                listSetter.accept(entity, nested); // set nested entity list of entity
                result.addBack(entity);
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

    // Gson adapter for LinkedListInterface
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
