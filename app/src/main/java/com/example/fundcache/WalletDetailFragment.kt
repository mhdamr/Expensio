package com.example.fundcache

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomappbar.BottomAppBar

class WalletDetailFragment : Fragment() {

    private lateinit var walletId: String
    private lateinit var walletName: String
    private lateinit var totalBalanceText: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private lateinit var walletCurrency: String
    private lateinit var walletColor: String

    private lateinit var fab: FloatingActionButton
    private lateinit var fab1: FloatingActionButton
    private lateinit var fab2: FloatingActionButton
    private lateinit var fab3: FloatingActionButton
    private var isFABOpen = false
    private val alphaHide = 0f
    private val alphaShow = 1f
    private val scaleHide = 0f
    private val scaleShow = 1f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_wallet_detail, container, false)

        // Inform the fragment that it has menu items to display
        setHasOptionsMenu(true)

        // Get the walletId, walletName, walletCurrency, and walletColor arguments passed from WalletsFragment
        walletId = arguments?.getString("walletId") ?: ""
        walletName = arguments?.getString("walletName") ?: ""
        walletCurrency = arguments?.getString("walletCurrency") ?: ""
        walletColor = arguments?.getString("walletColor") ?: ""

        // Set the background of the rounded_wallet_info layout to the gradient drawable
        val roundedWalletInfo = view.findViewById<View>(R.id.wallet_detail_box)
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = resources.getDimension(R.dimen.wallet_item_corner_radius)
        shape.setColor(Color.parseColor(walletColor))
        roundedWalletInfo.background = shape

        // Set up the wallet currency text view
        val walletCurrencyTextView = view.findViewById<TextView>(R.id.wallet_currency_textview)
        walletCurrencyTextView.text = walletCurrency

        // Set up the wallet name text view
        val walletNameTextView = view.findViewById<TextView>(R.id.wallet_name_textview)
        walletNameTextView.text = walletName

        // Set up the total balance text view
        totalBalanceText = view.findViewById(R.id.total_balance_textview)
        db.collection("users").document(currentUser?.uid ?: "").collection("wallets").document(walletId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalBalance = 0.0
                if (querySnapshot != null) {
                    val amount = querySnapshot.getDouble("amount")
                    if (amount != null) {
                        totalBalance += amount
                    }
                }

                // Set the total balance text
                totalBalanceText.text = String.format("%.2f", totalBalance)
            }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab = requireActivity().findViewById(R.id.floatingActionButton)
        fab1 = requireActivity().findViewById(R.id.fab1)
        fab2 = requireActivity().findViewById(R.id.fab2)
        fab3 = requireActivity().findViewById(R.id.fab3)

        // Set a click listener to the FloatingActionButton
        fab?.setOnClickListener {
            animateFAB()
        }

        if (currentUser != null) {
            // Query the transactions collection for the selected wallet
            db.collection("users").document(currentUser.uid).collection("wallets").document(walletId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var totalBalance = 0.0
                    if (querySnapshot != null) {
                        val amount = querySnapshot.getDouble("amount")
                        if (amount != null) {
                            totalBalance += amount
                        }
                    }

                    // Set the total balance text
                    totalBalanceText.text = String.format("%.2f", totalBalance)
                }
        }

        fab1.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("walletId", walletId)
            val incomeFragment = IncomeFragment()
            incomeFragment.arguments = bundle
            findNavController().navigate(R.id.incomeFragment, bundle)
            animateFAB()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_wallet -> {
                val bundle = Bundle()
                bundle.putString("walletId", walletId)
                bundle.putString("walletName", walletName)
                findNavController().navigate(R.id.action_editWalletsFragment, bundle)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // Add the following code inside WalletsFragment class
    override fun onResume() {
        super.onResume()
        showFAB()
    }

    override fun onPause() {
        super.onPause()
        hideFAB()
    }

    private fun showFAB() {
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton)
        val bottomAppBar = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)

        fab.visibility = View.VISIBLE

        bottomAppBar.fabCradleMargin = 30f
        bottomAppBar.fabCradleRoundedCornerRadius = 30f
        bottomAppBar.cradleVerticalOffset = 30f
    }

    private fun hideFAB() {
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.floatingActionButton)
        val bottomAppBar = requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar)

        fab.visibility = View.GONE

        bottomAppBar.fabCradleMargin = 0f
        bottomAppBar.fabCradleRoundedCornerRadius = 0f
        bottomAppBar.cradleVerticalOffset = 0f
    }

    private fun animateFAB() {
        if (isFABOpen) {
            // Close the FABs
            fab.setImageResource(R.drawable.icon_add)
            fab1.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
            fab2.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
            fab3.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
            isFABOpen = false

        } else {
            // Open the FABs
            fab.setImageResource(R.drawable.icon_light_mode)
            val distance = resources.getDimension(R.dimen.standard_125)
            val angle = 80f // Angle between FABs (in degrees)

            fab1.animate().translationX((distance * Math.sin(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).translationY(-(distance * Math.cos(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).alpha(alphaShow).scaleX(scaleShow).scaleY(scaleShow)
            fab1.visibility = View.VISIBLE

            fab2.animate().translationX(0f).translationY(-distance).alpha(alphaShow).scaleX(scaleShow).scaleY(scaleShow)
            fab2.visibility = View.VISIBLE

            fab3.animate().translationX(-(distance * Math.sin(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).translationY(-(distance * Math.cos(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).alpha(alphaShow).scaleX(scaleShow).scaleY(scaleShow)
            fab3.visibility = View.VISIBLE

            isFABOpen = true
        }
    }

}
