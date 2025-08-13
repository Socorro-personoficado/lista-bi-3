package com.example.firebasemlktdemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class DetectorEmocoesActivity : AppCompatActivity() {

    private lateinit var imageViewEmocao: ImageView
    private lateinit var tvResultadoEmocao: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detector_emocoes)

        imageViewEmocao = findViewById(R.id.imageViewEmocao)
        tvResultadoEmocao = findViewById(R.id.tvResultadoEmocao)

        val btnSelecionarImagem = findViewById<Button>(R.id.btnSelecionarImagemEmocao)

        btnSelecionarImagem.setOnClickListener {
            ImagePicker.with(this)
                    .crop()
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data!!
            detectarEmocoes(uri)
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Seleção de imagem cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detectarEmocoes(imageUri: Uri) {
        val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()

        val detector = FaceDetection.getClient(options)
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, imageUri)

            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val rectPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            val textPaint = Paint().apply {
                color = Color.YELLOW
                textSize = 50f
                strokeWidth = 3f
                style = Paint.Style.FILL
            }

            detector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            tvResultadoEmocao.text = "Faces detectadas: ${faces.size}\n"
                            for (face in faces) {
                                val bounds = face.boundingBox
                                canvas.drawRect(bounds, rectPaint)

                                tvResultadoEmocao.append("Face (x:${bounds.left}, y:${bounds.top})\n")

                                val smileProb = face.smilingProbability ?: 0f
                                val leftEyeOpenProb = face.leftEyeOpenProbability ?: 0f
                                val rightEyeOpenProb = face.rightEyeOpenProbability ?: 0f

                                val emotion = when {
                                    smileProb > 0.8f -> "Feliz"
                                    leftEyeOpenProb < 0.2f && rightEyeOpenProb < 0.2f -> "Sonolento"
                                    else -> "Neutro"
                                }

                                tvResultadoEmocao.append("Emoção: $emotion\n")
                                tvResultadoEmocao.append("Sorriso: ${"%.1f".format(smileProb * 100)}%\n")
                                tvResultadoEmocao.append("Olho Esquerdo Aberto: ${"%.1f".format(leftEyeOpenProb * 100)}%\n")
                                tvResultadoEmocao.append("Olho Direito Aberto: ${"%.1f".format(rightEyeOpenProb * 100)}%\n\n")

                                canvas.drawText(emotion, bounds.left.toFloat(), bounds.top.toFloat() - 10, textPaint)
                            }
                            imageViewEmocao.setImageBitmap(mutableBitmap)
                        } else {
                            tvResultadoEmocao.text = "Nenhuma face detectada."
                            imageViewEmocao.setImageURI(imageUri)
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Falha ao detectar faces: ${e.message}", Toast.LENGTH_LONG).show()
                        tvResultadoEmocao.text = "Erro: ${e.message}"
                        e.printStackTrace()
                        imageViewEmocao.setImageURI(imageUri)
                    }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Falha ao carregar imagem: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
