package tarumtresort.utility;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import tarumtresort.adt.LinkedList;
import tarumtresort.adt.LinkedListInterface;

/**
 * Gson adapter factory that lets the custom LinkedList ADT be embedded in
 * entity JSON (e.g. Guest.notificationList inside guests.json).
 * A LinkedListInterface field is written as a plain JSON array of its
 * elements (never the Node chain) and read back into a fresh LinkedList.
 * Raw types are used internally because LinkedList requires Comparable
 * elements; only size/get/addBack are touched.
 */
public class LinkedListTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!LinkedListInterface.class.isAssignableFrom(type.getRawType())) {
            return null;
        }

        // element type, e.g. LinkedListInterface<Notification> -> Notification
        Type elementType = Object.class;
        Type declared = type.getType();
        if (declared instanceof ParameterizedType) {
            elementType = ((ParameterizedType) declared).getActualTypeArguments()[0];
        }
        TypeAdapter<Object> elementAdapter = (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(elementType));

        return (TypeAdapter<T>) new TypeAdapter<LinkedListInterface>() {
            @Override
            public void write(JsonWriter out, LinkedListInterface list) throws IOException {
                if (list == null) {
                    out.nullValue();
                    return;
                }
                out.beginArray();
                for (int i = 0; i < list.size(); i++) {
                    elementAdapter.write(out, list.get(i));
                }
                out.endArray();
            }

            @Override
            public LinkedListInterface read(JsonReader in) throws IOException {
                LinkedList result = new LinkedList();
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return result;
                }
                in.beginArray();
                while (in.hasNext()) {
                    result.addBack((Comparable) elementAdapter.read(in));
                }
                in.endArray();
                return result;
            }
        };
    }
}
