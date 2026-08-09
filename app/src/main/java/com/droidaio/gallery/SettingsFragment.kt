package com.droidaio.gallery

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsFragment : Fragment() {

    private lateinit var recycler : RecyclerView
    private val options = listOf(
        ThemeManager.ThemeChoice.SYSTEM to getStringSafe(R.string.pref_theme_system),
        ThemeManager.ThemeChoice.LIGHT to getStringSafe(R.string.pref_theme_light),
        ThemeManager.ThemeChoice.DARK to getStringSafe(R.string.pref_theme_dark),
        ThemeManager.ThemeChoice.PURE_BLACK to getStringSafe(R.string.pref_theme_pure_black)
    )

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        recycler = root.findViewById(R.id.themeOptionsRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = ThemeAdapter()
        return root
    }

    private fun getStringSafe(id : Int) : String {
        return try {
            getString(id)
        } catch (e : Exception) {
            id.toString()
        }
    }

    inner class ThemeAdapter : RecyclerView.Adapter<ThemeAdapter.VH>() {
        private var selected = ThemeManager.getSavedTheme(requireContext())

        inner class VH(view : View) : RecyclerView.ViewHolder(view) {
            val label : TextView = view.findViewById(R.id.label)
            val radio : RadioButton = view.findViewById(R.id.radio)
        }

        override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : VH {
            val v = layoutInflater.inflate(R.layout.item_theme_option, parent, false)
            return VH(v)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder : VH, position : Int) {
            val (choice, labelText) = options[position]
            holder.label.text = labelText
            holder.radio.isChecked = (choice == selected)
            holder.itemView.setOnClickListener {
                selected = choice
                ThemeManager.applyTheme(requireContext(), choice)
                // If PureBlack selected, set Activity theme explicitly and recreate to apply
                if (choice == ThemeManager.ThemeChoice.PURE_BLACK) {
                    requireActivity().setTheme(R.style.Theme_PhotoGallery_PureBlack)
                } else {
                    // ensure default theme is used (Theme.Gallery)
                    requireActivity().setTheme(R.style.Theme_PhotoGallery_Default)
                }
                notifyDataSetChanged()
                requireActivity().recreate()
            }
            holder.radio.setOnClickListener {
                holder.itemView.performClick()
            }
        }

        override fun getItemCount() : Int = options.size
    }
}

