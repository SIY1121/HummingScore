package space.siy.hummingscore

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_list.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import space.siy.hummingscore.humming.HummingFile
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

@RuntimePermissions
class ListActivity : AppCompatActivity() {
    lateinit var adapter: Adapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        adapter = Adapter(this, R.layout.item_list, arrayListOf())
        updateFileListWithPermissionCheck()
        humming_list_view.adapter = adapter
        humming_list_view.setOnItemClickListener { _, _, pos, path ->
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("path", adapter.items[pos].file.absolutePath)
            startActivity(intent)
        }
        new_humming_button.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun updateFileList() {
        val files = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/").listFiles()
            .filter { it.extension == "json" }
            .map { HummingFile(it.absolutePath) }.toMutableList()
        adapter.items.clear()
        adapter.items.addAll(files)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        updateFileList()
    }

    class Adapter(val context: Context, val itemLayoutId: Int, val items: MutableList<HummingFile>) : BaseAdapter() {
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = items.size

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: inflater.inflate(itemLayoutId, parent, false).apply {
                val holder = ViewHolder(this)
                tag = holder
            }

            val holder = v.tag as ViewHolder
            holder.titleTextView.text = items[position].hummingData.name

            holder.dateTextView.text = Date(items[position].file.lastModified()).toString()

            return v
        }

        class ViewHolder(val view: View) {
            val titleTextView: TextView = view.findViewById(R.id.humming_title_text_view)
            val dateTextView: TextView = view.findViewById(R.id.humming_date_text_view)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
}
