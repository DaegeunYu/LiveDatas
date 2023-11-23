package com.awesomepia.metalive

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import com.awesomepia.metalive.Util.Log
import com.awesomepia.metalive.Util.Utilities


class BioAuthActivity : AppCompatActivity() {
    private lateinit var button: TextView
    private lateinit var toast: Toast
    private var index = 0

    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                checkAvailableAuth()  //생체 인증 가능 여부확인 다시 호출
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utilities.instance.thread(this, Utilities.instance.buildTrace(0, 3))
        setContentView(R.layout.activity_bio)

        toast = Toast.makeText(applicationContext, "", Toast.LENGTH_SHORT)

        button = findViewById(R.id.button)
        button.setOnClickListener {
            checkAvailableAuth()
        }

    }

    private fun checkAvailableAuth() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                //  생체 인증 가능
                Auth()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                //  기기에서 생체 인증을 지원하지 않는 경우
                Toast.makeText(applicationContext, "생체 없음", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.instance.e("F2")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                //  생체 인식 정보가 등록되지 않은 경우
                goBiometricSettings()
            }
            else -> {
                //   기타 실패
                Log.instance.e("F1")
            }
        }
    }

    private fun goBiometricSettings() {
        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        }
        loginLauncher.launch(enrollIntent)
    }


    private fun Auth() {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                toast.cancel()
                toast = Toast.makeText(applicationContext, "성공", Toast.LENGTH_SHORT)
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    toast.show()
                }, 10)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                toast.cancel()
                toast = Toast.makeText(applicationContext, "실패 ".plus(index), Toast.LENGTH_SHORT)
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    toast.show()
                }, 10)
                index++
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                toast.cancel()
                toast = Toast.makeText(applicationContext, "오류", Toast.LENGTH_SHORT)
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    toast.show()
                }, 10)
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val promptInfo = PromptInfo.Builder()
            .setTitle("지문 인증")
            .setSubtitle("기기에 등록된 지문을 이용하여 지문을 인증해주세요.")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
