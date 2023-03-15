package com.example.fundcache

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fundcache.databinding.ActivityMainBinding
import com.example.fundcache.databinding.FragmentWalletsBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class WalletsFragment : Fragment() {

    private lateinit var binding: FragmentWalletsBinding
    private lateinit var walletList: ViewGroup
    private lateinit var totalAmountText: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWalletsBinding.inflate(inflater, container, false)
        walletList = binding.walletsList
        totalAmountText = binding.totalAmountText
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fab = activity?.findViewById<FloatingActionButton>(R.id.floatingActionButton)

        // Set a click listener to the FloatingActionButton
        fab?.setOnClickListener {
            // Navigate to the AddWalletsFragment
            if (isVisible) {
                findNavController().navigate(R.id.action_walletsFragment_to_addWalletsFragment)
            }
        }

        if (currentUser != null) {
            // Query the wallets collection for the current user
            db.collection("users").document(currentUser.uid).collection("wallets")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // Clear any existing wallets from the list
                    walletList.removeAllViews()

                    // Loop through the query results and add a box for each wallet
                    var totalAmount = 0.0
                    for (document in querySnapshot) {
                        val wallet = document.data
                        val walletName = wallet["name"] as String
                        val walletCurrency = wallet["currency"] as String
                        val walletAmount = wallet["amount"] as Double
                        val walletColor = wallet["color"] as String
                        val walletColorInt = Color.parseColor(walletColor)

                        val walletBox =
                            layoutInflater.inflate(R.layout.wallet_item, walletList, false)
                        walletBox.findViewById<TextView>(R.id.wallet_name_textview).text = walletName
                        walletBox.findViewById<TextView>(R.id.wallet_amount_textview).text =
                            String.format("%.2f %s", walletAmount, walletCurrency)

                        // Create a gradient drawable with rounded corners and set its color to walletColorInt
                        val shape = GradientDrawable()
                        shape.shape = GradientDrawable.RECTANGLE
                        shape.cornerRadius = resources.getDimension(R.dimen.wallet_item_corner_radius)
                        shape.setColor(walletColorInt)

                        // Set the background of the walletBox to the gradient drawable
                        walletBox.background = shape

                        walletList.addView(walletBox)



                        totalAmount += walletAmount
                    }

                    // Set the total amount text
                    totalAmountText.text = String.format("Total: $%.2f", totalAmount)
                }
        }
    }

}