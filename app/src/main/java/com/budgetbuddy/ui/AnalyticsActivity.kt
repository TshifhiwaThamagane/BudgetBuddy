package com.budgetbuddy.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.databinding.ActivityAnalyticsBinding
import com.budgetbuddy.ui.viewmodel.AnalyticsViewModel
import com.budgetbuddy.util.CurrencyUtils
import com.budgetbuddy.util.DateUtils
import com.budgetbuddy.util.SessionManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Calendar

class AnalyticsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AnalyticsActivity"
    }

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var viewModel: AnalyticsViewModel
    private lateinit var sessionManager: SessionManager

    private val chartColors = intArrayOf(
        R.color.chart_blue,
        R.color.chart_orange,
        R.color.chart_purple,
        R.color.chart_teal,
        R.color.chart_red,
        R.color.green_500,
        R.color.green_300
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        val repository = BudgetRepository.getInstance(this)
        viewModel = ViewModelProvider(
            this,
            AnalyticsViewModelFactory(repository)
        )[AnalyticsViewModel::class.java]

        binding.etStartDate.setText(DateUtils.toDisplay(DateUtils.startOfMonth()))
        binding.etEndDate.setText(DateUtils.today())

        binding.etStartDate.setOnClickListener { showDatePicker(binding.etStartDate) }
        binding.etEndDate.setOnClickListener { showDatePicker(binding.etEndDate) }

        binding.btnApplyFilter.setOnClickListener {
            viewModel.loadAnalytics(
                sessionManager.getUserId(),
                binding.etStartDate.text.toString(),
                binding.etEndDate.text.toString()
            )
        }

        observeState()
        viewModel.loadAnalytics(sessionManager.getUserId())
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            state.budgetGoal?.let {
                binding.tvMinGoalAnalytics.text = CurrencyUtils.toRand(it.minGoal)
                binding.tvMaxGoalAnalytics.text = CurrencyUtils.toRand(it.maxGoal)
            }

            if (state.isEmpty && !state.isLoading) {
                binding.tvEmptyAnalytics.visibility = View.VISIBLE
                binding.cardPieChart.visibility = View.GONE
                binding.cardBarChart.visibility = View.GONE
                binding.cardLineChart.visibility = View.GONE
                return@observe
            }

            binding.tvEmptyAnalytics.visibility = View.GONE
            binding.cardPieChart.visibility = View.VISIBLE
            binding.cardBarChart.visibility = View.VISIBLE
            binding.cardLineChart.visibility = View.VISIBLE

            setupPieChart(state.categorySpending)
            setupBarChart(state.categorySpending)
            setupLineChart(state.dailySpending)

            state.error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupPieChart(data: List<com.budgetbuddy.data.dao.CategorySpending>) {
        if (data.isEmpty()) {
            binding.pieChart.clear()
            binding.pieChart.setNoDataText(getString(R.string.no_data_analytics))
            return
        }

        val entries = data.map { PieEntry(it.total.toFloat(), it.categoryName) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors.map { ContextCompat.getColor(this@AnalyticsActivity, it) }
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = CurrencyUtils.toRand(value.toDouble())
            }
        }
        binding.pieChart.apply {
            this.data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setEntryLabelTextSize(11f)
            animateY(800)
            invalidate()
        }
        Log.d(TAG, "PieChart updated with ${data.size} categories")
    }

    private fun setupBarChart(data: List<com.budgetbuddy.data.dao.CategorySpending>) {
        if (data.isEmpty()) {
            binding.barChart.clear()
            binding.barChart.setNoDataText(getString(R.string.no_data_analytics))
            return
        }

        val entries = data.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.total.toFloat())
        }
        val labels = data.map { it.categoryName }
        val dataSet = BarDataSet(entries, "Spending").apply {
            colors = chartColors.map { ContextCompat.getColor(this@AnalyticsActivity, it) }
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = CurrencyUtils.toRand(value.toDouble())
            }
        }
        binding.barChart.apply {
            this.data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false
            animateY(800)
            invalidate()
        }
        Log.d(TAG, "BarChart updated with ${data.size} categories")
    }

    private fun setupLineChart(data: List<com.budgetbuddy.data.dao.DailySpending>) {
        if (data.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.setNoDataText(getString(R.string.no_data_analytics))
            return
        }

        val entries = data.mapIndexed { index, item ->
            Entry(index.toFloat(), item.total.toFloat())
        }
        val labels = data.map { it.date }
        val dataSet = LineDataSet(entries, "Daily Spending").apply {
            color = ContextCompat.getColor(this@AnalyticsActivity, R.color.chart_blue)
            setCircleColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.chart_blue))
            lineWidth = 2f
            circleRadius = 4f
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@AnalyticsActivity, R.color.green_100)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = CurrencyUtils.toRand(value.toDouble())
            }
        }
        binding.lineChart.apply {
            this.data = LineData(dataSet)
            description.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                labelRotationAngle = -45f
                setDrawGridLines(false)
            }
            axisRight.isEnabled = false
            animateX(800)
            invalidate()
        }
        Log.d(TAG, "LineChart updated with ${data.size} data points")
    }

    private fun showDatePicker(editText: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selected = Calendar.getInstance()
                selected.set(year, month, day)
                editText.setText(DateUtils.formatDisplay(selected))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

class AnalyticsViewModelFactory(
    private val repository: BudgetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AnalyticsViewModel(repository) as T
    }
}
