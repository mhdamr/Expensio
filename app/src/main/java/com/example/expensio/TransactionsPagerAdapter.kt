package com.example.expensio

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.firebase.auth.FirebaseUser
import java.util.*

class TransactionsPagerAdapter(
    fragment: Fragment,
    private val walletId: String,
    private val currentUser: FirebaseUser?
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = Int.MAX_VALUE

    override fun createFragment(position: Int): Fragment {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, position - Int.MAX_VALUE / 2)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        return TransactionListFragment.newInstance(walletId, currentUser, year, month)
    }
}