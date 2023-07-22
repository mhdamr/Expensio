package com.example.expensio

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import java.util.*

class MonthYearPickerDialog : DialogFragment() {

    private var listener: DatePickerDialog.OnDateSetListener? = null

    fun setListener(listener: DatePickerDialog.OnDateSetListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = view.findViewById<NumberPicker>(R.id.monthPicker)
        val yearPicker = view.findViewById<NumberPicker>(R.id.yearPicker)

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.value = month
        monthPicker.displayedValues = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        yearPicker.minValue = 1970
        yearPicker.maxValue = 2100
        yearPicker.value = year

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                listener?.onDateSet(null, yearPicker.value, monthPicker.value, 1)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}