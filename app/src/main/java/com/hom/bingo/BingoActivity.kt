package com.hom.bingo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hom.bingo.databinding.ActivityBingoBinding
import com.hom.bingo.databinding.SignleButtonBinding

class BingoActivity : AppCompatActivity() {
    private var myTurn: Boolean = false
        set(value) {
            field = value
            tvInfo.setText(if (value) "請選號" else "等對手選號")
        }
    private lateinit var adapter: FirebaseRecyclerAdapter<Boolean, NumberHolder>
    private lateinit var recycler: RecyclerView
    private lateinit var tvInfo: TextView
    private lateinit var binding: ActivityBingoBinding
    private var NUMBER_COUNT: Int = 25
    private var isCreator: Boolean = false

    private var roomId: String = ""
    private val TAG: String? = BingoActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBingoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        roomId = intent.getStringExtra("ROOM_ID").toString()
        isCreator = intent.getBooleanExtra("IS_CREATOR", false)

        if (isCreator) {
            for (i in 0 until NUMBER_COUNT) {
                val number = (i + 1).toString()
                FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId).child("numbers").child(number).setValue(false)
            }
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId).child("status")
                .setValue(Room.STATUS_CREATOR)
        } else {
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId).child("status")
                .setValue(Room.STATUS_JOINIES)
        }

        generRandomNumber()
        findviews()

    }

    var buttons = mutableListOf<NumberButton>()
    var numberMap = mutableMapOf<Int, NumberButton>()
    private fun generRandomNumber() {
        val randomNumbers = mutableListOf<Int>()
        for (i in 0 until NUMBER_COUNT) {
            randomNumbers.add(i + 1)
        }
        randomNumbers.shuffle()

        for (i in 0 until NUMBER_COUNT) {
            var button = NumberButton(this)
            button.apply {
                number = randomNumbers.get(i)
                text = number.toString()
                pos = i
            }.also {
                buttons.add(it)
                numberMap.put(it.number, it)
            }
        }
    }

    private fun findviews() {
        tvInfo = binding.tvInfo
        recycler = binding.recyclerButton
        recycler.setHasFixedSize(true)
        recycler.layoutManager = GridLayoutManager(this, 5)
        val query = FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomId).child("numbers").orderByKey()
        val option = FirebaseRecyclerOptions.Builder<Boolean>()
            .setQuery(query, Boolean::class.java).build()
        adapter = object : FirebaseRecyclerAdapter<Boolean, NumberHolder>(option) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberHolder {
                val v = SignleButtonBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return NumberHolder(v)
            }

            override fun onBindViewHolder(holder: NumberHolder, position: Int, model: Boolean) {
                holder.numberButton.setText(buttons.get(position).number.toString())
                holder.numberButton.isEnabled = !buttons.get(position).picked
                holder.itemView.setOnClickListener {
                    if (myTurn) {
                        val number = buttons.get(position).number.toString()
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId).child("numbers").child(number)
                            .setValue(true)

                    }
                }
            }

            override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DataSnapshot,
                newIndex: Int,
                oldIndex: Int
            ) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex)
                if (type == ChangeEventType.CHANGED) {
                    val myKey = snapshot.key?.toInt()
                    val myPos = numberMap.get(myKey)?.pos
                    val holder = recycler.findViewHolderForAdapterPosition(myPos!!) as NumberHolder
                    holder.numberButton.isEnabled = false
                    numberMap.get(myKey)?.picked = true

                    if (myTurn) {
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId).child("status")
                            .setValue(if (isCreator) Room.STATUS_JOINTED_TURN else Room.STATUS_CREATED_TURN)
                    }

                    var bingo = 0
                    var sum = 0
                    var nums = mutableListOf<Int>()
                    for (i in 0 until NUMBER_COUNT) {
                        nums.add(if (buttons.get(i).picked) 1 else 0)
                    }
                    for (i in 0 until 5) {
                        sum = 0
                        for (j in 0 until 5) {
                            sum += nums[i * 5 + j]
                        }
                        bingo += if (sum == 5) 1 else 0
                        sum = 0
                        for (j in 0 until 5) {
                            sum += nums[j * 5 + i]
                        }
                        bingo += if (sum == 5) 1 else 0
                    }

                    if (bingo > 0) {
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId).child("status")
                            .setValue(if (isCreator) Room.STATUS_CREATED_BINGO else Room.STATUS_JOINTED_BINGO)
                        AlertDialog.Builder(this@BingoActivity)
                            .setTitle(" End Game ")
                            .setMessage(" Yout Win ")
                            .setIcon(R.drawable.yahoo)
                            .setPositiveButton("OK") { ok, which ->
                                endGame()
                            }
                            .show()
                    }

                }
            }//end-onChildChanged

        }
        recycler.adapter = adapter

    }

    class NumberHolder(var view: SignleButtonBinding) : RecyclerView.ViewHolder(view.root) {
        val numberButton = view.viewButton
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
        FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomId).child("status").addValueEventListener(statusListener)
    }

    val statusListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.getValue() != null) {
                val status = snapshot.getValue() as Long
                when (status.toInt()) {
                    Room.STATUS_CREATOR -> {
                        tvInfo.setText("等對手加入")
                        true
                    }

                    Room.STATUS_JOINIES -> {
                        tvInfo.setText("對手已經加入")
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId).child("status")
                            .setValue(Room.STATUS_CREATED_TURN)
                        true
                    }

                    Room.STATUS_CREATED_TURN -> {
                        myTurn = isCreator
                        true
                    }

                    Room.STATUS_JOINTED_TURN -> {
                        myTurn = !isCreator
                        true
                    }

                    Room.STATUS_CREATED_BINGO -> {
                        if (!isCreator) {
                            AlertDialog.Builder(this@BingoActivity)
                                .setTitle(" End Game ")
                                .setMessage(" Yout Loss - CREATED_BINGO ")
                                .setIcon(R.drawable.crying)
                                .setPositiveButton("OK") { ok, which ->
                                    endGame()
                                }
                                .show()
                        }
                        true
                    }

                    Room.STATUS_JOINTED_BINGO -> {
                        if (isCreator) {
                            AlertDialog.Builder(this@BingoActivity)
                                .setTitle(" End Game ")
                                .setMessage(" Yout Loss - JOINTED_BINGO ")
                                .setIcon(R.drawable.crying)
                                .setPositiveButton("OK") { ok, which ->
                                    endGame()
                                }
                                .show()
                        }
                        true
                    }
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }

    private fun endGame() {
        FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomId).child("status").removeEventListener(statusListener)
        if (isCreator) {
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId).removeValue()
        }
        finish()
    }


    override fun onStop() {
        super.onStop()
        adapter.stopListening()
        adapter.notifyDataSetChanged()
    }


}













