package com.example.expensio

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.expensio.Wallets.AddWalletsFragment
import com.example.expensio.databinding.FragmentCurrencyDialogBinding

class CurrencyDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentCurrencyDialogBinding
    private var currencies: List<CountryCurrency> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCurrencyDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        val adapter = CountryCurrencyAdapter(requireContext(), currencies)
        binding.currencyList.adapter = adapter

        binding.currencySearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val filteredCurrencies = currencies.filter {
                    it.currency.contains(s.toString(), ignoreCase = true) ||
                            it.countryName.contains(s.toString(), ignoreCase = true)
                }
                (binding.currencyList.adapter as CountryCurrencyAdapter).updateData(filteredCurrencies)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.currencyList.setOnItemClickListener { _, _, position, _ ->
            (binding.currencyList.adapter as CountryCurrencyAdapter).getItem(position)
                ?.let { (targetFragment as? AddWalletsFragment)?.onCurrencySelected(it) }
            dismiss()
        }
    }

    fun setCurrencyData(currencies: List<CountryCurrency>) {
        this.currencies = currencies
    }
}