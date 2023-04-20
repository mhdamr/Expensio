package com.example.fundcache

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class WalletDetailFragment : Fragment(R.layout.fragment_wallet_detail), TransactionListFragment.OnTransactionListWalletBalanceUpdatedListener {

    private lateinit var walletId: String
    private lateinit var walletName: String
    private lateinit var totalBalanceText: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private lateinit var walletCurrency: String
    private lateinit var walletColor: String
    private var walletBalance: Double = 0.0

    private lateinit var fab: FloatingActionButton
    private lateinit var fab1: FloatingActionButton
    private lateinit var fab3: FloatingActionButton
    private var isFABOpen = false
    private val alphaHide = 0f
    private val alphaShow = 1f
    private val scaleHide = 0f
    private val scaleShow = 1f

    private lateinit var monthLabel: TextView
    private lateinit var viewPager: ViewPager2

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
        updateTotalBalanceTextView()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val icon = view.findViewById<ImageView>(R.id.icon)
        icon.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("walletId", walletId)
            findNavController().navigate(R.id.action_walletDetailFragment_to_recurrenceListFragment, bundle)
        }
        monthLabel = view.findViewById(R.id.month_label)

        // Initialize the ViewPager2 with TransactionsPagerAdapter
        viewPager = view.findViewById<ViewPager2>(R.id.transactions_viewpager)
        viewPager.adapter = TransactionsPagerAdapter(this, walletId, currentUser)
        viewPager.setCurrentItem(Int.MAX_VALUE / 2, false)

        checkAndUpdateRecurrence()

        fab = requireActivity().findViewById(R.id.floatingActionButton)
        fab1 = requireActivity().findViewById(R.id.fab1)
        fab3 = requireActivity().findViewById(R.id.fab3)

        // Set a click listener to the FloatingActionButton
        fab?.setOnClickListener {
            animateFAB()
        }

        fab1.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("walletId", walletId)

            db.collection("users").document(currentUser?.uid ?: "").collection("wallets").document(walletId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null) {
                        val amount = querySnapshot.getDouble("amount")
                        if (amount != null) {
                            bundle.putDouble("walletBalance", amount)
                        }
                    }

                }

            val incomeFragment = IncomeFragment()
            incomeFragment.arguments = bundle
            findNavController().navigate(R.id.incomeFragment, bundle)
            animateFAB()
        }

        fab3.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("walletId", walletId)

            db.collection("users").document(currentUser?.uid ?: "").collection("wallets").document(walletId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null) {
                        val amount = querySnapshot.getDouble("amount")
                        if (amount != null) {
                            bundle.putDouble("walletBalance", amount)
                        }
                    }

                }

            val expenseFragment = ExpenseFragment()
            expenseFragment.arguments = bundle
            findNavController().navigate(R.id.expenseFragment, bundle)
            animateFAB()
        }


        // Set the onPageChangeCallback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, position - Int.MAX_VALUE / 2)
                monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
            }
        })

        // Set the initial month label
        monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

        monthLabel.setOnClickListener {
            showMonthYearPicker()
        }


    }

    private fun showMonthYearPicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.month_year_picker_dialog, null)

        val monthPicker: NumberPicker = view.findViewById(R.id.month_picker)
        val yearPicker: NumberPicker = view.findViewById(R.id.year_picker)

        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.value = currentMonth
        monthPicker.displayedValues = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val minYear = currentYear - 100
        val maxYear = currentYear + 100
        yearPicker.minValue = minYear
        yearPicker.maxValue = maxYear
        yearPicker.value = currentYear

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(view)
            .setPositiveButton("OK") { _, _ ->
                val selectedYear = yearPicker.value
                val selectedMonth = monthPicker.value
                updateSelectedMonthYear(selectedYear, selectedMonth)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }
    }

    private fun updateSelectedMonthYear(year: Int, month: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val currentPosition = viewPager.currentItem
        val currentCalendar = Calendar.getInstance()
        currentCalendar.add(Calendar.MONTH, currentPosition - Int.MAX_VALUE / 2)

        val differenceInMonths = ((calendar.get(Calendar.YEAR) - currentCalendar.get(Calendar.YEAR)) * 12) + (calendar.get(Calendar.MONTH) - currentCalendar.get(Calendar.MONTH))
        viewPager.currentItem = currentPosition + differenceInMonths
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

    private fun animateFAB() {
        if (isFABOpen) {
            // Close the FABs
            fab.animate().rotation(0f).setDuration(200).start()
            fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorAccent))
            fab.setImageResource(R.drawable.icon_add)
            fab1.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
            fab3.animate().translationX(0f).translationY(0f).alpha(alphaHide).scaleX(scaleHide).scaleY(scaleHide)
            isFABOpen = false

        } else {
            // Open the FABs
            fab.animate().rotation(45f).setDuration(200).start()
            fab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorBoxBackground))
            fab.setImageResource(R.drawable.icon_add)
            val distance = resources.getDimension(R.dimen.standard_125)
            val angle = 80f // Angle between FABs (in degrees)

            fab1.animate().translationX((distance * Math.sin(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).translationY(-(distance * Math.cos(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).alpha(alphaShow).scaleX(scaleShow).scaleY(scaleShow)
            fab1.visibility = View.VISIBLE

            fab3.animate().translationX(-(distance * Math.sin(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).translationY(-(distance * Math.cos(Math.toRadians((180.0 - angle) / 2.0))).toFloat()).alpha(alphaShow).scaleX(scaleShow).scaleY(scaleShow)
            fab3.visibility = View.VISIBLE

            isFABOpen = true
        }
    }

    override fun onTransactionListWalletBalanceUpdated() {
        updateTotalBalanceTextView()
    }

    private fun updateTotalBalanceTextView() {

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
    }

    private fun updateWalletBalance(totalAmountAdded: Double, totalAmountDeducted: Double) {
        if ((totalAmountAdded != 0.0) || (totalAmountDeducted != 0.0)){
            // Get the wallet balance from the document snapshot and update the walletBalance
            db.collection("users").document(currentUser!!.uid)
                .collection("wallets")
                .document(walletId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val walletAmount = documentSnapshot.getDouble("amount")
                    walletBalance = walletAmount ?: 0.0 // Update the wallet balance property with the retrieved value
                    walletBalance += totalAmountAdded
                    walletBalance -= totalAmountDeducted

                    db.collection("users").document(currentUser.uid)
                        .collection("wallets")
                        .document(walletId)
                        .update("amount", (walletBalance))
                        .addOnSuccessListener {
                            updateTotalBalanceTextView()
                        }
                }
        }
    }


    private fun calculatePeriodsPassed(timeDifference: Long, currentTime: Long, latestTimestamp: Long): Long {
        val timeDifferenceMillis = TimeUnit.MILLISECONDS.convert(timeDifference, TimeUnit.MILLISECONDS)
        return (currentTime - latestTimestamp) / timeDifferenceMillis
    }

    private fun addTimeDifferenceToTimestamp(timestamp: Long, timeDifference: Long, periods: Long): Long {
        return timestamp + (timeDifference * periods)
    }

    private fun checkAndUpdateRecurrence() {
        val currentTime = Calendar.getInstance().timeInMillis
        if (currentUser != null) {
            val walletRef = db.collection("users").document(currentUser.uid)
                .collection("wallets")
                .document(walletId)
                .collection("recurrence")

            // Declare the variable before the loop
            var totalAmountAdded = 0.0
            var totalAmountDeducted = 0.0

            // Get all income transactions with a recurrence
            walletRef.whereEqualTo("type", "income")
                .get()
                .addOnSuccessListener { documents ->
                    for (income in documents) {
                        val latestTimestamp = income.getDate("timestamp")?.time ?: continue
                        val recurrenceOption = income.getString("recurrence") ?: continue

                        // Calculate the time difference based on the recurrence option
                        val timeDifference = when (recurrenceOption) {
                            "Every day" -> TimeUnit.SECONDS.toMillis(10)
                            "Every 2 days" -> TimeUnit.DAYS.toMillis(2)
                            "Weekly" -> TimeUnit.DAYS.toMillis(7)
                            "Monthly" -> TimeUnit.DAYS.toMillis(30) // Approximation
                            "Yearly" -> TimeUnit.DAYS.toMillis(365) // Approximation
                            else -> 0L
                        }


                        // Calculate the number of periods passed
                        val periodsPassed = calculatePeriodsPassed(timeDifference, currentTime, latestTimestamp)

                        // If at least one period has passed, add new transactions
                        if (periodsPassed > 0) {
                            // Add the appropriate number of transactions
                            for (i in 1..periodsPassed) {
                                val newTimestamp = addTimeDifferenceToTimestamp(latestTimestamp, timeDifference, i)
                                val amountIncome = income.getDouble("amount") ?: continue

                                totalAmountAdded += amountIncome

                                val newIncome = hashMapOf(
                                    "amount" to income.getDouble("amount"),
                                    "description" to income.getString("description"),
                                    "type" to "income",
                                    "timestamp" to Date(newTimestamp)
                                )

                                val transactionsRef = db.collection("users").document(currentUser.uid)
                                    .collection("wallets")
                                    .document(walletId)
                                    .collection("transactions")

                                transactionsRef.add(newIncome)
                                    .addOnSuccessListener {
                                        // Update the timestamp of the recurrence document
                                        walletRef.document(income.id)
                                            .update("timestamp", Date(newTimestamp))


                                        Log.d("IncomeFragment", "New income added successfully based on recurrence")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("IncomeFragment", "Error adding new income based on recurrence", e)
                                    }
                            }
                            // Call updateWalletBalance() after the for loop
                            updateWalletBalance(totalAmountAdded, totalAmountDeducted)
                        }
                    }
                }



            // Get all expense transactions with a recurrence
            walletRef.whereEqualTo("type", "expense")
                .get()
                .addOnSuccessListener { documents ->
                    for (expense in documents) {
                        val latestTimestamp = expense.getDate("timestamp")?.time ?: continue
                        val recurrenceOption = expense.getString("recurrence") ?: continue

                        // Calculate the time difference based on the recurrence option
                        val timeDifference = when (recurrenceOption) {
                            "Every day" -> TimeUnit.SECONDS.toMillis(10)
                            "Every 2 days" -> TimeUnit.DAYS.toMillis(2)
                            "Weekly" -> TimeUnit.DAYS.toMillis(7)
                            "Monthly" -> TimeUnit.DAYS.toMillis(30) // Approximation
                            "Yearly" -> TimeUnit.DAYS.toMillis(365) // Approximation
                            else -> 0L
                        }

                        // Calculate the number of periods passed
                        val periodsPassed = calculatePeriodsPassed(timeDifference, currentTime, latestTimestamp)

                        // If at least one period has passed, add new transactions
                        if (periodsPassed > 0) {
                            // Add the appropriate number of transactions
                            for (i in 1..periodsPassed) {
                                val newTimestamp = addTimeDifferenceToTimestamp(latestTimestamp, timeDifference, i)
                                val amountExpense = expense.getDouble("amount") ?: continue

                                totalAmountDeducted += amountExpense

                                val newExpense = hashMapOf(
                                    "amount" to expense.getDouble("amount"),
                                    "description" to expense.getString("description"),
                                    "type" to "expense",
                                    "timestamp" to Date(newTimestamp)
                                )

                                val transactionsRef = db.collection("users").document(currentUser.uid)
                                    .collection("wallets")
                                    .document(walletId)
                                    .collection("transactions")

                                transactionsRef.add(newExpense)
                                    .addOnSuccessListener {
                                        // Update the timestamp of the recurrence document
                                        walletRef.document(expense.id)
                                            .update("timestamp", Date(newTimestamp))


                                        Log.d("ExpenseFragment", "New expense added successfully based on recurrence")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("ExpenseFragment", "Error adding new expense based on recurrence", e)
                                    }

                            }
                            // Call updateWalletBalance() after the for loop
                            updateWalletBalance(totalAmountAdded, totalAmountDeducted)
                        }
                    }
                }

        }
    }
}