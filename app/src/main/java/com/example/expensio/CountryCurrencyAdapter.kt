package com.example.expensio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class CountryCurrencyAdapter(context: Context, private var countries: List<CountryCurrency>)
    : ArrayAdapter<CountryCurrency>(context, 0, countries) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, android.R.layout.simple_spinner_item)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item)
    }
    override fun getItem(position: Int): CountryCurrency? = countries[position]
    override fun getCount(): Int = countries.size
    fun updateData(newCountries: List<CountryCurrency>) {
        this.countries = newCountries
        notifyDataSetChanged()
    }
    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup, layoutResource: Int): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(layoutResource, parent, false)
        val countryCurrency = getItem(position)
        val text1 = view.findViewById<TextView>(android.R.id.text1)

        if (countryCurrency != null) {
            text1.text = "${countryCurrency.countryName} (${countryCurrency.currency})"

        }

        return view
    }
}
