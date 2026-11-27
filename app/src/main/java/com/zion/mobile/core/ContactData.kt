package com.zion.mobile.core

import android.content.Context
import android.provider.ContactsContract
import com.zion.mobile.commands.ContactMatch

class ContactData(private val context: Context) {

    fun loadContacts(): List<ContactMatch> {
        val contacts = mutableListOf<ContactMatch>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            val numberIndex = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            while (it.moveToNext()) {
                contacts.add(
                    ContactMatch(
                        name = it.getString(nameIndex),
                        number = it.getString(numberIndex)
                    )
                )
            }
        }

        return contacts
    }
}