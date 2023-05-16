package com.juniork.carfull52

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.juniork.carfull52.databinding.ActivityMainBinding
import org.threeten.bp.format.DateTimeFormatter
import java.io.Serializable
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        val today = GregorianCalendar()
        var year: Int = today.get(Calendar.YEAR)
        var month: Int = today.get(Calendar.MONTH)
        var date: Int = today.get(Calendar.DATE)
        var dateText: String = String.format("%1$04d년 %2$02d월 %3$02d일", year, month+1, date)
        binding.dateTextView.text = dateText
        binding.dateTextView.setOnClickListener {
            DatePickerDialog(this, object: DatePickerDialog.OnDateSetListener{
                override fun onDateSet(p0: DatePicker?, p1: Int, p2: Int, p3: Int) {
                    year = p1
                    month = p2
                    date = p3
                    dateText = String.format("%1$04d년 %2$02d월 %3$02d일", year, month+1, date)
                    binding.dateTextView.text = dateText
                }
            }, year, month, date).show()
        }

        val locations = arrayOf<String>("52여단","원통","서화","인제")
        var fromLocation = ""
        var toLocation = ""
        binding.fromLocationText.setOnClickListener {
            AlertDialog.Builder(this).run {
                setTitle("출발지 선택")
                setItems(locations, object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        fromLocation = locations[p1]
                        binding.fromLocationText.text = fromLocation
                    }
                })
                setPositiveButton("닫기",null)
                show()
            }

        }
        binding.toLocationText.setOnClickListener {
            AlertDialog.Builder(this).run {
                setTitle("도착지 선택")
                setItems(locations, object : DialogInterface.OnClickListener {
                    override fun onClick(p0: DialogInterface?, p1: Int) {
                        toLocation = locations[p1]
                        binding.toLocationText.text = toLocation
                    }
                })
                setPositiveButton("닫기",null)
                show()
            }

        }

        binding.reservationButton.setOnClickListener {
            if (fromLocation != "" && toLocation != "") {
                val reservation = MyApplication.Companion.Reservation()
                reservation.email = MyApplication.email
                reservation.userID = MyApplication.ID!!
                reservation.start = fromLocation
                reservation.end = toLocation
                reservation.date = dateText
                reservation.time = null
                val intent = Intent(this, ReservationRecycler::class.java)
                intent.putExtra("reservation", reservation)
                ContextCompat.startActivity(this, intent, null)
            } else {
                toast?.cancel()
                toast = Toast.makeText(this, "목적지와 도착지를 설정해주세요", Toast.LENGTH_SHORT)
                toast?.show()
            }
        }

        binding.twoArrowButton.setOnClickListener {
            var temp = fromLocation
            fromLocation = toLocation
            toLocation = temp
            binding.fromLocationText.text = fromLocation
            binding.toLocationText.text = toLocation
        }

        binding.logout.setOnClickListener {
            MyApplication.auth.signOut()
            toast?.cancel()
            toast = Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT)
            toast?.show()
            finish()
        }

        binding.deleteAccount.setOnClickListener {
            val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            alertDialog.setTitle("계정 삭제")
            alertDialog.setMessage("계정 삭제 후에는 복구가 불가능하며, 삭제 후에도 작성한 예약글은 유지됩니다.\n계정을 삭제하시겠습니까?")
            alertDialog.setPositiveButton("확인") {_,_ ->
                MyApplication.auth.currentUser!!.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        MyApplication.db.collection("ID_List").document(MyApplication.ID.toString()).delete()
                            .addOnSuccessListener {
                            Log.d("Jun", "MainActivity) ${MyApplication.ID} document is deleted")
                            }.addOnFailureListener { e ->
                                Log.d("Jun", "MainActivity) Deleting document failed with ", e)
                            }
                        toast?.cancel()
                        toast = Toast.makeText(this, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT)
                        toast?.show()

                        MyApplication.auth.signOut()
                        finish()
                    } else {
                        toast?.cancel()
                        toast = Toast.makeText(this, "계정 삭제 오류.", Toast.LENGTH_SHORT)
                        toast?.show()
                        Log.d("Jun", "MainActivity) Deleting account failed with ", task.exception)
                    }
                }
            }
            alertDialog.setNegativeButton("닫기") {dialogInterface,_ ->
                dialogInterface.dismiss()
            }
            alertDialog.show()
        }
    }

    override fun onStop() {
        super.onStop()
        toast?.cancel()
    }

    var initTime = 0L
    override fun onBackPressed() {
        if (System.currentTimeMillis() - initTime > 3000) {
            toast?.cancel()
            toast = Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT)
            toast?.show()
            initTime = System.currentTimeMillis()
        } else {
            ActivityCompat.finishAffinity(this)
            System.runFinalization()
            exitProcess(0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.MyReservationMenuIcon) {
            Log.d("Jun", "Menu selected")
            val intent = Intent(this, MyReservationActivity::class.java)
            ContextCompat.startActivity(this, intent, null)
        }
        return super.onOptionsItemSelected(item)
    }
}