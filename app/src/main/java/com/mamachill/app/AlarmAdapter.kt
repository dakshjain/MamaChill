package com.mamachill.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mamachill.app.data.Alarm
import com.mamachill.app.databinding.ItemAlarmBinding
import java.util.Locale

class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onDelete: (Alarm) -> Unit,
    private val onClick: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemAlarmBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: Alarm) {
            val hour12 = when {
                alarm.hour == 0 -> 12
                alarm.hour > 12 -> alarm.hour - 12
                else -> alarm.hour
            }
            binding.tvTime.text = String.format(Locale.getDefault(), "%d:%02d", hour12, alarm.minute)
            binding.tvAmPm.text = if (alarm.hour < 12) "AM" else "PM"
            binding.tvLabel.text = alarm.label.ifEmpty { "Alarm" }
            binding.tvRepeat.text = repeatText(alarm.repeatDays)
            binding.switchEnabled.isChecked = alarm.isEnabled

            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.setOnCheckedChangeListener { _, checked -> onToggle(alarm, checked) }
            binding.btnDelete.setOnClickListener { onDelete(alarm) }
            binding.root.setOnClickListener { onClick(alarm) }
        }

        private fun repeatText(days: Int): String {
            if (days == 0) return "Once"
            if (days == 127) return "Every day"
            val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            return (0..6).filter { days and (1 shl it) != 0 }.joinToString(", ") { names[it] }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(old: Alarm, new: Alarm) = old.id == new.id
        override fun areContentsTheSame(old: Alarm, new: Alarm) = old == new
    }
}
