package com.mandrykevich.myhelper.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.data.repository.SearchQuery

class SearchQueryAdapter(private val queries: List<SearchQuery>) : RecyclerView.Adapter<SearchQueryAdapter.QueryViewHolder>() {

    class QueryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val queryTextView: TextView = itemView.findViewById(R.id.text_name)

        fun bind(query: SearchQuery) {
            queryTextView.text = query.query
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return QueryViewHolder(view)
    }

    override fun onBindViewHolder(holder: QueryViewHolder, position: Int) {
        holder.bind(queries[position])
    }

    override fun getItemCount(): Int = queries.size
}