package com.doxel.stylusnotes

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DrawingActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private var existingFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)

        drawingView = findViewById(R.id.drawingView)

        existingFileName = intent.getStringExtra(EXTRA_FILE_NAME)
        existingFileName?.let { name ->
            val file = File(notesDir(), name)
            if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.let {
                    drawingView.loadBitmap(it)
                }
            }
        }

        findViewById<android.widget.Button>(R.id.btnUndo).setOnClickListener {
            drawingView.undo()
        }
        findViewById<android.widget.Button>(R.id.btnClear).setOnClickListener {
            drawingView.clear()
        }
        findViewById<android.widget.Button>(R.id.btnSave).setOnClickListener {
            saveNote()
        }
        findViewById<android.widget.Button>(R.id.btnSettings).setOnClickListener {
            showPalmSettingsDialog()
        }
    }

    private fun showPalmSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_palm_settings, null)
        val seek = view.findViewById<SeekBar>(R.id.seekSensitivity)
        val label = view.findViewById<TextView>(R.id.txtSensitivityValue)
        val checkZone = view.findViewById<CheckBox>(R.id.checkWritingZone)

        // maxContactSize va de 0.02 (très strict, ne laisse passer que des pointes très fines)
        // à 0.30 (permissif). On mappe le SeekBar 0..100 sur cette plage.
        fun sizeFromProgress(p: Int) = 0.02f + (p / 100f) * 0.28f
        fun progressFromSize(s: Float) = (((s - 0.02f) / 0.28f) * 100f).toInt().coerceIn(0, 100)

        seek.progress = progressFromSize(drawingView.maxContactSize)
        label.text = getString(R.string.palm_sensitivity_value, drawingView.maxContactSize)
        checkZone.isChecked = drawingView.restrictToWritingZone

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = sizeFromProgress(progress)
                drawingView.maxContactSize = v
                label.text = getString(R.string.palm_sensitivity_value, v)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        checkZone.setOnCheckedChangeListener { _, checked ->
            drawingView.restrictToWritingZone = checked
            drawingView.writingZoneTopRatio = if (checked) 0.15f else 0f
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.palm_settings)
            .setView(view)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun notesDir(): File {
        val dir = File(filesDir, "notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun saveNote() {
        val bmp = drawingView.exportBitmap() ?: return
        val name = existingFileName ?: run {
            val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            "note_$ts.png"
        }
        val file = File(notesDir(), name)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        existingFileName = name
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_FILE_NAME = "extra_file_name"
    }
}
