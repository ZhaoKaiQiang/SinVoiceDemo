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

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;
import android.text.TextUtils;

import com.libra.sinvoice.Buffer.BufferData;

/**
 * 
 * @ClassName: com.libra.sinvoice.SinVoicePlayer
 * @Description: 声音播放类
 * @author zhaokaiqiang
 * @date 2014-11-15 下午12:56:57
 * 
 */
public class SinVoicePlayer implements Encoder.EncoderCallback,
		PcmPlayer.PcmListener, PcmPlayer.PcmCallback {

	private final static String TAG = "SinVoicePlayer";

	private final static int STATE_START = 1;
	private final static int STATE_STOP = 2;
	private final static int STATE_PENDING = 3;

	// 默认的持续时间
	private final static int DEFAULT_GEN_DURATION = 100;

	private String mCodeBook;
	// 用于存放使用CoodBook编码过的数字
	private List<Integer> mCodes = new ArrayList<Integer>();

	private Encoder mEncoder;
	private PcmPlayer pcmPlayer;
	private Buffer mBuffer;

	private int mState;
	private Listener mListener;
	private Thread mPlayThread;
	private Thread mEncodeThread;

	public static interface Listener {

		void onPlayStart();

		void onPlayEnd();
	}

	public SinVoicePlayer() {
		this(Common.DEFAULT_CODE_BOOK);
	}

	public SinVoicePlayer(String codeBook) {
		this(codeBook, Common.DEFAULT_SAMPLE_RATE, Common.DEFAULT_BUFFER_SIZE,
				Common.DEFAULT_BUFFER_COUNT);
	}

	/**
	 * 构造函数
	 * 
	 * @param codeBook
	 * @param sampleRate
	 *            采样率
	 * @param bufferSize
	 *            缓冲区体积
	 * @param buffCount
	 *            缓冲区数量
	 */
	public SinVoicePlayer(String codeBook, int sampleRate, int bufferSize,
			int buffCount) {

		mState = STATE_STOP;
		mBuffer = new Buffer(buffCount, bufferSize);

		mEncoder = new Encoder(this, sampleRate, SinGenerator.BITS_16,
				bufferSize);
		pcmPlayer = new PcmPlayer(this, sampleRate,
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
				bufferSize);
		pcmPlayer.setListener(this);

		setCodeBook(codeBook);
	}

	public void setListener(Listener listener) {
		mListener = listener;
	}

	public void setCodeBook(String codeBook) {
		if (!TextUtils.isEmpty(codeBook)
				&& codeBook.length() < Encoder.getMaxCodeCount() - 1) {
			mCodeBook = codeBook;
		}
	}

	/**
	 * 将要加密的文本根据CodeBook进行编码
	 * 
	 * @param text
	 * @return 是否编码成功
	 */
	private boolean convertTextToCodes(String text) {
		boolean ret = true;

		if (!TextUtils.isEmpty(text)) {
			mCodes.clear();
			mCodes.add(Common.START_TOKEN);
			int len = text.length();
			for (int i = 0; i < len; ++i) {
				char ch = text.charAt(i);
				int index = mCodeBook.indexOf(ch);
				if (index > -1) {
					mCodes.add(index + 1);
				} else {
					ret = false;
					LogHelper.d(TAG, "invalidate char:" + ch);
					break;
				}
			}
			if (ret) {
				mCodes.add(Common.STOP_TOKEN);
			}
		} else {
			ret = false;
		}

		return ret;
	}

	public void play(final String text) {
		if (STATE_STOP == mState && null != mCodeBook
				&& convertTextToCodes(text)) {
			mState = STATE_PENDING;

			mPlayThread = new Thread() {
				@Override
				public void run() {
					pcmPlayer.start();
				}
			};

			if (null != mPlayThread) {
				mPlayThread.start();
			}

			mEncodeThread = new Thread() {
				@Override
				public void run() {
					mEncoder.encode(mCodes, DEFAULT_GEN_DURATION);
					stopPlayer();
					mEncoder.stop();
					pcmPlayer.stop();
				}
			};
			if (null != mEncodeThread) {
				mEncodeThread.start();
			}

			mState = STATE_START;
		}
	}

	public void stop() {
		if (STATE_START == mState) {
			mState = STATE_PENDING;
			mEncoder.stop();
			if (null != mEncodeThread) {
				try {
					mEncodeThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					mEncodeThread = null;
				}
			}

		}
	}

	private void stopPlayer() {
		if (mEncoder.isStoped()) {
			pcmPlayer.stop();
		}

		// put end buffer
		mBuffer.putFull(BufferData.getEmptyBuffer());

		if (null != mPlayThread) {
			try {
				mPlayThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				mPlayThread = null;
			}
		}

		mBuffer.reset();
		mState = STATE_STOP;
	}

	@Override
	public void freeEncodeBuffer(BufferData buffer) {
		if (null != buffer) {
			mBuffer.putFull(buffer);
		}
	}

	@Override
	public BufferData getEncodeBuffer() {
		return mBuffer.getEmpty();
	}

	@Override
	public BufferData getPcmPlayBuffer() {
		// 获取播放资源
		return mBuffer.getFull();
	}

	@Override
	public void freePcmPlayData(BufferData data) {
		// 释放播放资源
		mBuffer.putEmpty(data);
	}

	@Override
	public void onPcmPlayStart() {
		if (null != mListener) {
			mListener.onPlayStart();
		}
	}

	@Override
	public void onPcmPlayStop() {
		if (null != mListener) {
			mListener.onPlayEnd();
		}
	}

}
