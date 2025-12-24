package com.hom.bingo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.util.query
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hom.bingo.databinding.ActivityMainBinding
import com.hom.bingo.databinding.RowRoomItemBinding
import java.util.Arrays

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    private lateinit var adaptor: FirebaseRecyclerAdapter<Room, RoomHolder>
    private lateinit var groupAvatar: Group
    private lateinit var ivAvatar: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var recyclerRoom: RecyclerView
    private var member: Member? = null
    private val requestSignIn = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
        }
    }
    private var user: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    val avatarIds = intArrayOf(
        R.drawable.avatar_0,
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        findViews()

        binding.fab.setOnClickListener { view ->
            showAddRoom()
        }
    }

    private fun showAddRoom() {
        val roomText = EditText(this)
        roomText.setText("Room")
        AlertDialog.Builder(this)
            .setTitle(" Room Title ")
            .setMessage(" Enter Room Title ")
            .setView(roomText)
            .setPositiveButton("OK") { ok, whcih ->
                val room = Room(roomText.text.toString(), member)
                FirebaseDatabase.getInstance().getReference("rooms")
                    .push().setValue(room, object : DatabaseReference.CompletionListener {
                        override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
                            val roomId = ref.key.toString()
                            FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId).child("id").setValue(roomId)
                            Intent(this@MainActivity, BingoActivity::class.java)
                                .apply {
                                    putExtra("ROOM_ID", roomId)
                                    putExtra("IS_CREATOR", true)
                                }.also {
                                    startActivity(it)
                                }
                        }
                    })
            }
            .show()
    }

    private fun findViews() {
        tvNickname = binding.contentMain.tvNickname
        ivAvatar = binding.contentMain.ivAvatar
        groupAvatar = binding.contentMain.groupAvatar
        recyclerRoom = binding.contentMain.recyclerRoom
        groupAvatar.visibility = View.GONE

        binding.contentMain.avatar0.setOnClickListener(this)
        binding.contentMain.avatar1.setOnClickListener(this)
        binding.contentMain.avatar2.setOnClickListener(this)
        binding.contentMain.avatar3.setOnClickListener(this)
        binding.contentMain.avatar4.setOnClickListener(this)
        binding.contentMain.avatar5.setOnClickListener(this)
        binding.contentMain.avatar6.setOnClickListener(this)

        recyclerRoom.setHasFixedSize(true)
        recyclerRoom.layoutManager = GridLayoutManager(this, 1)
        val query = FirebaseDatabase.getInstance().getReference("rooms")
            .limitToLast(30)
        val option = FirebaseRecyclerOptions.Builder<Room>()
            .setQuery(query, Room::class.java).build()
        adaptor = object : FirebaseRecyclerAdapter<Room, RoomHolder>(option) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHolder {
                val v = RowRoomItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return RoomHolder(v)
            }

            override fun onBindViewHolder(holder: RoomHolder, position: Int, model: Room) {
                holder.roomTitle.setText(model.title)
                holder.roomAvatar.setImageResource(avatarIds.get(model.init?.avatarId!!))
                holder.itemView.setOnClickListener {
                    Intent(this@MainActivity, BingoActivity::class.java)
                        .apply {
                            putExtra("ROOM_ID", model.id)
                            putExtra("IS_CREATOR", false)
                        }.also {
                            startActivity(it)
                        }
                }

            }
        }
        recyclerRoom.adapter = adaptor

    }

    class RoomHolder(view: RowRoomItemBinding) : RecyclerView.ViewHolder(view.root) {
        val roomTitle = view.rowRoomTitle
        val roomAvatar = view.rowRoomAvatar
    }

    fun setAvatar(view: View) {
        groupAvatar.visibility =
            if (groupAvatar.visibility == View.GONE) View.VISIBLE else View.GONE

    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(this)
        adaptor.startListening()
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(this)
        adaptor.stopListening()
        adaptor.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_signout -> {
                auth.signOut()
                true
            }

            R.id.action_exit -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        user = p0.currentUser
        user?.also {
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).child("uid").setValue(it.uid)
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).child("displayName").setValue(it.displayName)
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).addValueEventListener(statusListener)
        } ?: signUp()
    }

    val statusListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            member = snapshot.getValue(Member::class.java)
            user?.also {
                member?.nickname?.also { nick ->
                    tvNickname.setText(nick)
                } ?: showNickname(it.uid, it.displayName)
                ivAvatar.setImageResource(avatarIds.get(member?.avatarId!!))
            }

        }

        override fun onCancelled(error: DatabaseError) {
        }
    }

    private fun showNickname(uid: String, name: String?) {
        val nickText = EditText(this)
        nickText.setText(name)
        AlertDialog.Builder(this)
            .setTitle(" Nick Name")
            .setMessage(" Enter Nick Name ")
            .setView(nickText)
            .setPositiveButton("OK") { ok, which ->
                FirebaseDatabase.getInstance().getReference("users")
                    .child(uid).child("nickname")
                    .setValue(nickText.text.toString())
            }
            .setNegativeButton("Cancel") { cancel, which -> null }
            .show()
    }


    private fun signUp() {
        val signIn = AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(
                Arrays.asList(
                    EmailBuilder().build(),
                    GoogleBuilder().build(),
                )
            ).setIsSmartLockEnabled(false).build()
        requestSignIn.launch(signIn)
    }

    override fun onClick(v: View?) {
        val clkId = v?.id
        val selectId = when (clkId) {
            R.id.avatar_0 -> 0
            R.id.avatar_1 -> 1
            R.id.avatar_2 -> 2
            R.id.avatar_3 -> 3
            R.id.avatar_4 -> 4
            R.id.avatar_5 -> 5
            R.id.avatar_6 -> 6
            else -> 0
        }
        groupAvatar.visibility = View.GONE
        user?.also {
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).child("avatarId").setValue(selectId)
        }

    }

}












