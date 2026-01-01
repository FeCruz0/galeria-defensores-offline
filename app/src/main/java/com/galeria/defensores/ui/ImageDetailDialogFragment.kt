package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import android.graphics.BitmapFactory
import android.util.Base64
import android.provider.MediaStore
import android.widget.Toast
import android.content.ContentValues
import java.io.OutputStream

class ImageDetailDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(imageUrl: String): ImageDetailDialogFragment {
            val fragment = ImageDetailDialogFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // We reuse message_item_image temporarily or created a simple layout programmatically if needed, 
        // but for a proper dialog, let's inflate a simple layout. 
        // Since I cannot create a NEW layout file easily without another tool call, 
        // I will construct the view programmatically to save steps or assume a layout exists. 
        // Better: I will create the layout file first in next steps if needed, 
        // but to speed up, I'll use a standard layout or create one.
        // Let's create a layout file for this dialog.
        return inflater.inflate(R.layout.dialog_image_detail, container, false)
    }
    
    // NOTE: I will generate the XML layout in the next tool call.

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageView = view.findViewById<ImageView>(R.id.detail_image)
        val btnClose = view.findViewById<Button>(R.id.btn_close_image)
        val btnSave = view.findViewById<Button>(R.id.btn_save_image)

        val imageUrl = arguments?.getString(ARG_IMAGE_URL)

        if (imageUrl != null) {
            try {
                val cleanBase64 = if (imageUrl.contains(",")) {
                    imageUrl.split(",")[1]
                } else {
                    imageUrl
                }
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                imageView.setImageBitmap(bitmap)

                btnSave.setOnClickListener {
                    saveImageToGallery(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
            }
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun saveImageToGallery(bitmap: android.graphics.Bitmap) {
        val resolver = context?.contentResolver ?: return
        val imageCollection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "galeria_rpg_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        try {
            val uri = resolver.insert(imageCollection, contentValues)
            if (uri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                    Toast.makeText(context, "Imagem salva na Galeria!", Toast.LENGTH_SHORT).show()
                }
            } else {
                 Toast.makeText(context, "Erro ao criar arquivo na galeria", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
