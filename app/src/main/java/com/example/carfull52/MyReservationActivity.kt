package com.juniork.carfull52

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carfull52.BoardActivity
import com.juniork.carfull52.databinding.ActivityMyReservationBinding
import com.juniork.carfull52.databinding.ReservationItemBinding
import kotlinx.android.synthetic.main.activity_board.view.*

/*class MyViewHolder2(val binding: ReservationItemBinding) :
    RecyclerView.ViewHolder(binding.root)*/

class MyAdapter2(val datas: MutableList<MyApplication.Companion.Reservation>, val mContext: MyReservationActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var toast: Toast? = null
    private val activity = mContext as MyReservationActivity

    override fun getItemCount(): Int {
        return datas.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            RecyclerView.ViewHolder = MyViewHolder(ReservationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as MyViewHolder).binding
        val documentID = datas[position].email + datas[position].date + datas[position].time
        binding.reservationItemStart.text = datas[position].start
        binding.reservationItemEnd.text = datas[position].end
        binding.reservationItemTime.text = "예약 시간) " + datas[position].time
        binding.reservationItemUserID.text = "작성자: " + datas[position].userID
        binding.reservationItemCross.visibility = View.VISIBLE

        binding.reservationItemCross.setOnClickListener {
            val alertDialog = androidx.appcompat.app.AlertDialog.Builder(activity)
            alertDialog.setTitle("예약 삭제")
            alertDialog.setMessage("예약 기록을 삭제하시겠습니까?")
            alertDialog.setPositiveButton("확인") {_,_  ->
                MyApplication.db.collection("reservation_list").
                document(documentID).
                get().
                addOnSuccessListener { document ->
                    Log.d("Jun", "MyReservationActivity) Document ID for delete: ${document.id}")
                    MyApplication.db.collection("reservation_list").document(document.id).delete()
                }.addOnFailureListener { e ->
                    Log.w("Jun", "MyReservationActivity) Error getting documents: ", e)
                }
                datas.remove(datas[position])

                notifyDataSetChanged()

                toast?.cancel()
                toast = Toast.makeText(activity, "예약이 취소되었습니다.", Toast.LENGTH_SHORT)
                toast!!.show()
            }
            alertDialog.setNegativeButton("닫기") {_,_ ->
                alertDialog.create().dismiss()
            }
            alertDialog.show()
        }

        holder.itemView.setOnClickListener{
            val intent = Intent(activity, BoardActivity::class.java)
            intent.putExtra("document ID", documentID)
            ContextCompat.startActivity(activity, intent, null)
        }
    }
}

class MyDecoration2(val context: Context): RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.set(10, 10, 10, 10)
        view.setBackgroundColor(Color.rgb(0xFB,0xF7,0x96))
        ViewCompat.setElevation(view, 20.0f)
    }
}

class MyReservationActivity : AppCompatActivity() {
    var datas = mutableListOf<MyApplication.Companion.Reservation>()
    //val context_main: Context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMyReservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MyApplication.db.collection("reservation_list").
        whereEqualTo("email",MyApplication.auth.currentUser!!.email).
        get().
        addOnSuccessListener { documents ->
            Log.d("Jun", "MyReservationActivity) documents: ${documents}")
            for (document in documents) {
                Log.d("Jun", "MyReservationActivity) datas add ${document}")
                datas.add(document.toObject(MyApplication.Companion.Reservation::class.java))
            }
            binding.reservationRecyclerView.adapter!!.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Log.w("Jun", "Error getting documents: ", e)
        }

        binding.reservationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.reservationRecyclerView.adapter = MyAdapter2(datas, this)
        binding.reservationRecyclerView.addItemDecoration(MyDecoration2(this))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}