/*
 * Copyright (C) 2013 gujicheng
 * 
 * Licensed under the GPL License Version 2.0;
 * you may not use this file except in compliance with the License.
 * 
 * If you have any question, please contact me.
 * 
 *************************************************************************
 **                   Author information                                **
 *************************************************************************
 ** Email: gujicheng197@126.com                                         **
 ** QQ   : 29600731                                                     **
 ** Weibo: http://weibo.com/gujicheng197                                **
 *************************************************************************
 */
package com.libra.sinvoice;

import android.media.AudioManager;
import android.media.AudioTrack;

import com.libra.sinvoice.Buffer.BufferData;

/**
 * 
 * @ClassName: com.libra.sinvoice.PcmPlayer
 * @Description: PCM播放器
 * @author zhaokaiqiang
 * @date 2014-11-15 下午1:10:18
 * 
 */
public class PcmPlayer {

	private final static String TAG = "PcmPlayer";
	private final static int STATE_START = 1;
	private final static int STATE_STOP = 2;
	// 播放状态，用于控制播放或者是停止
	private int mState;
	private AudioTrack audioTrack;
	// 已经播放过的字节长度
	private long playedLen;
	private PcmListener pcmListener;
	private PcmCallback playerCallback;

	public static interface PcmListener {

		void onPcmPlayStart();

		void onPcmPlayStop();
	}

	public static interface PcmCallback {

		BufferData getPcmPlayBuffer();

		void freePcmPlayData(BufferData data);
	}

	public PcmPlayer(PcmCallback callback, int sampleRate, int channel,
			int format, int bufferSize) {
		playerCallback = callback;
		// 初始化AudioTrack对象(音频流类型，采样率，通道，格式，缓冲区大小，模式)
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				channel, format, bufferSize, AudioTrack.MODE_STREAM);
		mState = STATE_STOP;
	}

	public void setListener(PcmListener listener) {
		pcmListener = listener;
	}

	public void start() {

		if (STATE_STOP == mState && null != audioTrack) {
			mState = STATE_START;
			playedLen = 0;

			if (null != playerCallback) {

				if (null != pcmListener) {
					pcmListener.onPcmPlayStart();
				}
				while (STATE_START == mState) {
					// 获取要播放的字节数据
					BufferData data = playerCallback.getPcmPlayBuffer();

					if (null != data) {
						if (null != data.byteData) {
							
							// 首次进入，开始播放声音
							if (0 == playedLen) {
								audioTrack.play();
							}
							
							// 设置要播放的字节数据
							int len = audioTrack.write(data.byteData, 0,
									data.getFilledSize());
							
							playedLen += len;
							// 释放数据
							playerCallback.freePcmPlayData(data);
						} else {
							LogHelper.d(TAG,
									"it is the end of input, so need stop");
							break;
						}
					} else {
						LogHelper.d(TAG, "get null data");
						break;
					}

				}

				if (STATE_STOP == mState) {
					audioTrack.pause();
					audioTrack.flush();
					audioTrack.stop();
					audioTrack.release();
				}
				if (null != pcmListener) {
					pcmListener.onPcmPlayStop();
				}
			} else {
				throw new IllegalArgumentException("PcmCallback can't be null");
			}
		}
	}

	public void stop() {
		if (STATE_START == mState && null != audioTrack) {
			mState = STATE_STOP;
		}
	}
}
