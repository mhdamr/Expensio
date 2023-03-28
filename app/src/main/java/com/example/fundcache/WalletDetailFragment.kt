package com.example.fundcache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WalletDetailFragment : Fragment() {

    private lateinit var walletId: String
    private lateinit var walletName: String
    private lateinit var totalBalanceText: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wallet_detail, container, false)

        // Get the walletId and walletName arguments passed from WalletsFragment
        walletId = arguments?.getString("walletId") ?: ""
        walletName = arguments?.getString("walletName") ?: ""

        // Set the wallet name as the title of the toolbar
        val toolbar = requireActivity().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.title = walletName

        // Set up the total balance text view
        totalBalanceText = view.findViewById(R.id.total_balance_textview)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (currentUser != null) {
            // Query the transactions collection for the selected wallet
            db.collection("users").document(currentUser.uid).collection("wallets").document(walletId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var totalBalance = 0.0
                    if (querySnapshot != null) {
                        val transaction = querySnapshot.data
                        val amount = querySnapshot.getDouble("amount")
                        if (amount != null) {
                            totalBalance += amount
                        }
                    }

                    // Set the total balance text
                    totalBalanceText.text = String.format("%.2f", totalBalance)
                }
        }

        // Add click listener to income_button
        view.findViewById<FloatingActionButton>(R.id.income_button).setOnClickListener {
            // Create a bundle to pass arguments to IncomeFragment
            val bundle = Bundle()
            bundle.putString("walletId", walletId)
            // Navigate to IncomeFragment with the walletId argument
            findNavController().navigate(R.id.incomeFragment, bundle)

        }


    }
}
