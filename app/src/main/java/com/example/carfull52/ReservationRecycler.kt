package com.juniork.carfull52

import android.app.AlertDialog
import android.app.ProgressDialog.show
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Build.ID
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carfull52.BoardActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.juniork.carfull52.MyApplication
import com.juniork.carfull52.MyApplication.Companion.auth
import com.juniork.carfull52.databinding.ReservationItemBinding
import com.juniork.carfull52.databinding.ReservationRecyclerBinding
import io.grpc.InternalChannelz.id
import kotlinx.android.synthetic.main.activity_join.*
import kotlinx.android.synthetic.main.reservation_dialogfragment.*
import kotlinx.android.synthetic.main.reservation_dialogfragment.view.*
import kotlinx.android.synthetic.main.reservation_recycler.view.*
import org.threeten.bp.LocalDate
import java.sql.Time
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.Serializable

class MyViewHolder(val binding: ReservationItemBinding) :
        RecyclerView.ViewHolder(binding.root)

class MyAdapter(val datas: MutableList<MyApplication.Companion.Reservation>, val mContext: ReservationRecycler) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount(): Int {
        return datas.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            RecyclerView.ViewHolder = MyViewHolder(ReservationItemBinding.inflate(LayoutInflater.from(
        parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as MyViewHolder).binding
        val documentID = datas[position].email + datas[position].date + datas[position].time

        binding.reservationItemStart.text = datas[position].start
        binding.reservationItemEnd.text = datas[position].end
        binding.reservationItemTime.text = "예약 시간) " + datas[position].time
        binding.reservationItemUserID.text = "작성자: " + datas[position].userID

        holder.itemView.setOnClickListener{
            val intent = Intent(mContext, BoardActivity::class.java)
            intent.putExtra("document ID", documentID)
            ContextCompat.startActivity(mContext, intent, null)
        }
    }


}

class MyDecoration(val context: Context): RecyclerView.ItemDecoration() {
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

class ReservationRecycler : AppCompatActivity() {
    private var toast: Toast? = null
    var datas = mutableListOf<MyApplication.Companion.Reservation>()

    fun update(datas: MutableList<MyApplication.Companion.Reservation>, reservation: MyApplication.Companion.Reservation, adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
        datas.clear()
        MyApplication.db.collection("reservation_list").
        whereEqualTo("date",reservation.date).whereEqualTo("start",reservation.start).whereEqualTo("end", reservation.end).
        get().
        addOnSuccessListener { documents ->
            Log.d("Jun", "documents: ${documents}")
            for (document in documents) {
                Log.d("Jun", "datas add ${document}")
                datas.add(document.toObject(MyApplication.Companion.Reservation::class.java))
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Log.w("Jun", "Error getting documents: ", e)
        }
        Log.d("Jun", "datas: ${datas}")
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ReservationRecyclerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.reservationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.reservationRecyclerView.adapter = MyAdapter(datas, this)
        binding.reservationRecyclerView.addItemDecoration(MyDecoration(this))

        val reservation = intent.getSerializableExtra("reservation") as MyApplication.Companion.Reservation
        binding.reservationRecyclerViewDate.text = reservation.date
        update(datas, reservation, binding.reservationRecyclerView.adapter!!)

        var hour: Int? = null
        var min: Int? = null
        var DocumentID = ""
        var dialog = TimePickerDialog(this, object: TimePickerDialog.OnTimeSetListener{
            override fun onTimeSet(p0: TimePicker?, p1: Int, p2: Int) {
                hour = p1
                min = p2
                reservation.time = String.format("%02d:%02d", hour, min)
                DocumentID = reservation.email + reservation.date + reservation.time

                Log.d("Jun", "ID set as ${DocumentID}")
                MyApplication.db.collection("reservation_list").
                document(DocumentID).
                get().
                addOnSuccessListener { document ->
                    if (document.data != null && document.toObject(MyApplication.Companion.Reservation::class.java)!!.email!! == auth.currentUser!!.email) {
                        Log.d("Jun", "Document data: ${document.data}")
                        toast?.cancel()
                        toast = Toast.makeText(this@ReservationRecycler, "이미 해당 시간에 예약을 하셨습니다.", Toast.LENGTH_SHORT)
                        toast?.show()
                    } else {
                        MyApplication.db.collection("reservation_list").
                        document(DocumentID).
                        set(reservation).addOnSuccessListener {
                            Log.d("Jun", "ReservationDocument added with ID: ${ID}")

                            toast?.cancel()
                            toast = Toast.makeText(this@ReservationRecycler, "예약이 추가되었습니다.", Toast.LENGTH_SHORT)
                            toast?.show()

                            update(datas, reservation, binding.reservationRecyclerView.adapter!!)
                        }.addOnFailureListener { e ->
                            Log.d("Jun", "Adding ReservationDocument failed with ${e}")
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.w("Jun", "get failed with", e)
                    toast?.cancel()
                    toast = Toast.makeText(this@ReservationRecycler, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT)
                    toast?.show()
                }
            }
        },0,0,false)
        dialog.setTitle("예약 시간 설정")

        binding.reservationRecyclerViewFab.setOnClickListener {
            dialog.show()
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        binding.reservationRecyclerViewLeftArrow.setOnClickListener {
            val date: LocalDate = LocalDate.parse(reservation.date, formatter).minusDays(1)
            reservation.date = date.format(formatter)
            update(datas, reservation, findViewById<RelativeLayout>(R.id.reservationRecyclerViewMain).reservationRecyclerView.adapter!!)
            binding.reservationRecyclerViewDate.text = reservation.date
        }

        binding.reservationRecyclerViewRightArrow.setOnClickListener {
            val date: LocalDate = LocalDate.parse(reservation.date, formatter).plusDays(1)
            reservation.date = date.format(formatter)
            update(datas, reservation, findViewById<RelativeLayout>(R.id.reservationRecyclerViewMain).reservationRecyclerView.adapter!!)
            binding.reservationRecyclerViewDate.text = reservation.date
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}