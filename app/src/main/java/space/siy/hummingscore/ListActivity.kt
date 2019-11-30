package space.siy.hummingscore

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_list.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import space.siy.hummingscore.humming.HummingFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

@RuntimePermissions
class ListActivity : AppCompatActivity() {
    lateinit var adapter: Adapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = "Humming リスト"
        adapter = Adapter(this, R.layout.item_list, arrayListOf())
        updateFileListWithPermissionCheck()
        humming_list_view.adapter = adapter
        humming_list_view.setOnItemClickListener { _, _, pos, path ->
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("path", adapter.items[pos].file.absolutePath)
            startActivity(intent)
        }
        humming_list_view.setOnItemLongClickListener { parent, view, position, id ->
            AlertDialog.Builder(this).setTitle("削除しますか？").setPositiveButton("はい") { _, _ ->
                adapter.items[position].file.delete()
                adapter.items.removeAt(position)
                adapter.notifyDataSetChanged()
            }.setNegativeButton("キャンセル", null).show()
            true
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
        File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/").mkdir()
        val files = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/").listFiles()
            .filter { it.extension == "json" }
            .map { HummingFile(it.absolutePath) }.toMutableList()
        adapter.items.clear()
        adapter.items.addAll(files)
        adapter.notifyDataSetChanged()
        empty_text_view.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
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

            holder.dateTextView.text =
                SimpleDateFormat(
                    "yyyy/MM/dd E hh:mm:ss",
                    Locale.JAPAN
                ).format(Date(items[position].file.lastModified()))

            holder.lenTextView.text =
                (60f / items[position].hummingData.option.bpm / (items[position].hummingData.option.noteResolution / 4) * items[position].hummingData.notes.size).toInt().toString() + "秒"
            holder.sizeTextView.text = ((items[position].file.length() / 1024.0 * 10.0).toInt() / 10f).toString() + "MB"
            return v
        }

        class ViewHolder(val view: View) {
            val titleTextView: TextView = view.findViewById(R.id.humming_title_text_view)
            val dateTextView: TextView = view.findViewById(R.id.humming_date_text_view)
            val lenTextView: TextView = view.findViewById(R.id.humming_len_text_view)
            val sizeTextView: TextView = view.findViewById(R.id.humming_size_text_view)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.info_button -> AlertDialog.Builder(this).setTitle("HummingScore β v0.1.0").setMessage(
                """
                    このアプリはベータ版です。
                    Developer: @SIY1121
                """.trimIndent()
            ).setPositiveButton("OK", null).show()
            R.id.license_button -> startActivity(Intent(this, OssLicensesMenuActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
}
