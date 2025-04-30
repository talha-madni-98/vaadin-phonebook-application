package org.vaadin.example;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Route("")
public class MainView extends VerticalLayout {

    private Crud<Contact> crud;
    private ContactDataProvider dataProvider;
    private Registration broadcasterRegistration;
    private Span totalCountSpan = new Span(); // Displays total contacts count
//    private boolean isEditorConflicted = false;


    private String NAME = "name";
    private String STREET = "street";
    private String CITY = "city";
    private String COUNTRY = "country";
    private String PHONE_NUMBER = "phone";
    private String EMAIL = "email";

    public MainView() {
        dataProvider = new ContactDataProvider();
        crud = new Crud<>(Contact.class, createEditor());

        setupDataProvider();
        setupGrid();
        setupToolbar();

        add(crud);

        broadcasterRegistration = ContactChangeBroadcaster.register(this::receiveBroadcast);
    }

    private void receiveBroadcast(Contact updatedContact) {
        getUI().ifPresent(ui -> ui.access(() -> {
            Contact editingContact = crud.getEditor().getItem();
            if (editingContact != null && editingContact.getId() != null
                    && editingContact.getId().equals(updatedContact.getId())) {

//                isEditorConflicted = true;

                Notification.show("Warning: This record was updated by someone else. Please re-open the Editor.",
                                5000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);

//                crud.edit(null, Crud.EditMode.EXISTING_ITEM);
                crud.getSaveButton().setEnabled(false);
            }

            crud.getGrid().getDataProvider().refreshAll();
        }));
    }

    @Override
    public void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    private void setupGrid(){
        Grid<Contact> grid = crud.getGrid();

        Crud.removeEditColumn(grid);

        grid.addItemClickListener(event -> crud.edit(event.getItem(),
                Crud.EditMode.EXISTING_ITEM));

        List<String> visibleColumns = Arrays.asList(NAME, EMAIL, PHONE_NUMBER);
        grid.getColumns().forEach(column -> {
            String key = column.getKey();
            if(!visibleColumns.contains(key)){
                grid.removeColumn(column);
            }
        });

        grid.setColumnOrder(grid.getColumnByKey(NAME),
                grid.getColumnByKey(PHONE_NUMBER),
                grid.getColumnByKey(EMAIL));
    }

    private CrudEditor<Contact> createEditor() {
        TextField name = new TextField("Name");
        TextField street = new TextField("Street");
        TextField city = new TextField("City");
        TextField country = new TextField("Country");
        TextField phone = new TextField("Phone");
        TextField email = new TextField("Email");

        FormLayout form = new FormLayout(name, street, city, country, phone, email);

        Binder<Contact> binder = new Binder<>(Contact.class);
        binder.forField(name).asRequired().bind(Contact::getName, Contact::setName);
        binder.forField(street).asRequired().bind(Contact::getStreet, Contact::setStreet);
        binder.forField(city).asRequired().bind(Contact::getCity, Contact::setCity);
        binder.forField(country).asRequired().bind(Contact::getCountry, Contact::setCountry);
        binder.forField(phone)
                .asRequired("Phone number is required")
                .withValidator(new RegexpValidator("Phone number must contain only digits", "\\d+"))
                .withValidator(phoneNumber -> isPhoneUnique(phoneNumber, crud.getEditor().getItem()), // binder.getBean()
                        "Phone number already exists!")
                .bind(Contact::getPhone, Contact::setPhone);
        binder.forField(email).asRequired("Email is required")
                .withValidator(new EmailValidator("Please enter a valid email address"))
//                .withValidator(emailAddress -> isEmailUnique(emailAddress, crud.getEditor().getItem()), // binder.getBean()
//                        "Email already exists!")
                .bind(Contact::getEmail, Contact::setEmail);

        return new BinderCrudEditor<>(binder, form);
    }

    private void setupDataProvider(){
        crud.setDataProvider(dataProvider);
        crud.addDeleteListener(
                deleteEvent -> {
                    dataProvider.delete(deleteEvent.getItem());
                    updateTotalCount();
                });
        crud.addSaveListener(
                saveEvent -> {
                    try {
                        dataProvider.persist(saveEvent.getItem());
                    } catch (IllegalArgumentException e) {
                        Notification notification = Notification.show(e.getMessage());
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        notification.setPosition(Notification.Position.BOTTOM_CENTER);
                        notification.setDuration(3000);
//                        crud.edit(saveEvent.getItem(), Crud.EditMode.EXISTING_ITEM);
                    }
                });

//        crud.addSaveListener(saveEvent -> {
//            if (isEditorConflicted) {
//                Notification.show("Save blocked! Someone else updated this record. Please close and reopen the editor.",
//                                5000, Notification.Position.BOTTOM_CENTER)
//                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
//                crud.edit(saveEvent.getItem(), Crud.EditMode.EXISTING_ITEM);
//                return; // Don't proceed with saving
//            }
//
//            Contact contact = saveEvent.getItem();
//            Optional<Contact> latestOpt = dataProvider.findById(contact.getId());
//
//            if (latestOpt.isPresent()) {
//                Contact latest = latestOpt.get();
//                if (!Objects.equals(contact.getLastModified(), latest.getLastModified())) {
//                    Notification.show("Conflict detected! Someone else updated this record.",
//                                    5000, Notification.Position.BOTTOM_CENTER)
//                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
//                    crud.edit(latest, Crud.EditMode.EXISTING_ITEM); // re-open latest version
//                    return;
//                }
//            }
//
//            contact.setLastModified(Instant.now());
//            dataProvider.persist(contact);
//            ContactChangeBroadcaster.broadcast(contact);
//            updateTotalCount();
//        });
    }

    private void setupToolbar() {
        updateTotalCount();
        Button button = new Button("Add Contact", VaadinIcon.PLUS.create());
        button.addClickListener(event -> {
            crud.edit(new Contact(), Crud.EditMode.NEW_ITEM);
        });
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        crud.setNewButton(button);

        HorizontalLayout toolbar = new HorizontalLayout(totalCountSpan);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, toolbar);
        toolbar.setSpacing(false);
        crud.setToolbar(toolbar);
    }

    private void updateTotalCount() {
        totalCountSpan.setText("Total: " + dataProvider.DATABASE.size() + " contacts");
    }

    private boolean isPhoneUnique(String phoneNumber, Contact currentContact) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return true; // don't validate empty value here, already handled by asRequired()
        }
        return ContactDataProvider.DATABASE.stream()
                .filter(contact -> !contact.equals(currentContact)) // skip self
                .noneMatch(contact -> contact.getPhone().equals(phoneNumber));
    }

    private boolean isEmailUnique(String emailAddress, Contact currentContact) {
        if (emailAddress == null || emailAddress.isEmpty()) {
            return true; // don't validate empty value here, already handled by asRequired()
        }
        return ContactDataProvider.DATABASE.stream()
                .filter(contact -> !contact.equals(currentContact)) // skip self
                .noneMatch(contact -> contact.getEmail().equals(emailAddress));
    }
}