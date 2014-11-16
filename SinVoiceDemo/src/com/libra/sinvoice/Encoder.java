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

import java.util.List;

import com.libra.sinvoice.Buffer.BufferData;

/**
 * 
 * @ClassName: com.libra.sinvoice.Encoder
 * @Description: 编码器
 * @author zhaokaiqiang
 * @date 2014-11-16 下午1:32:17
 * 
 */
public class Encoder implements SinGenerator.SinGeneratorCallback {

	private final static String TAG = "Encoder";
	private final static int STATE_ENCODING = 1;
	private final static int STATE_STOPED = 2;

	// index 0, 1, 2, 3, 4, 5, 6
	// circleCount 31, 28, 25, 22, 19, 15, 10
	private final static int[] CODE_FREQUENCY = { 1422, 1575, 1764, 2004, 2321,
			2940, 4410 };
	private int mState;

	private SinGenerator mSinGenerator;

	private EncoderCallback encoderCallback;

	public static interface EncoderCallback {

		void freeEncodeBuffer(BufferData buffer);

		BufferData getEncodeBuffer();
	}

	public Encoder(EncoderCallback callback, int sampleRate, int bits,
			int bufferSize) {
		encoderCallback = callback;
		mState = STATE_STOPED;
		mSinGenerator = new SinGenerator(this, sampleRate, bits, bufferSize);
	}

	public final static int getMaxCodeCount() {
		return CODE_FREQUENCY.length;
	}

	public final boolean isStoped() {
		return (STATE_STOPED == mState);
	}

	// content of input from 0 to (CODE_FREQUENCY.length-1)
	public void encode(List<Integer> codes, int duration) {
		if (STATE_STOPED == mState) {
			mState = STATE_ENCODING;

			mSinGenerator.start();
			for (int index : codes) {
				if (STATE_ENCODING == mState) {
					if (index >= 0 && index < CODE_FREQUENCY.length) {
						// 使用正弦发生器编码
						mSinGenerator.gen(CODE_FREQUENCY[index], duration);
					} else {
						LogHelper.d(TAG, "code index error");
					}
				} else {
					LogHelper.d(TAG, "encode force stop");
					break;
				}
			}
			mSinGenerator.stop();

		}
	}

	public void stop() {
		if (STATE_ENCODING == mState) {
			mState = STATE_STOPED;
			mSinGenerator.stop();
		}
	}

	@Override
	public BufferData getGenBuffer() {
		if (null != encoderCallback) {
			return encoderCallback.getEncodeBuffer();
		}
		return null;
	}

	@Override
	public void freeGenBuffer(BufferData buffer) {
		if (null != encoderCallback) {
			encoderCallback.freeEncodeBuffer(buffer);
		}
	}
}
