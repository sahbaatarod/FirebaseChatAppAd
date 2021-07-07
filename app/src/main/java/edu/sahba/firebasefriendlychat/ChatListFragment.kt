package edu.sahba.firebasefriendlychat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import edu.sahba.firebasefriendlychat.data.ChatMessage
import edu.sahba.firebasefriendlychat.data.User
import edu.sahba.firebasefriendlychat.databinding.FragmentChatListBinding
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

class ChatListFragment : MyFragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val testDeviceIds = listOf("B36E4E9257E718A3C874D608CFD21EA4")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)

        val adRequest = AdRequest.Builder().build()
        binding.ad.loadAd(adRequest)

        listenForLatestMessages()
        initView()

    }

    private fun initView() {
        binding.chatListRv.adapter = adapter
        binding.chatListRv.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                RecyclerView.VERTICAL
            )
        )
        binding.logoutBtn.setOnClickListener {
            val alertDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Are you sure you want to sign out?")
                .setPositiveButton(
                    "Yes"
                ) { _, _ ->
                    auth.signOut()
                    val intent = Intent(requireContext(), AuthenticationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity?.startActivity(intent)
                }
                .setNegativeButton("No!") { dialog, _ ->
                    dialog.dismiss()
                }
            alertDialog.show()
        }

        binding.editProfile.setOnClickListener {
            findNavController().navigate(R.id.action_chatListFragment_to_editProfileFragment)
        }

        binding.newMsgBtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatListFragment_to_newMessageFragment)
        }

        adapter.setOnItemClickListener { item, _ ->
            val row = item as LatestMessageAdapter
            Timber.e("${row.chatPartnerUser?.uid} -- ${row.chatPartnerUser}")
            val bundle = bundleOf(USER_KEY to row.chatPartnerUser)
            findNavController().navigate(R.id.action_chatListFragment_to_chatLogFragment, bundle)
        }
    }

    val latestMessagesMap = HashMap<String, ChatMessage>()

    private fun listenForLatestMessages() {
        val ref = database.getReference("/latestMessages/${App.receiverId}")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.exists()) {
                    binding.emptyDataTv.visibility = View.GONE
                    binding.emptyIv.visibility = View.GONE
                    val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                    latestMessagesMap[snapshot.key!!] = chatMessage
                    refreshRecyclerMessages()
                } else {
                    binding.emptyDataTv.visibility = View.VISIBLE
                    binding.emptyIv.visibility = View.VISIBLE
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.exists()) {
                    binding.emptyDataTv.visibility = View.GONE
                    binding.emptyIv.visibility = View.GONE
                    val chatMessage = snapshot.getValue(ChatMessage::class.java) ?: return
                    latestMessagesMap[snapshot.key!!] = chatMessage
                    refreshRecyclerMessages()
                } else {
                    binding.emptyDataTv.visibility = View.VISIBLE
                    binding.emptyIv.visibility = View.VISIBLE
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Timber.e("onChildRemoved")
                adapter.notifyDataSetChanged()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                Timber.e("onChildMoved")
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("onCancelled")
            }

        })
    }

    private fun refreshRecyclerMessages() {
        adapter.clear()
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessageAdapter(it))
        }
    }

    override fun onPause() {
        super.onPause()
        binding.ad.pause()
    }

    override fun onResume() {
        super.onResume()
        binding.ad.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.ad.destroy()
    }

}