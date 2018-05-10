package org.thoughtcrime.securesms.contactshare;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import org.thoughtcrime.securesms.contactshare.model.Contact;
import org.thoughtcrime.securesms.contactshare.model.Selectable;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.util.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;

class ContactShareEditViewModel extends ViewModel {

  private final MutableLiveData<List<Contact>> contacts;
  private final SingleLiveEvent<Event>         events;
  private final ContactRepository              repo;

  ContactShareEditViewModel(@NonNull List<Long>        contactIds,
                            @NonNull ContactRepository contactRepository)
  {
    contacts = new MutableLiveData<>();
    events   = new SingleLiveEvent<>();
    repo     = contactRepository;

    repo.getContacts(contactIds, retrieved -> {
      if (retrieved.isEmpty()) {
        events.postValue(Event.BAD_CONTACT);
      } else {
        contacts.postValue(retrieved);
      }
    });
  }

  @NonNull LiveData<List<Contact>> getContacts() {
    return contacts;
  }

  @NonNull LiveData<List<Contact>> getFinalizedContacts() {
    List<Contact> currentContacts = getCurrentContacts();
    List<Contact> trimmedContacts = new ArrayList<>(currentContacts.size());

    for (Contact contact : currentContacts) {
      trimmedContacts.add(new Contact(contact.getName(),
                                      contact.getOrganization(),
                                      trimSelectables(contact.getPhoneNumbers()),
                                      trimSelectables(contact.getEmails()),
                                      trimSelectables(contact.getPostalAddresses()),
                                      contact.getAvatar()));
    }

    SingleLiveEvent<List<Contact>> finalizedContacts = new SingleLiveEvent<>();
    repo.persistContactImages(trimmedContacts, finalizedContacts::postValue);

    return finalizedContacts;
  }

  @NonNull LiveData<Event> getEvents() {
    return events;
  }

  private <E extends Selectable> List<E> trimSelectables(List<E> selectables) {
    return Stream.of(selectables).filter(Selectable::isSelected).toList();
  }

  @NonNull
  private List<Contact> getCurrentContacts() {
    List<Contact> currentContacts = contacts.getValue();
    return currentContacts != null ? currentContacts : new ArrayList<>();
  }

  enum Event {
    BAD_CONTACT
  }

  static class Factory extends ViewModelProvider.NewInstanceFactory {

    private final List<Long>        contactIds;
    private final ContactRepository contactRepository;

    Factory(@NonNull List<Long> contactIds, @NonNull ContactRepository contactRepository) {
      this.contactIds        = contactIds;
      this.contactRepository = contactRepository;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return modelClass.cast(new ContactShareEditViewModel(contactIds, contactRepository));
    }
  }
}
