package org.vaadin.example;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Contact {

    private Integer id;
    private String name;
    private String street;
    private String city;
    private String country;
    private String phone;
    private String email;
    private Instant lastModified;

    public Contact() {

    }

    public Contact(Integer id, String name, String street, String city, String country, String phone, String email) {
        this.id = id;
        this.name = name;
        this.street = street;
        this.city = city;
        this.country = country;
        this.phone = phone;
        this.email = email;
        this.lastModified = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;  // Same object check
//        if (o == null || getClass() != o.getClass()) return false;  // Null or different class check
//        Contact contact = (Contact) o;  // Type cast
//
//        // Compare all important fields
//        return Objects.equals(id, contact.id) &&
//                Objects.equals(name, contact.name) &&
//                Objects.equals(email, contact.email) &&
//                Objects.equals(phone, contact.phone) &&
//                Objects.equals(street, contact.street) &&
//                Objects.equals(city, contact.city) &&
//                Objects.equals(country, contact.country) &&
//                Objects.equals(lastModified, contact.lastModified);  // Comparing lastModified for concurrency checks
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id, name, email, phone, street, city, country, lastModified);  // Hashing based on all important fields
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || this.id == null || ((Contact) o).id == null) return false;
        Contact contact = (Contact) o;
        return Objects.equals(id, contact.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}