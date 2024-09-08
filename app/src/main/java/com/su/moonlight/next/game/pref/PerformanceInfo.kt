package com.su.moonlight.next.game.pref

import android.net.TrafficStats
import android.os.Process
import com.limelight.binding.video.VideoStats
import com.limelight.binding.video.VideoStatsFps
import com.limelight.utils.TrafficStatsHelper
import com.su.moonlight.next.App
import com.su.moonlight.next.R

class PerformanceInfo private constructor() {

    companion object {

        private var currentUseIndex = 0
        private val insPool = mutableListOf<PerformanceInfo>().apply {
            repeat(5) { add(PerformanceInfo()) }
        }

        @JvmStatic
        fun create(
            videoStats: VideoStats,
            initialWidth: Int,
            initialHeight: Int,
            netDataDelta: Long,
            decoder: String,
            rttInfo: Long
        ): PerformanceInfo {
            if (currentUseIndex >= insPool.size) {
                currentUseIndex = 0
            }
            val info = insPool[currentUseIndex++]
            info.videoStats = videoStats
            info.initialWidth = initialWidth
            info.initialHeight = initialHeight
            info.netDataDelta = netDataDelta
            info.decoder = decoder
            info.rttInfo = rttInfo
            info.videoStatsFps = videoStats.fps
            return info
        }

        val EMPTY = PerformanceInfo()
    }

    var videoStats: VideoStats = VideoStats()
        private set
    var initialWidth: Int = 0
        private set
    var initialHeight: Int = 0
        private set
    var netDataDelta: Long = 0
        private set
    var decoder: String = ""
        private set
    var rttInfo: Long = 0
        private set

    var videoStatsFps: VideoStatsFps = VideoStatsFps()
        private set

    val decodeTimeMs: Float
        get() =
            videoStats.decoderTimeMs.toFloat() / videoStats.totalFramesReceived

    fun toFillDesc(): String {
        val sb = StringBuilder()
        sb.append(
            App.ins.getString(
                R.string.perf_overlay_streamdetails,
                initialWidth.toString() + "x" + initialHeight,
                videoStatsFps.totalFps
            )
        ).append('\n')
        sb.append(App.ins.getString(R.string.perf_overlay_decoder, decoder)).append('\n')
        sb.append(App.ins.getString(R.string.perf_overlay_incomingfps, videoStatsFps.receivedFps))
            .append('\n')
        sb.append(App.ins.getString(R.string.perf_overlay_renderingfps, videoStatsFps.renderedFps))
            .append('\n')
        sb.append(
            App.ins.getString(
                R.string.perf_overlay_netdrops,
                videoStats.framesLost.toFloat() / videoStats.totalFrames * 100
            )
        ).append('\n')
        sb.append(
            App.ins.getString(
                R.string.perf_overlay_netlatency,
                (rttInfo shr 32).toInt(), rttInfo.toInt()
            )
        ).append('\n')
        if (videoStats.framesWithHostProcessingLatency > 0) {
            sb.append(
                App.ins.getString(
                    R.string.perf_overlay_hostprocessinglatency,
                    videoStats.minHostProcessingLatency.code.toFloat() / 10,
                    videoStats.maxHostProcessingLatency.code.toFloat() / 10,
                    videoStats.totalHostProcessingLatency.toFloat() / 10 / videoStats.framesWithHostProcessingLatency
                )
            ).append('\n')
        }
        sb.append(App.ins.getString(R.string.perf_overlay_dectime, decodeTimeMs))
        return sb.toString()
    }

    fun toLiteDesc(): String {
        val sb = StringBuilder()

        if (TrafficStatsHelper.getPackageRxBytes(Process.myUid()) != TrafficStats.UNSUPPORTED.toLong()) {
            sb.append(App.ins.getString(R.string.perf_overlay_lite_bandwidth) + ": ")
            val realtimeNetData: Float = netDataDelta / 1024f
            if (realtimeNetData >= 1000) {
                sb.append(String.format("%.2f", realtimeNetData / 1024f) + "M/s\t ")
            } else {
                sb.append(String.format("%.2f", realtimeNetData) + "K/s\t ")
            }
        }
        //sb.append("分辨率：");
        //sb.append(initialWidth + "x" + initialHeight);
        sb.append(App.ins.getString(R.string.perf_overlay_lite_network_decoding_delay) + ": ")
        sb.append(
            App.ins.getString(
                R.string.perf_overlay_lite_net,
                (rttInfo shr 32).toInt(),
                rttInfo.toInt()
            )
        )
        sb.append(" / ")
        sb.append(App.ins.getString(R.string.perf_overlay_lite_dectime, decodeTimeMs))
        sb.append(" ")
        sb.append(App.ins.getString(R.string.perf_overlay_lite_packet_loss) + ": ")
        sb.append(
            App.ins.getString(
                R.string.perf_overlay_lite_netdrops,
                videoStats.framesLost.toFloat() / videoStats.totalFrames * 100
            )
        )
        sb.append("\t FPS：")
        sb.append(App.ins.getString(R.string.perf_overlay_lite_fps, videoStatsFps.totalFps))

        return sb.toString()
    }
}