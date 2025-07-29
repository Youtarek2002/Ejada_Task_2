package services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class SearchService<T> {
    public T searchById(List<T> list, String id, Function<T, String> idGetter) {
        for (T item : list) {
            if (idGetter.apply(item).equals(id)) return item;
        }
        return null;
    }

    public List<T> searchByName(List<T> list, String name, Function<T, String> nameGetter) {
        List<T> results = new ArrayList<>();
        for (T item : list) {
            if (nameGetter.apply(item).toLowerCase().contains(name.toLowerCase())) {
                results.add(item);
            }
        }
        return results;
    }
}