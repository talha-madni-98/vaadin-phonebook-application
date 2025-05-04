package org.vaadin.example;

import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DatabaseContactDataProvider extends AbstractBackEndDataProvider<Contact, CrudFilter> implements ContactService {

    private Consumer<Long> sizeChangeListener;

    private final Connection connection;

    public DatabaseContactDataProvider(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void persist(Contact contact) {
        try {

            if (contact.getId() == null) {
                String insertSql = "INSERT INTO contacts (name, street, city, country, phone, email, last_modified) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    Instant now = Instant.now();
                    stmt.setString(1, contact.getName());
                    stmt.setString(2, contact.getStreet());
                    stmt.setString(3, contact.getCity());
                    stmt.setString(4, contact.getCountry());
                    stmt.setString(5, contact.getPhone());
                    stmt.setString(6, contact.getEmail());
                    stmt.setTimestamp(7, Timestamp.from(now));
                    stmt.executeUpdate();

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            contact.setId(generatedKeys.getInt(1));
                            contact.setLastModified(now);
                        }
                    }
                }
            } else {
                Contact existing = find(contact.getId()).orElseThrow(() ->
                        new IllegalArgumentException("Contact not found"));

                if (!existing.getLastModified().equals(contact.getLastModified())) {
                    throw new IllegalArgumentException("This contact was modified by another user!");
                }

                Instant now = Instant.now();
                String updateSql = "UPDATE contacts SET name=?, street=?, city=?, country=?, phone=?, email=?, last_modified=? WHERE id=?";
                try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                    stmt.setString(1, contact.getName());
                    stmt.setString(2, contact.getStreet());
                    stmt.setString(3, contact.getCity());
                    stmt.setString(4, contact.getCountry());
                    stmt.setString(5, contact.getPhone());
                    stmt.setString(6, contact.getEmail());
                    stmt.setTimestamp(7, Timestamp.from(now));
                    stmt.setInt(8, contact.getId());
                    stmt.executeUpdate();
                }
                contact.setLastModified(now);
            }

            ContactChangeBroadcaster.broadcast(contact);

        } catch (SQLException e) {
            throw new RuntimeException("Error while saving contact", e);
        }
    }

    @Override
    public Optional<Contact> find(Integer id) {
        String sql = "SELECT * FROM contacts WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while finding contact", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Contact> findById(Integer id) {
        return find(id);
    }

    @Override
    public void delete(Contact contact) {
        String sql = "DELETE FROM contacts WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, contact.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error while deleting contact", e);
        }

        ContactChangeBroadcaster.broadcast(contact);
    }

    private Contact mapRow(ResultSet rs) throws SQLException {
        Contact contact = new Contact();
        contact.setId(rs.getInt("id"));
        contact.setName(rs.getString("name"));
        contact.setStreet(rs.getString("street"));
        contact.setCity(rs.getString("city"));
        contact.setCountry(rs.getString("country"));
        contact.setPhone(rs.getString("phone"));
        contact.setEmail(rs.getString("email"));
        contact.setLastModified(rs.getTimestamp("last_modified").toInstant());
        return contact;
    }

    @Override
    protected Stream<Contact> fetchFromBackEnd(Query<Contact, CrudFilter> query) {
        int offset = query.getOffset();
        int limit = query.getLimit();

        Stream<Contact> stream = findAllContacts().stream();

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

    public List<Contact> findAllContacts() {
        List<Contact> contacts = new ArrayList<>();
        String sql = "SELECT * from contacts";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Contact contact = new Contact();
                contact.setId(rs.getInt("id"));
                contact.setName(rs.getString("name"));
                contact.setStreet(rs.getString("street"));
                contact.setCity(rs.getString("city"));
                contact.setCountry(rs.getString("country"));
                contact.setEmail(rs.getString("email"));
                contact.setPhone(rs.getString("phone"));
                contact.setLastModified(rs.getTimestamp("last_modified").toInstant());

                contacts.add(contact);
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Proper logging should be added
        }

        return contacts;
    }
}
