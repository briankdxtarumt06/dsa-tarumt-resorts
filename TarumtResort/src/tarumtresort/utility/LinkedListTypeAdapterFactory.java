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

public class LinkedListTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<?> rawType = typeToken.getRawType();

        if (!LinkedListInterface.class.isAssignableFrom(rawType)) {
            return null; // not our type, let Gson handle it normally
        }

        Type type = typeToken.getType();
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalStateException(
                "LinkedListInterface field must be parameterized, e.g. LinkedListInterface<Reservation>");
        }

        Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
        TypeAdapter<?> elementAdapter = gson.getAdapter(TypeToken.get(elementType));

        return (TypeAdapter<T>) newLinkedListAdapter(elementAdapter);
    }

    @SuppressWarnings("unchecked")
    private <E extends Comparable<E>> TypeAdapter<LinkedListInterface<E>> newLinkedListAdapter(TypeAdapter<?> rawElementAdapter) {
        TypeAdapter<E> elementAdapter = (TypeAdapter<E>) rawElementAdapter;

        return new TypeAdapter<LinkedListInterface<E>>() {

            @Override
            public void write(JsonWriter out, LinkedListInterface<E> value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                out.beginArray();
                for (int i = 0; i < value.size(); i++) {
                    elementAdapter.write(out, value.get(i));
                }
                out.endArray();
            }

            @Override
            public LinkedListInterface<E> read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return new LinkedList<E>();
                }
                LinkedListInterface<E> list = new LinkedList<E>();
                in.beginArray();
                while (in.hasNext()) {
                    list.addBack(elementAdapter.read(in));
                }
                in.endArray();
                return list;
            }
        };
    }
}