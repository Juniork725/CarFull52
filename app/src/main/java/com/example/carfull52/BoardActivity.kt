package com.example.carfull52

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.juniork.carfull52.*
import com.juniork.carfull52.databinding.ActivityBoardBinding
import com.juniork.carfull52.databinding.BoardItemBinding
import com.juniork.carfull52.databinding.ReservationItemBinding
import kotlinx.android.synthetic.main.activity_board.view.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/*class MyViewHolder2(val binding: ReservationItemBinding) :
    RecyclerView.ViewHolder(binding.root)*/
class BoardViewHolder(val binding: BoardItemBinding) : RecyclerView.ViewHolder(binding.root)

class BoardAdapter(val datas: MutableList<DocumentSnapshot>, val mContext: BoardActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var toast: Toast? = null
    private val activity = mContext

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            RecyclerView.ViewHolder = BoardViewHolder(BoardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as BoardViewHolder).binding

        val data = datas[position].data!!
        binding.boardItemUserID.text = data.get("userID").toString()
        binding.boardItemText.text = data.get("content").toString()
        binding.boardItemTime.text = data.get("time").toString()
    }
}

class BoardActivity : AppCompatActivity() {
    private var toast: Toast? = null
    var datas = mutableListOf<DocumentSnapshot>()

    fun update(datas: MutableList<DocumentSnapshot>, documentID: String, adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
        datas.clear()
        MyApplication.db.collection("reservation_list").
        document(documentID).
        collection("reply_List").
        get().
        addOnSuccessListener { documents ->
            for (document in documents) {
                Log.d("Jun", "BoardActivity) Document added on datas: ${document}")
                datas.add(document)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Log.w("Jun", "BoardActivity) Getting documents failed with: ", e)
        }
        Log.d("Jun", "BoardActivity) datas: ${datas}")
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val documentID = intent.getSerializableExtra("document ID").toString()
        MyApplication.db.collection("reservation_list").document(documentID).get().addOnSuccessListener { document ->
            binding.reservationItemStartToEnd.text = "행선지: " + document.data!!.get("start") + " >> " + document.data!!.get("end")
            binding.reservationItemDate.text = "날짜: " + document.data!!.get("date")
            binding.reservationItemTime.text = "시간: " + document.data!!.get("time")
            binding.reservationItemID.text = "예약자 ID: " + document.data!!.get("userID")
        }

        binding.boardRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.boardRecyclerView.adapter = BoardAdapter(datas, this)
        update(datas, documentID, binding.boardRecyclerView.adapter!!)

        binding.sendButton.setOnClickListener {
            val reply = mapOf(
                "userID" to MyApplication.ID,
                "time" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")).toString(),
                "content" to binding.boardText.text.toString(),
                "realtime" to LocalDateTime.now().toString()
            )
            binding .boardText.text.clear()
            val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.boardText.windowToken, 0)
            MyApplication.db.collection("reservation_list").document(documentID).collection("reply_List").document(reply.get("realtime")!!).set(reply).addOnSuccessListener {
                Log.d("Jun", "BoardActivity) Reply data added on DB: ${binding.boardText.text}")
                update(datas, documentID ,binding.boardViewRoot.boardRecyclerView.adapter!!)
            }.addOnFailureListener { e ->
                Log.d("Jun", "BoardActivity) Adding reply data on DB failed with: ${e}")
            }
        }
    }
}