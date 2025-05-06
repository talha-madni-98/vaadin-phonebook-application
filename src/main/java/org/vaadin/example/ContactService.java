package org.vaadin.example;

import java.util.List;
import java.util.Optional;

public interface ContactService {

    void persist(Contact contact);

    Optional<Contact> find(Integer id); // Redundant but keeping since both are used

    void delete(Contact contact);
}