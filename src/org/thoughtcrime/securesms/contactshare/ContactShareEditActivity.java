package org.thoughtcrime.securesms.contactshare;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.annimon.stream.Stream;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.SignalExecutors;
import org.thoughtcrime.securesms.contactshare.model.Contact;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;

import java.util.ArrayList;
import java.util.List;

import static org.thoughtcrime.securesms.contactshare.ContactShareEditViewModel.*;

public class ContactShareEditActivity extends PassphraseRequiredActionBarActivity {

  public  static final String KEY_CONTACTS  = "contacts";
  private static final String KEY_CONTACT_IDS = "ids";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ContactShareEditViewModel viewModel;

  public static Intent getIntent(@NonNull Context context, @NonNull List<Long> contactIds) {
    ArrayList<String> serializedIds = new ArrayList<>(Stream.of(contactIds).map(String::valueOf).toList());

    Intent intent = new Intent(context, ContactShareEditActivity.class);
    intent.putStringArrayListExtra(KEY_CONTACT_IDS, serializedIds);
    return intent;
  }

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState, boolean ready) {
    setContentView(R.layout.activity_contact_share_edit);

    if (getIntent() == null) {
      throw new IllegalStateException("You must supply extras to this activity. Please use the #getIntent() method.");
    }

    List<String> serializedIds = getIntent().getStringArrayListExtra(KEY_CONTACT_IDS);
    if (serializedIds == null) {
      throw new IllegalStateException("You must supply contact ID's to this activity. Please use the #getIntent() method.");
    }

    List<Long> contactIds = Stream.of(serializedIds).map(Long::parseLong).toList();

    View sendButton = findViewById(R.id.contact_share_edit_send);
    sendButton.setOnClickListener(v -> viewModel.getFinalizedContacts().observe(this, this::onSendClicked));

    RecyclerView contactList = findViewById(R.id.contact_share_edit_list);
    contactList.setLayoutManager(new LinearLayoutManager(this));
    contactList.getLayoutManager().setAutoMeasureEnabled(true);

    ContactShareEditAdapter contactAdapter = new ContactShareEditAdapter(GlideApp.with(this), dynamicLanguage.getCurrentLocale());
    contactList.setAdapter(contactAdapter);

    ContactRepository contactRepository = new ContactRepository(this,
                                                                SignalExecutors.DATABASE,
                                                                dynamicLanguage.getCurrentLocale(),
                                                                DatabaseFactory.getContactsDatabase(this),
                                                                DatabaseFactory.getThreadDatabase(this));

    viewModel = ViewModelProviders.of(this, new Factory(contactIds, contactRepository)).get(ContactShareEditViewModel.class);
    viewModel.getContacts().observe(this, contacts -> {
      contactAdapter.setContacts(contacts);
      contactList.post(() -> contactList.scrollToPosition(0));
    });
    viewModel.getEvents().observe(this, this::presentEvent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicTheme.onResume(this);
  }

  private void presentEvent(@Nullable Event event) {
    if (event == null) {
      return;
    }

    if (event == Event.BAD_CONTACT) {
      Toast.makeText(this, R.string.ContactShareEditActivity_invalid_contact, Toast.LENGTH_SHORT).show();
      finish();
    }
  }

  private void onSendClicked(List<Contact> contacts) {
    Intent intent = new Intent();

    ArrayList<Contact> contactArrayList = new ArrayList<>(contacts.size());
    contactArrayList.addAll(contacts);
    intent.putExtra(KEY_CONTACTS, contactArrayList);

    setResult(Activity.RESULT_OK, intent);

    finish();
  }
}
