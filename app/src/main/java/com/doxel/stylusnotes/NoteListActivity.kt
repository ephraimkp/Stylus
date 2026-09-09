package com.doxel.stylusnotes

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class NoteListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        recycler = findViewById(R.id.recyclerNotes)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = NoteAdapter(emptyList()) { fileName ->
            val intent = Intent(this, DrawingActivity::class.java)
            intent.putExtra(DrawingActivity.EXTRA_FILE_NAME, fileName)
            startActivity(intent)
        }
        recycler.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabNewNote).setOnClickListener {
            startActivity(Intent(this, DrawingActivity::class.java))
        }
        findViewById<android.widget.Button>(R.id.btnDebug).setOnClickListener {
            startActivity(Intent(this, TouchDebugActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val dir = File(filesDir, "notes")
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        adapter.update(files.map { it.name })
    }
}

class NoteAdapter(
    private var items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<NoteAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.imgThumb)
        val name: TextView = view.findViewById(R.id.txtName)
    }

    fun update(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val fileName = items[position]
        holder.name.text = fileName
        holder.itemView.setOnClickListener { onClick(fileName) }

        val dir = File(holder.itemView.context.filesDir, "notes")
        val file = File(dir, fileName)
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
        holder.thumb.setImageBitmap(bmp)
    }

    override fun getItemCount() = items.size
}
