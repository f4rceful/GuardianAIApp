package com.example.guardianai.util

import android.content.Context
import android.provider.ContactsContract

data class ContactInfo(
    val name: String,
    val id: String
)

object ContactManager {
    fun getContacts(context: Context): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            
            while (it.moveToNext()) {
                if (idIndex != -1 && nameIndex != -1) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        contacts.add(ContactInfo(name, id))
                    }
                }
            }
        }
        return contacts
    }
}
