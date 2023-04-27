package com.example.fundcache

import android.graphics.Color.green
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fundcache.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class HomeFragment : Fragment() {

    // Import necessary libraries
    private lateinit var binding: FragmentHomeBinding
    private val currentUser by lazy { FirebaseAuth.getInstance().currentUser!! }
    private val db = FirebaseFirestore.getInstance()

    // Initialize other variables
    private lateinit var walletsAdapter: ArrayAdapter<String>
    private lateinit var walletIds: MutableList<String>
    private var selectedWalletId: String? = null
    private lateinit var selectedMonthYear: Pair<Int, Int>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fab = activity?.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        // Set a click listener to the FloatingActionButton
        fab?.setOnClickListener {
            // Navigate to the AddWalletsFragment
                findNavController().navigate(R.id.searchTransactionFragment)
        }

        // Initialize walletIds list and ArrayAdapter for the wallet spinner
        walletIds = mutableListOf()
        walletsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)

        // Load wallets
        loadWallets()

        // Set up wallet spinner
        binding.walletSpinner.adapter = walletsAdapter
        binding.walletSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedWalletId = walletIds[position]
                // Update the chart when a new wallet is selected
                updateChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedWalletId = null
            }
        }

        // Set up month/year picker
        binding.monthYearPicker.setOnClickListener {
            val dialog = MonthYearPickerDialog()
            dialog.setListener { _, year, month, _ ->
                selectedMonthYear = Pair(year, month)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                val monthName = SimpleDateFormat("MMMM").format(calendar.time)
                binding.monthYearPicker.text = "$monthName $year"
                // Update the chart when a new month/year is selected
                updateChart()
            }
            dialog.show(parentFragmentManager, "MonthYearPickerDialog")
        }
    }

    private fun loadWallets() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .collection("wallets")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    querySnapshot.documents.forEach { documentSnapshot ->
                        walletsAdapter.add(documentSnapshot.getString("name") ?: "")
                        walletIds.add(documentSnapshot.id)
                    }
                }
        }
    }

    private fun updateChart() {
        if (selectedWalletId != null && ::selectedMonthYear.isInitialized) {
            getTransactionsForMonthYear { transactions ->
                val chartData = processTransactions(transactions)
                drawBarChart(chartData)

                val incomeTransactions = transactions.filter { it.getString("type") == "income" }
                val income = incomeTransactions.sumByDouble { it.getDouble("amount") ?: 0.0 }.toFloat()
                val incomeCount = incomeTransactions.size

                val expenseTransactions = transactions.filter { it.getString("type") == "expense" }
                val expense = expenseTransactions.sumByDouble { it.getDouble("amount") ?: 0.0 }.toFloat()
                val expenseCount = expenseTransactions.size

                drawPieChart(income, expense)
                updateIncomeExpenseSummaries(income, expense, incomeCount, expenseCount)
            }
        }
    }

    private fun getTransactionsForMonthYear(callback: (List<DocumentSnapshot>) -> Unit) {
        val startOfMonth = Calendar.getInstance().apply {
            set(selectedMonthYear.first, selectedMonthYear.second, 1, 0, 0, 0)
        }.time
        val endOfMonth = Calendar.getInstance().apply {
            set(selectedMonthYear.first, selectedMonthYear.second + 1, 1, 0, 0, 0)
            add(Calendar.SECOND, -1)
        }.time

        db.collection("users").document(currentUser.uid)
            .collection("wallets").document(selectedWalletId!!)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .whereLessThanOrEqualTo("timestamp", endOfMonth)
            .get()
            .addOnSuccessListener { querySnapshot ->
                callback(querySnapshot.documents)
            }
    }

    private fun processTransactions(transactions: List<DocumentSnapshot>): List<BarEntry> {
        val dailySums = mutableMapOf<Int, Pair<Double, Double>>()

        transactions.forEach { transaction ->
            val timestamp = transaction.getDate("timestamp") ?: return@forEach
            val calendar = Calendar.getInstance().apply { time = timestamp }
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val amount = transaction.getDouble("amount") ?: 0.0
            val type = transaction.getString("type") ?: ""

            val dailyAmounts = dailySums[dayOfMonth] ?: Pair(0.0, 0.0)
            val newDailyAmounts = when (type) {
                "income" -> Pair(dailyAmounts.first + amount, dailyAmounts.second)
                "expense" -> Pair(dailyAmounts.first, dailyAmounts.second + amount)
                else -> dailyAmounts
            }
            dailySums[dayOfMonth] = newDailyAmounts
        }

        return dailySums.map { (day, amounts) ->
            BarEntry(day.toFloat(), floatArrayOf(amounts.first.toFloat(), -amounts.second.toFloat()))
        }
    }

    private fun drawBarChart(data: List<BarEntry>) {

        val barDataSet = BarDataSet(data, "").apply {
            setColors(ContextCompat.getColor(requireContext(), R.color.Income), ContextCompat.getColor(requireContext(), R.color.Expense))
            stackLabels = arrayOf("Income", "Expense")

            valueTextColor = ContextCompat.getColor(requireContext(), R.color.colorTextPrimary) // set text color of the numbers inside the bar chart
        }

        val barData = BarData(barDataSet)
        binding.barChart.apply {
            this.data = barData
            xAxis.valueFormatter = DayOfMonthFormatter()
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)
            axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.colorTextPrimary)


            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false

            description.isEnabled = false
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)

            setNoDataText(if (data.isEmpty()) "This time period contains no transactions. Please select a time period that contains transactions for the selected wallet." else "")

            invalidate()
        }
        binding.barChart.xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.colorTextPrimary) // set text color of the x-axis labels
    }

    private class DayOfMonthFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return value.toInt().toString()
        }
    }

    private fun drawPieChart(income: Float, expense: Float) {
        val entries = mutableListOf<PieEntry>()

        if ( (income >  0 && expense > 0) || (income > 0 && 0 <= expense) ) {
            entries.add(PieEntry(income, "Income"))
        }

        if ( (expense > 0 && income > 0) || (expense > 0 && 0 <= income) ) {
            entries.add(PieEntry(expense, "Expense"))

        }

        val pieDataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.Income),
                ContextCompat.getColor(requireContext(), R.color.Expense)
            )
        }

        val pieData = PieData(pieDataSet)
        binding.pieChart.apply {
            this.data = pieData

            description.isEnabled = false
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)

            setUsePercentValues(true)

            setNoDataText(if (entries.isEmpty()) "This time period contains no transactions. Please select a time period that contains transactions for the selected wallet." else "")

            invalidate()
        }
    }

    private fun updateIncomeExpenseSummaries(income: Float, expense: Float, incomeCount: Int, expenseCount: Int) {
        binding.incomeSummary.text = getString(
            R.string.income_summary,
            incomeCount,
            String.format("%.2f", income)
        )
        binding.expenseSummary.text = getString(
            R.string.expense_summary,
            expenseCount,
            String.format("%.2f", expense)
        )
    }

}