package org.vaadin.example;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;

import static java.util.Comparator.naturalOrder;

// Contact data provider
public class ContactDataProvider
        extends AbstractBackEndDataProvider<Contact, CrudFilter> implements ContactService{

    static final Map<Integer, Contact> DATABASE = new ConcurrentHashMap<>(getData());

    private Consumer<Long> sizeChangeListener;

    @Override
    protected Stream<Contact> fetchFromBackEnd(Query<Contact, CrudFilter> query) {
        int offset = query.getOffset();
        int limit = query.getLimit();

        Stream<Contact> stream = DATABASE.values().stream();

        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()))
                    .sorted(comparator(query.getFilter().get()));
        }

        return stream.skip(offset).limit(limit);
    }

    @Override
    protected int sizeInBackEnd(Query<Contact, CrudFilter> query) {
        // For RDBMS just execute a SELECT COUNT(*) ... WHERE query
        long count = fetchFromBackEnd(query).count();

        if (sizeChangeListener != null) {
            sizeChangeListener.accept(count);
        }

        return (int) count;
    }

    private static Predicate<Contact> predicate(CrudFilter filter) {
        // For RDBMS just generate a WHERE clause
        return filter.getConstraints().entrySet().stream()
                .map(constraint -> (Predicate<Contact>) Contact -> {
                    try {
                        Object value = valueOf(constraint.getKey(), Contact);
                        return value != null && value.toString().toLowerCase()
                                .contains(constraint.getValue().toLowerCase());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }).reduce(Predicate::and).orElse(e -> true);
    }

    private static Comparator<Contact> comparator(CrudFilter filter) {
        // For RDBMS just generate an ORDER BY clause
        return filter.getSortOrders().entrySet().stream().map(sortClause -> {
            try {
                Comparator<Contact> comparator = Comparator.comparing(
                        Contact -> (Comparable) valueOf(sortClause.getKey(),
                                Contact));

                if (sortClause.getValue() == SortDirection.DESCENDING) {
                    comparator = comparator.reversed();
                }

                return comparator;

            } catch (Exception ex) {
                return (Comparator<Contact>) (o1, o2) -> 0;
            }
        }).reduce(Comparator::thenComparing).orElse((o1, o2) -> 0);
    }

    private static Object valueOf(String fieldName, Contact Contact) {
        try {
            Field field = Contact.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(Contact);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void persist(Contact item) {
        if (item.getId() == null) {
            item.setId(DATABASE.keySet().stream().max(Integer::compareTo).orElse(0) + 1);
        }

        boolean emailExists = DATABASE.values().stream()
                .anyMatch(contact -> !contact.getId().equals(item.getId()) && contact.getEmail().equals(item.getEmail()));

        if (emailExists) {
            throw new IllegalArgumentException("Email already exists!");
        }

        boolean phoneExists = DATABASE.values().stream()
                .anyMatch(contact -> !contact.getId().equals(item.getId()) && contact.getPhone().equals(item.getPhone()));

        if (phoneExists) {
            throw new IllegalArgumentException("Phone number already exists!");
        }

        final Optional<Contact> existingItem = find(item.getId());
        if (existingItem.isPresent()) {
            Contact existingContact = existingItem.get();

            if (!existingContact.getLastModified().equals(item.getLastModified())) {
                throw new IllegalArgumentException("This contact was modified by another user!");
            }
        }
        item.setLastModified(Instant.now());
        DATABASE.put(item.getId(), item);
        ContactChangeBroadcaster.broadcast(item);
    }

    @Override
    public Optional<Contact> find(Integer id) {
        return Optional.ofNullable(DATABASE.get(id));
    }

    @Override
    public void delete(Contact item) {
        DATABASE.remove(item.getId());
        ContactChangeBroadcaster.broadcast(item);
    }

    public static Map<Integer, Contact> getData(){
        Map<Integer, Contact> contactMap = new ConcurrentHashMap<>();
        contactMap.put(1, new Contact(1, "Alice Johnson", "123 Maple St", "Los Angeles", "USA", "2134567890", "alice.johnson@example.com"));
        contactMap.put(2, new Contact(2, "Bob Smith", "45 King Road", "Chicago", "USA", "3129876543", "bob.smith@example.com"));
        contactMap.put(3, new Contact(3, "Charlie Davis", "78 Oak Avenue", "Houston", "USA", "7134563210", "charlie.davis@example.com"));
        return contactMap;
    }
}