package com.example.numscan

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<MaterialButton>(R.id.btnStartScan)
        val iconCard = findViewById<MaterialCardView>(R.id.ivAppIcon)

        // Entrance animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleIn = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        iconCard.startAnimation(scaleIn)
        btnStart.startAnimation(slideUp)

        btnStart.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    startActivity(Intent(this, ScanActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
                }.start()
        }
    }
}
