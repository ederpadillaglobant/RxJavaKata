package com.example.rxjavasamples.room.ui

import android.arch.persistence.room.Room
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.rxjavasamples.R

import com.example.rxjavasamples.room.adapter.ContactsAdapter
import com.example.rxjavasamples.room.db.ContactsAppDatabase
import com.example.rxjavasamples.room.db.entity.Contact
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

import java.util.ArrayList

class ContactsActivity : AppCompatActivity() {

    private var contactsAdapter: ContactsAdapter? = null
    private val contactArrayList = ArrayList<Contact>()
    private var recyclerView: RecyclerView? = null

    private var contactsAppDatabase: ContactsAppDatabase? = null

    private val compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.title = " Contacts Manager"

        contactsAppDatabase = Room.databaseBuilder<ContactsAppDatabase>(applicationContext,
                ContactsAppDatabase::class.java!!, "ContactDb")
                .allowMainThreadQueries()
                .build()
        recyclerView = findViewById(R.id.recycler_view_contacts)

        contactsAdapter = ContactsAdapter(this, contactArrayList, this@ContactsActivity)
        val mLayoutManager = LinearLayoutManager(applicationContext)
        recyclerView?.let {
            it.layoutManager = mLayoutManager
            it.itemAnimator = DefaultItemAnimator() as RecyclerView.ItemAnimator?
            it.adapter = contactsAdapter
        }

        compositeDisposable.add(
            contactsAppDatabase?.contactDAO?.contacts!!.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ contactList ->
                    contactArrayList.clear()
                    contactArrayList.addAll(contactList)
                    contactsAdapter?.notifyDataSetChanged()
                }, { throwable -> throwable.printStackTrace() })
        )



        val fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { addAndEditContacts(false, null, -1) }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_contacts, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }


    fun addAndEditContacts(isUpdate: Boolean, contact: Contact?, position: Int) {
        val layoutInflaterAndroid = LayoutInflater.from(applicationContext)
        val view = layoutInflaterAndroid.inflate(R.layout.layout_add_contact, null)

        val alertDialogBuilderUserInput = AlertDialog.Builder(this@ContactsActivity)
        alertDialogBuilderUserInput.setView(view)

        val contactTitle = view.findViewById<TextView>(R.id.new_contact_title)
        val newContact = view.findViewById<EditText>(R.id.name)
        val contactEmail = view.findViewById<EditText>(R.id.email)

        contactTitle.text = if (!isUpdate) "Add New Contact" else "Edit Contact"

        if (isUpdate && contact != null) {
            newContact.setText(contact.name)
            contactEmail.setText(contact.email)
        }

        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(if (isUpdate) "Update" else "Save") { dialogBox, id -> }
                .setNegativeButton("Delete"
                ) { dialogBox, id ->
                    if (isUpdate) {

                        deleteContact(contact, position)
                    } else {

                        dialogBox.cancel()

                    }
                }


        val alertDialog = alertDialogBuilderUserInput.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
            if (TextUtils.isEmpty(newContact.text.toString())) {
                Toast.makeText(this@ContactsActivity, "Enter contact name!", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            } else {
                alertDialog.dismiss()
            }


            if (isUpdate && contact != null) {

                updateContact(newContact.text.toString(), contactEmail.text.toString(), position)
            } else {

                createContact(newContact.text.toString(), contactEmail.text.toString())
            }
        })
    }

    private fun deleteContact(contact: Contact?, position: Int) {

        contact?.let {
            contactsAppDatabase!!.contactDAO.deleteContact(it)
        }
    }

    private fun updateContact(name: String, email: String, position: Int) {

        val contact = contactArrayList[position]

        contact.name = name
        contact.email = email

        contactsAppDatabase!!.contactDAO.updateContact(contact)


    }

    private fun createContact(name: String, email: String) {
        val id = contactsAppDatabase!!.contactDAO.addContact(Contact(0, name, email))
    }
}