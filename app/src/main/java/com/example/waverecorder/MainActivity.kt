package com.example.waverecorder

import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.waverecorder.databinding.ActivityMainBinding
import com.example.waverecorder.lib.recorder.RecorderState
import com.example.waverecorder.lib.recorder.WaveRecorder
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var permissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var permissionsGranted = false

    private lateinit var waveRecorder: WaveRecorder

    private lateinit var filePath: String

    private var isPaused = false

    val mutableList: MutableList<Int> = mutableListOf()

    private lateinit var handler: Handler

    private lateinit var runnable: Runnable

    private var type = ""

    private lateinit var recorder: MediaRecorder

    private var dirPath = ""
    private var filename = ""

    private var isRecordingmp3 = false
    private var isPausedmp3 = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionsGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, permissions[1]) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, permissions[2]) == PackageManager.PERMISSION_GRANTED

        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }

// Assuming you have a reference to a Looper for the main thread
        val mainLooper = Looper.getMainLooper()

        // Create a Handler with the main Looper
        handler = Handler(mainLooper)

        filePath = externalCacheDir?.absolutePath + "/audioFile.wav"
        waveRecorder = WaveRecorder(filePath)
        waveRecorder.onStateChangeListener = {
            when(it) {
                RecorderState.RECORDING -> startRecording()
                RecorderState.PAUSE -> pauseRecording()
                RecorderState.STOP -> stopRecording()

            }
        }

        binding.btnMp3.setOnClickListener {
            binding.btnWav.visibility = View.GONE
            binding.btnStart.visibility = View.VISIBLE
            binding.btnMp3.setBackgroundResource(R.drawable.button_bg_darkblue)
            binding.tvFormat.visibility = View.GONE
            type = "mp3"
        }

        binding.btnWav.setOnClickListener {
            binding.btnMp3.visibility = View.GONE
            binding.btnStart.visibility = View.VISIBLE
            binding.btnWav.setBackgroundResource(R.drawable.button_bg_darkblue)
            binding.tvFormat.visibility = View.GONE
            type = "wav"
        }

        binding.btnStart.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED){

                if(type == "wav"){
                    waveRecorder.startRecording()
                }
                else if(type == "mp3"){
                    startRecordingInMp3()
                }

            }
            else{
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT ).show()
            }

            runnable = Runnable {

//                Toast.makeText(this, "runnable", Toast.LENGTH_SHORT).show()

                if(type == "wav"){


                    val amp = waveRecorder.getAmplitude()

                    Log.d("AMPs", amp.toString())
                    var norm = amp/128

                    if(norm>60) {
                        mutableList.add(5)
                        binding.waveformView.addAmplitude(5f)
                    }

                    else {
                        mutableList.add(norm)
                        binding.waveformView.addAmplitude(amp.toFloat())

                    }


//                Log.d("AMPs sjbfjhsahfjkasf", norm.toString())

                    val intArray: IntArray = mutableList.toIntArray()
//                binding.mWave.sample = intArray
                }
                else if(type == "mp3"){
                    binding.waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
                }
//            Toast.makeText(this, amp.toString(), Toast.LENGTH_SHORT).show()

//

                handler.postDelayed(runnable, 100)
            }

            handler.post(runnable)
        }

        binding.btnPause.setOnClickListener {
            if(!isPaused)
                if(type == "wav"){
                    waveRecorder.pauseRecording()
                }
                else if(type == "mp3"){
                    pauseRecordingInMp3()
                }
            else
                if(type == "wav"){
                    waveRecorder.resumeRecording()
                }
                else if(type == "mp3"){
                    resumeRecordingInMp3()
                }
        }

        binding.btnStop.setOnClickListener {

            if(type == "wav"){
                waveRecorder.stopRecording()
            }
            else if(type == "mp3"){
                stopRecordingInMp3()
            }
            handler.removeCallbacks(runnable)

//            binding.mWave.visibility = View.GONE

        }
    }

    private fun resumeRecordingInMp3() {
        recorder.resume()
        isPausedmp3 = false
        isPaused = false
        binding.btnStart.visibility = View.GONE
    }

    private fun pauseRecordingInMp3() {
        recorder.pause()
        isPausedmp3 = true
        binding.btnPause.setImageResource(R.drawable.ic_play)
        isPaused = true
    }

    private fun startRecordingInMp3() {

        binding.waveformView.visibility = View.VISIBLE
        // Implement start recording
        recorder = MediaRecorder()

        binding.btnPause.visibility  = View.VISIBLE
        binding.btnStop.visibility  = View.VISIBLE
        binding.btnPause.setImageResource(R.drawable.ic_pause)
        binding.btnStart.visibility = View.GONE

        dirPath = "${externalCacheDir?.absolutePath}/"
        var simpleDateFormat = SimpleDateFormat("dd-MM-yyyy_hh-mm-ss")
        var date = simpleDateFormat.format(Date())
        filename = "audio_record_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")

            try {
                prepare()
            } catch (e: IOException) {
            }

            start()
        }

        isRecordingmp3 = true
        isPausedmp3 = false
    }

    private fun stopRecordingInMp3(){
//        timer.stop()
        binding.waveformView.clear()


        recorder.apply {
            stop()
            release()
        }

        isPausedmp3 = false
        isRecordingmp3 = false

        binding.btnMp3.visibility = View.VISIBLE
        binding.btnWav.visibility = View.VISIBLE

        binding.btnStart.visibility = View.GONE
        binding.btnStop.visibility = View.GONE
        binding.btnPause.visibility = View.GONE
        binding.waveformView.visibility = View.GONE

        binding.tvFormat.visibility = View.VISIBLE


        binding.btnMp3.setBackgroundResource(R.drawable.button_bg_lightblue)
        binding.btnWav.setBackgroundResource(R.drawable.button_bg_lightblue)




    }





    private fun startRecording() {
//        binding.mWave.visibility = View.VISIBLE
        binding.waveformView.visibility = View.VISIBLE
        binding.btnPause.visibility  = View.VISIBLE
        binding.btnStop.visibility  = View.VISIBLE
        binding.btnPause.setImageResource(R.drawable.ic_pause)
        binding.btnStart.visibility = View.GONE

        if(isPaused){
            isPaused = false
            binding.btnStart.visibility = View.GONE

        }

    }

    private fun pauseRecording() {
        if(!isPaused)
        {
            binding.btnPause.setImageResource(R.drawable.ic_play)
            isPaused = true
        }

    }

    private fun stopRecording() {

        binding.waveformView.clear()

        binding.btnPause.visibility  = View.GONE
        binding.btnStop.visibility  = View.GONE
        binding.btnStart.visibility = View.GONE
        binding.waveformView.visibility = View.GONE
        binding.btnWav.visibility = View.VISIBLE
        binding.btnMp3.visibility = View.VISIBLE



        binding.tvFormat.visibility = View.VISIBLE

        binding.btnMp3.setBackgroundResource(R.drawable.button_bg_lightblue)
        binding.btnWav.setBackgroundResource(R.drawable.button_bg_lightblue)


        Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 1) {
            permissionsGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED
        }
    }




}