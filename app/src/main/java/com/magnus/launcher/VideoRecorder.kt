package com.magnus.launcher

import android.content.Context
import android.media.MediaRecorder
import android.hardware.Camera
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var camera: Camera? = null
    private var isRecording = false

    fun startRecording(): Boolean {
        return try {
            // Ouvre caméra frontale
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
            
            mediaRecorder = MediaRecorder().apply {
                setCamera(camera)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(1280, 720)
                setVideoFrameRate(30)
                setAudioSamplingRate(44100)
                
                val videoFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "MAGNUS_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.mp4"
                )
                setOutputFile(videoFile.absolutePath)
                
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecording(): Boolean {
        return try {
            if (isRecording) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                
                camera?.release()
                camera = null
                
                isRecording = false
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isRecording(): Boolean = isRecording
}
