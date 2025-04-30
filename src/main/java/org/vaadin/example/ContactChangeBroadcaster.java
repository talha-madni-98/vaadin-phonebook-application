package org.vaadin.example;

import com.vaadin.flow.shared.Registration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public class ContactChangeBroadcaster {

    private static final List<Consumer<Contact>> listeners = new CopyOnWriteArrayList<>();

    public static synchronized Registration register(Consumer<Contact> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public static synchronized void broadcast(Contact contact) {
        for (Consumer<Contact> listener : listeners) {
            listener.accept(contact);
        }
    }
}


//public class ContactChangeBroadcaster {
//
//    private static final List<Consumer<Void>> listeners = new LinkedList<>();
//
//    public static synchronized Registration register(Consumer<Void> listener) {
//        listeners.add(listener);
//        System.out.println("Listener registered: ");
//        return () -> {
//            synchronized (ContactChangeBroadcaster.class) {
//                listeners.remove(listener);
//                System.out.println("Listener removed: ");
//            }
//        };
//    }
//
//    public static synchronized void broadcast() {
//        System.out.println("Broadcasting message to listeners: ");
//        for (Consumer<Void> listener : listeners) {
//            listener.accept(null);
//        }
//    }
//}
