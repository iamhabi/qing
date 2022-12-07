package com.habidev.qing

import java.util.ArrayList

interface CommandFunction {
    fun playTriggerSound()
    fun command(resultList: ArrayList<String>?)
    fun startRecording()
    fun stopRecording()
}