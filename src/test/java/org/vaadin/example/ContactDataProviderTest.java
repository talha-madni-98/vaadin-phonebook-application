package org.vaadin.example;

import static org.junit.jupiter.api.Assertions.*;

import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

public class ContactDataProviderTest {

    private ContactDataProvider provider;

    @BeforeEach
    public void setUp() {
        provider = new ContactDataProvider();
        ContactDataProvider.DATABASE.clear(); // Reset between tests
        ContactDataProvider.DATABASE.putAll(ContactDataProvider.getData());
    }

    @Test
    public void testFetchFromBackEnd_withNoFilter_returnsAll() {
        List<Contact> result = provider.fetch(new Query<>()).toList();
        assertEquals(8, result.size());
    }

    @Test
    public void testSizeInBackEnd_returnsCorrectSize() {
        int size = provider.size(new Query<>());
        assertEquals(8, size);
    }

    @Test
    public void testPersist_addNewContact_successfully() {
        Contact newContact = new Contact(null, "New Guy", "Street", "City", "Country", "1230009999", "newguy@example.com");
        provider.persist(newContact);
        assertNotNull(newContact.getId());
        assertEquals(9, ContactDataProvider.DATABASE.size());
    }

    @Test
    public void testPersist_updateExistingContact_successfully() {
        Contact existing = getAnyExistingContact();
        Instant originalModified = existing.getLastModified();

        Contact updated = new Contact(existing.getId(), "Updated Name", existing.getStreet(), existing.getCity(), existing.getCountry(), existing.getPhone(), existing.getEmail());
        updated.setLastModified(originalModified);
        provider.persist(updated);

        Optional<Contact> found = provider.find(existing.getId());
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
        assertTrue(found.get().getLastModified().isAfter(originalModified));
    }

    @Test
    public void testPersist_duplicateEmail_throwsException() {
        Contact existing = getAnyExistingContact();
        Contact duplicate = new Contact(null, "Another Person", "Street", "City", "Country", "1231112222", existing.getEmail());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> provider.persist(duplicate));
        assertEquals("Email already exists!", ex.getMessage());
    }

    @Test
    public void testPersist_duplicatePhone_throwsException() {
        Contact existing = getAnyExistingContact();
        Contact duplicate = new Contact(null, "Another Person", "Street", "City", "Country", existing.getPhone(), "new@example.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> provider.persist(duplicate));
        assertEquals("Phone number already exists!", ex.getMessage());
    }

    @Test
    public void testPersist_outdatedLastModified_throwsException() {
        Contact original = getAnyExistingContact();
        Instant outdated = original.getLastModified().minusSeconds(60); // simulate stale timestamp

        Contact updated = new Contact(original.getId(), "Conflict Name", original.getStreet(), original.getCity(), original.getCountry(), original.getPhone(), original.getEmail());
        updated.setLastModified(outdated);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> provider.persist(updated));
        assertEquals("This contact was modified by another user!", ex.getMessage());
    }

    @Test
    public void testDelete_removesContact() {
        Contact toDelete = getAnyExistingContact();
        provider.delete(toDelete);

        Optional<Contact> result = provider.find(toDelete.getId());
        assertFalse(result.isPresent());
        assertEquals(7, ContactDataProvider.DATABASE.size());
    }

    @Test
    public void testFind_returnsCorrectContact() {
        Contact correctContact = getAnyExistingContact();
        Optional<Contact> found = provider.find(correctContact.getId());

        assertTrue(found.isPresent());
        assertEquals(correctContact.getName(), found.get().getName());
    }

    private Contact getAnyExistingContact() {
        return ContactDataProvider.DATABASE.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No contacts in database"));
    }

}
