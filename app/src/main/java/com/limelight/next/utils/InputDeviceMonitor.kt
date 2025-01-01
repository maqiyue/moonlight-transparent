package com.limelight.next.utils

import android.content.Context
import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import android.view.InputDevice.SOURCE_GAMEPAD
import android.view.InputDevice.SOURCE_KEYBOARD
import android.view.InputDevice.SOURCE_MOUSE
import android.view.InputDevice.SOURCE_TOUCHPAD
import com.limelight.Game

class InputDeviceMonitor(private val game: Game) : InputManager.InputDeviceListener {
    private val TAG = "InputDeviceMonitor"
    private val inputManager = game.getSystemService(Context.INPUT_SERVICE) as InputManager
    private var connectedDevices = mutableSetOf<Int>()

    init {
        inputManager.registerInputDeviceListener(this, null)
        updateConnectedDevices()
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId) ?: return
        if (isRelevantInputDevice(device)) {
            connectedDevices.add(deviceId)
            logDeviceInfo(device, "设备已接入")
            logConnectionStatus()
            updateFloatingKeyboard()
        }
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        if (connectedDevices.remove(deviceId)) {
            Log.i(TAG, "设备已断开 (ID: $deviceId)")
            logConnectionStatus()

            updateFloatingKeyboard()
        }
    }

    private fun updateFloatingKeyboard() {
        game.runOnUiThread {
            if (!hasConnectedDevices() && game.isFloatingKeyboardShowing()) {
                game.showFloatingKeyboard()
            } else {
                game.hideFloatingKeyboard()
            }
        }
    }

    private fun updateConnectedDevices() {
        val devices = InputDevice.getDeviceIds()
        for (deviceId in devices) {
            val device = InputDevice.getDevice(deviceId) ?: continue
            if (isRelevantInputDevice(device)) {
                connectedDevices.add(deviceId)
                logDeviceInfo(device, "初始化设备")
            }
        }
        logConnectionStatus()
    }

    private fun isRelevantInputDevice(device: InputDevice): Boolean {
        // 排除虚拟设备
        if (device.isVirtual) {
            return false
        }

        // 检查是否为物理设备 (真实的物理设备通常有有效的vendorId和productId)
        val isPhysicalDevice = device.vendorId > 1000 && device.productId > 1000
        if (!isPhysicalDevice) {
            return false
        }

        // 检查设备类型
        val sources = device.sources
        return (sources and SOURCE_KEYBOARD) == SOURCE_KEYBOARD ||
               (sources and SOURCE_MOUSE) == SOURCE_MOUSE ||
               (sources and SOURCE_GAMEPAD) == SOURCE_GAMEPAD ||
               (sources and SOURCE_TOUCHPAD) == SOURCE_TOUCHPAD
    }
    

    private fun logDeviceInfo(device: InputDevice, action: String) {
        val deviceType = when {
            (device.sources and SOURCE_KEYBOARD) == SOURCE_KEYBOARD -> "键盘"
            (device.sources and SOURCE_MOUSE) == SOURCE_MOUSE -> "鼠标"
            (device.sources and SOURCE_GAMEPAD) == SOURCE_GAMEPAD -> "手柄"
            (device.sources and SOURCE_TOUCHPAD) == SOURCE_TOUCHPAD -> "触摸板"
            else -> "未知设备"
        }
        Log.i(TAG, """
            $action: 
            类型: $deviceType
            名称: ${device.name}
            描述符: ${device.descriptor}
            来源: ${device.sources}
            产品ID: ${device.productId}
            供应商ID: ${device.vendorId}
            是否虚拟: ${device.isVirtual}
        """.trimIndent())
    }

    private fun logConnectionStatus() {
        if (connectedDevices.isEmpty()) {
            Log.i(TAG, "当前没有外接输入设备")
        } else {
            Log.i(TAG, "当前已连接 ${connectedDevices.size} 个输入设备")
        }
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId) ?: return
        if (isRelevantInputDevice(device)) {
            logDeviceInfo(device, "设备状态已改变")
        }
    }

    fun release() {
        inputManager.unregisterInputDeviceListener(this)
    }

    // 获取当前是否有任何外接输入设备
    fun hasConnectedDevices(): Boolean = connectedDevices.isNotEmpty()

    // 获取当前连接的设备数量
    fun getConnectedDeviceCount(): Int = connectedDevices.size
} 