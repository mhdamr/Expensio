package com.example.expensio.Wallets

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.expensio.databinding.FragmentAddWalletsBinding
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.ContextCompat
import com.example.expensio.CountryCurrency
import com.example.expensio.CountryCurrencyAdapter
import com.example.expensio.CurrencyDialogFragment
import com.example.expensio.R
import com.github.dhaval2404.colorpicker.listener.ColorListener
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.bottomappbar.BottomAppBar

class AddWalletsFragment : Fragment() {
    private lateinit var binding: FragmentAddWalletsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser
    private var selectedColor = "#2196f3"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddWalletsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ResourceType")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the bottom app bar
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.GONE

        // Find the EditText view
        val currencyEditText = view.findViewById<EditText>(R.id.currency_spinner)


        // Set up currency spinner
        val currencies = mutableListOf(
            CountryCurrency("Afghanistan", "AFN"),
            CountryCurrency("Albania", "ALL"),
            CountryCurrency("Algeria", "DZD"),
            CountryCurrency("Eurozone", "EUR"),
            CountryCurrency("Angola", "AOA"),
            CountryCurrency("Eastern Caribbean", "XCD"),
            CountryCurrency("Argentina", "ARS"),
            CountryCurrency("Armenia", "AMD"),
            CountryCurrency("Australia", "AUD"),
            CountryCurrency("Azerbaijan", "AZN"),
            CountryCurrency("Bahamas", "BSD"),
            CountryCurrency("Bahrain", "BHD"),
            CountryCurrency("Bangladesh", "BDT"),
            CountryCurrency("Barbados", "BBD"),
            CountryCurrency("Belarus", "BYN"),
            CountryCurrency("Belize", "BZD"),
            CountryCurrency("West African CFA", "XOF"),
            CountryCurrency("Bhutan", "BTN"),
            CountryCurrency("Bolivia", "BOB"),
            CountryCurrency("Bosnia and Herzegovina", "BAM"),
            CountryCurrency("Botswana", "BWP"),
            CountryCurrency("Brazil", "BRL"),
            CountryCurrency("Brunei", "BND"),
            CountryCurrency("Bulgaria", "BGN"),
            CountryCurrency("Burundi", "BIF"),
            CountryCurrency("Cambodia", "KHR"),
            CountryCurrency("Central African CFA", "XAF"),
            CountryCurrency("Canada", "CAD"),
            CountryCurrency("Cape Verde", "CVE"),
            CountryCurrency("Chile", "CLP"),
            CountryCurrency("China", "CNY"),
            CountryCurrency("Colombia", "COP"),
            CountryCurrency("Comoros", "KMF"),
            CountryCurrency("Costa Rica", "CRC"),
            CountryCurrency("Cuba", "CUP"),
            CountryCurrency("Czech Republic", "CZK"),
            CountryCurrency("DR Congo", "CDF"),
            CountryCurrency("Denmark", "DKK"),
            CountryCurrency("Djibouti", "DJF"),
            CountryCurrency("Dominican Republic", "DOP"),
            CountryCurrency("United States", "USD"),
            CountryCurrency("Egypt", "EGP"),
            CountryCurrency("Eritrea", "ERN"),
            CountryCurrency("Eswatini", "SZL"),
            CountryCurrency("Ethiopia", "ETB"),
            CountryCurrency("Fiji", "FJD"),
            CountryCurrency("Gambia", "GMD"),
            CountryCurrency("Georgia", "GEL"),
            CountryCurrency("Ghana", "GHS"),
            CountryCurrency("Guatemala", "GTQ"),
            CountryCurrency("Guinea", "GNF"),
            CountryCurrency("Guyana", "GYD"),
            CountryCurrency("Haiti", "HTG"),
            CountryCurrency("Honduras", "HNL"),
            CountryCurrency("Hungary", "HUF"),
            CountryCurrency("Iceland", "ISK"),
            CountryCurrency("India", "INR"),
            CountryCurrency("Indonesia", "IDR"),
            CountryCurrency("Iran", "IRR"),
            CountryCurrency("Iraq", "IQD"),
            CountryCurrency("Israel", "ILS"),
            CountryCurrency("Jamaica", "JMD"),
            CountryCurrency("Japan", "JPY"),
            CountryCurrency("Jordan", "JOD"),
            CountryCurrency("Kazakhstan", "KZT"),
            CountryCurrency("Kenya", "KES"),
            CountryCurrency("North Korea", "KPW"),
            CountryCurrency("South Korea", "KRW"),
            CountryCurrency("Kuwait", "KWD"),
            CountryCurrency("Kyrgyzstan", "KGS"),
            CountryCurrency("Laos", "LAK"),
            CountryCurrency("Lebanon", "LBP"),
            CountryCurrency("Lesotho", "LSL"),
            CountryCurrency("Liberia", "LRD"),
            CountryCurrency("Libya", "LYD"),
            CountryCurrency("Switzerland", "CHF"),
            CountryCurrency("Madagascar", "MGA"),
            CountryCurrency("Malawi", "MWK"),
            CountryCurrency("Malaysia", "MYR"),
            CountryCurrency("Maldives", "MVR"),
            CountryCurrency("Mauritania", "MRO"),
            CountryCurrency("Mauritius", "MUR"),
            CountryCurrency("Mexico", "MXN"),
            CountryCurrency("Moldova", "MDL"),
            CountryCurrency("Mongolia", "MNT"),
            CountryCurrency("Morocco", "MAD"),
            CountryCurrency("Mozambique", "MZN"),
            CountryCurrency("Myanmar", "MMK"),
            CountryCurrency("Namibia", "NAD"),
            CountryCurrency("Nepal", "NPR"),
            CountryCurrency("New Zealand", "NZD"),
            CountryCurrency("Nicaragua", "NIO"),
            CountryCurrency("Nigeria", "NGN"),
            CountryCurrency("North Macedonia", "MKD"),
            CountryCurrency("Norway", "NOK"),
            CountryCurrency("Oman", "OMR"),
            CountryCurrency("Pakistan", "PKR"),
            CountryCurrency("Panama", "PAB"),
            CountryCurrency("Papua New Guinea", "PGK"),
            CountryCurrency("Paraguay", "PYG"),
            CountryCurrency("Peru", "PEN"),
            CountryCurrency("Philippines", "PHP"),
            CountryCurrency("Poland", "PLN"),
            CountryCurrency("Qatar", "QAR"),
            CountryCurrency("Romania", "RON"),
            CountryCurrency("Russia", "RUB"),
            CountryCurrency("Rwanda", "RWF"),
            CountryCurrency("Samoa", "WST"),
            CountryCurrency("São Tomé and Príncipe", "STD"),
            CountryCurrency("Saudi Arabia", "SAR"),
            CountryCurrency("Serbia", "RSD"),
            CountryCurrency("Seychelles", "SCR"),
            CountryCurrency("Sierra Leone", "SLL"),
            CountryCurrency("Singapore", "SGD"),
            CountryCurrency("Solomon Islands", "SBD"),
            CountryCurrency("Somalia", "SOS"),
            CountryCurrency("South Africa", "ZAR"),
            CountryCurrency("South Sudan", "SSP"),
            CountryCurrency("Sri Lanka", "LKR"),
            CountryCurrency("Sudan", "SDG"),
            CountryCurrency("Suriname", "SRD"),
            CountryCurrency("Sweden", "SEK"),
            CountryCurrency("Syria", "SYP"),
            CountryCurrency("Taiwan", "TWD"),
            CountryCurrency("Tajikistan", "TJS"),
            CountryCurrency("Tanzania", "TZS"),
            CountryCurrency("Thailand", "THB"),
            CountryCurrency("Tonga", "TOP"),
            CountryCurrency("Trinidad and Tobago", "TTD"),
            CountryCurrency("Tunisia", "TND"),
            CountryCurrency("Turkey", "TRY"),
            CountryCurrency("Turkmenistan", "TMT"),
            CountryCurrency("Uganda", "UGX"),
            CountryCurrency("Ukraine", "UAH"),
            CountryCurrency("United Arab Emirates", "AED"),
            CountryCurrency("United Kingdom", "GBP"),
            CountryCurrency("Uruguay", "UYU"),
            CountryCurrency("Uzbekistan", "UZS"),
            CountryCurrency("Vanuatu", "VUV"),
            CountryCurrency("Venezuela", "VES"),
            CountryCurrency("Vietnam", "VND"),
            CountryCurrency("Yemen", "YER"),
            CountryCurrency("Zambia", "ZMW"),
            CountryCurrency("Zimbabwe", "ZWL")
        )
        val adapter = CountryCurrencyAdapter(requireContext(), currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set the adapter for the spinner
        currencyEditText.setOnClickListener {
            val dialogFragment = CurrencyDialogFragment().apply {
                setCurrencyData(currencies)
                setTargetFragment(this@AddWalletsFragment, 0)
            }
            dialogFragment.show(parentFragmentManager, "CurrencyDialogFragment")
        }


        binding.btnSelectColor.setOnClickListener {
            ColorPickerDialog
                .Builder(requireContext())
                .setTitle("Select a Color")
                .setColorShape(ColorShape.SQAURE)
                .setDefaultColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                .setColorListener(object : ColorListener {
                    override fun onColorSelected(color: Int, colorHex: String) {
                        selectedColor = colorHex
                        binding.btnSelectColor.setBackgroundColor(color)
                    }
                })
                .show()
        }

        binding.createWalletButton.setOnClickListener {
            val walletName = binding.walletNameEditText.text.toString().trim()
            val currency = binding.currencySpinner.text.toString().trim()
            val amount = binding.amountEditText.text.toString().toDoubleOrNull() ?: 0.0

            if (walletName.isNotEmpty() && currency.isNotEmpty() && currentUser != null) {
                val wallet = hashMapOf(
                    "name" to walletName,
                    "currency" to currency,
                    "amount" to amount,
                    "color" to selectedColor // <-- Save the selected color
                )

                // Add the wallet data to the document with the ID of the current user
                db.collection("users").document(currentUser.uid)
                    .collection("wallets")
                    .add(wallet)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Wallet created successfully!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navigate back to the wallet list fragment
                            findNavController().navigate(R.id.walletsFragment)
                        } else {
                            Log.w(TAG, "Error adding wallet", task.exception)
                            Toast.makeText(
                                requireContext(),
                                "Failed to create wallet. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }


    }

    fun onCurrencySelected(countryCurrency: CountryCurrency) {
        binding.currencySpinner.setText(countryCurrency.currency)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Show the bottom app bar when leaving the fragment
        requireActivity().findViewById<BottomAppBar>(R.id.bottomAppBar).visibility = View.VISIBLE
    }
}