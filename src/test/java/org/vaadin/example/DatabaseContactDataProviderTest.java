package org.vaadin.example;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DatabaseContactDataProviderTest {

    @Mock
    Connection connection;

    @Mock
    PreparedStatement preparedStatement;

    @Mock
    ResultSet resultSet;

    @InjectMocks
    DatabaseContactDataProvider provider;

    Contact contact;

    @BeforeEach
    void setUp() throws Exception {
        provider = new DatabaseContactDataProvider(connection);
        contact = new Contact();
        contact.setName("John");
        contact.setStreet("123 St");
        contact.setCity("City");
        contact.setCountry("Country");
        contact.setEmail("john@example.com");
        contact.setPhone("1234567890");
        contact.setLastModified(Instant.now());
    }

    @Test
    void persist_insert_success() throws Exception {
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        provider.persist(contact); // No ID set

        verify(preparedStatement).executeUpdate();
        assertEquals(1, contact.getId());
    }

    @Test
    void persist_update_success() throws Exception {
        contact.setId(5);

        Instant now = Instant.now();
        contact.setLastModified(now);

        when(connection.prepareStatement(startsWith("SELECT"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(5);
        when(resultSet.getString("name")).thenReturn("John Doe");
        when(resultSet.getString("street")).thenReturn("Main Street");
        when(resultSet.getString("city")).thenReturn("New York");
        when(resultSet.getString("country")).thenReturn("USA");
        when(resultSet.getString("phone")).thenReturn("1234567890");
        when(resultSet.getString("email")).thenReturn("john@example.com");
        when(resultSet.getTimestamp("last_modified")).thenReturn(Timestamp.from(now));

        when(connection.prepareStatement(startsWith("UPDATE"))).thenReturn(preparedStatement);
        provider.persist(contact);

        verify(preparedStatement).executeUpdate();
    }

    @Test
    void find_success() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("name")).thenReturn("John");
        when(resultSet.getString("street")).thenReturn("123 St");
        when(resultSet.getString("city")).thenReturn("City");
        when(resultSet.getString("country")).thenReturn("Country");
        when(resultSet.getString("email")).thenReturn("john@example.com");
        when(resultSet.getString("phone")).thenReturn("1234567890");
        when(resultSet.getTimestamp("last_modified")).thenReturn(Timestamp.from(Instant.now()));

        Optional<Contact> result = provider.find(1);
        assertTrue(result.isPresent());
        assertEquals("John", result.get().getName());
    }

    @Test
    void find_not_found() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<Contact> result = provider.find(999);
        assertFalse(result.isPresent());
    }

    @Test
    void delete_success() throws Exception {
        contact.setId(1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        provider.delete(contact);

        verify(preparedStatement).executeUpdate();
    }

    @Test
    void findAllContacts_success() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("name")).thenReturn("John");
        when(resultSet.getString("street")).thenReturn("123 St");
        when(resultSet.getString("city")).thenReturn("City");
        when(resultSet.getString("country")).thenReturn("Country");
        when(resultSet.getString("email")).thenReturn("john@example.com");
        when(resultSet.getString("phone")).thenReturn("1234567890");
        when(resultSet.getTimestamp("last_modified")).thenReturn(Timestamp.from(Instant.now()));

        assertEquals(1, provider.findAllContacts().size());
    }
}
