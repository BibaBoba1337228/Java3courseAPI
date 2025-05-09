package course.project.API.dto.converters;

import org.modelmapper.AbstractConverter;
import org.hibernate.collection.spi.PersistentBag;
import java.util.HashSet;
import java.util.Set;

// Это чтобы коллекции в DTO нормально конвертились, а то там ошибка стреляет (как можно заметить это для SET'ов, если нужен будет List, надо просто еще один сделать)
public class PersistentBagToSetConverter<T> extends AbstractConverter<PersistentBag, Set<T>> {
    @Override
    protected Set<T> convert(PersistentBag source) {
        if (source == null) {
            return null;
        }
        return new HashSet<>(source);
    }
} 