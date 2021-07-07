package edu.sahba.firebasefriendlychat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.EmailAuthProvider
import com.squareup.picasso.Picasso
import edu.sahba.firebasefriendlychat.App.Companion.currentUser
import edu.sahba.firebasefriendlychat.App.Companion.defaultPic
import edu.sahba.firebasefriendlychat.data.User
import edu.sahba.firebasefriendlychat.databinding.FragmentEditProfileBinding
import timber.log.Timber
import java.util.*

class EditProfileFragment : MyFragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    var selectedPhotoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Picasso.get().load(currentUser?.profileImgUrl).into(binding.profileImgUpdate)

        val constraintLayout = binding.constraintLayout
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        if (!auth.currentUser!!.isAnonymous) {
            binding.usernameInputLayout.visibility = View.GONE
            binding.passwordInputLayout.visibility = View.GONE
            constraintSet.connect(
                R.id.updateUserInfo,
                ConstraintSet.TOP,
                R.id.lNameInputLayout,
                ConstraintSet.BOTTOM,
                16
            )
        }

        binding.profileImgUpdate.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        binding.updateUserInfo.setOnClickListener {
            binding.updateUserInfo.showLoading()

            if (checkUserInput())
                if (auth.currentUser!!.isAnonymous)
                    linkAnonymousUser()
                else updateUserInfo()

        }

        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_editProfileFragment_to_chatListFragment)
        }

    }

    private fun updateUserInfo() {
        uploadExtraInfo()
    }

    private fun uploadExtraInfo() {
        if (selectedPhotoUri == null) {
            saveUserData(defaultPic)
            return
        } else {
            val filename = UUID.randomUUID().toString()
            val ref = storage.getReference("/images/$filename")
            ref.putFile(selectedPhotoUri!!)
                .addOnCompleteListener {
                    ref.downloadUrl.addOnCompleteListener {
                        Log.e("TAG", "uploadExtraInfo: ${it.result.toString()}")
                        saveUserData(it.result.toString())
                    }
                }
                .addOnFailureListener {
                    Timber.e(it.message)
                }
        }
    }

    private fun saveUserData(uri: String) {
        val uid = auth.uid ?: ""
        val ref = database.getReference("/users/$uid")
        val user = User(
            uid,
            "${binding.fNameEt.text.toString()} ${binding.lNameEt.text.toString()}",
            uri
        )
        ref.setValue(user)
            .addOnSuccessListener {
                currentUser = user
                findNavController().navigate(R.id.action_editProfileFragment_to_chatListFragment)
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message.toString(), Toast.LENGTH_SHORT).show()
            }
        binding.updateUserInfo.hideLoading()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(
                requireContext().contentResolver,
                selectedPhotoUri
            )
            binding.profileImgUpdate.setImageBitmap(bitmap)
        }
    }

    private fun linkAnonymousUser() {
        val credential = EmailAuthProvider.getCredential(
            binding.emailEt.text.toString(),
            binding.passwordEt.text.toString()
        )
        auth.currentUser!!.linkWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    uploadExtraInfo()
                }
            }
    }

    private fun checkUserInput(): Boolean {
        if (binding.fNameEt.text == null) {
            binding.fNameEt.error = "Please enter your name!"
            return false
        } else binding.fNameEt.error = null
        if (binding.lNameEt.text == null) {
            binding.lNameEt.error = "Please enter your lastname!"
            return false
        } else binding.lNameEt.error = null
        if (binding.emailEt.text == null && auth.currentUser!!.isAnonymous) {
            binding.emailEt.error = "Please enter your email"
            return false
        } else binding.emailEt.error = null
        if (binding.passwordEt.length() < 6 && auth.currentUser!!.isAnonymous) {
            binding.passwordEt.error = "Password must be 6 charachters or more"
            return false
        } else binding.passwordEt.error = null

        return true
    }

}