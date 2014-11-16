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

import com.libra.sinvoice.Buffer.BufferData;

/**
 * 
 * @ClassName: com.libra.sinvoice.SinGenerator
 * @Description: 正弦波发生器
 * @author zhaokaiqiang
 * @date 2014-11-15 下午2:51:34
 * 
 */
public class SinGenerator {

	private static final String TAG = "SinGenerator";

	private static final int STATE_START = 1;
	private static final int STATE_STOP = 2;

	// 2^8时的峰值
	public static final int BITS_8 = 128;
	// 默认为2^16时的峰值
	public static final int BITS_16 = 32768;

	// 采样率
	public static final int SAMPLE_RATE_8 = 8000;
	public static final int SAMPLE_RATE_11 = 11250;
	public static final int SAMPLE_RATE_16 = 16000;

	public static final int UNIT_ACCURACY_1 = 4;
	public static final int UNIT_ACCURACY_2 = 8;

	private int mState;
	private int mSampleRate;
	private int mBits;

	private static final int DEFAULT_BITS = BITS_8;
	private static final int DEFAULT_SAMPLE_RATE = SAMPLE_RATE_8;
	private static final int DEFAULT_BUFFER_SIZE = 1024;

	private int mFilledSize;
	private int mBufferSize;
	private SinGeneratorCallback sinGeneratorCallback;

	public static interface SinGeneratorCallback {

		BufferData getGenBuffer();

		void freeGenBuffer(BufferData buffer);
	}

	public SinGenerator(SinGeneratorCallback callback) {
		this(callback, DEFAULT_SAMPLE_RATE, DEFAULT_BITS, DEFAULT_BUFFER_SIZE);
	}

	public SinGenerator(SinGeneratorCallback callback, int sampleRate,
			int bits, int bufferSize) {
		sinGeneratorCallback = callback;

		mBufferSize = bufferSize;
		mSampleRate = sampleRate;
		mBits = bits;

		mFilledSize = 0;
		mState = STATE_STOP;
	}

	public void stop() {
		if (STATE_START == mState) {
			mState = STATE_STOP;
		}
	}

	public void start() {
		if (STATE_STOP == mState) {
			mState = STATE_START;
		}
	}

	/**
	 * 对数字进行编码
	 * 
	 * @param genRate
	 * @param duration
	 */
	public void gen(int genRate, int duration) {
		if (STATE_START == mState) {

			// 定值16384
			int n = mBits / 2;
			int totalCount = (duration * mSampleRate) / 1000;
			double per = (genRate / (double) mSampleRate) * 2 * Math.PI;
			double d = 0;

			LogHelper.d(TAG, "per:" + per + "___genRate:" + genRate);
			if (null != sinGeneratorCallback) {
				mFilledSize = 0;
				// 获取要编码的数据
				BufferData bufferData = sinGeneratorCallback.getGenBuffer();
				if (null != bufferData) {
					for (int i = 0; i < totalCount; ++i) {

						if (STATE_START == mState) {

							// 算出不同点的正弦值
							int out = (int) (Math.sin(d) * n) + 128;

							// 如果填充数量超过了缓冲区的大小，就重置mFilledSize，释放bufferData
							if (mFilledSize >= mBufferSize - 1) {
								// free buffer
								bufferData.setFilledSize(mFilledSize);
								sinGeneratorCallback.freeGenBuffer(bufferData);

								mFilledSize = 0;
								bufferData = sinGeneratorCallback
										.getGenBuffer();
								if (null == bufferData) {
									LogHelper.d(TAG, "get null buffer");
									break;
								}
							}

							// 转码为byte类型并保存，& 0xff是为了防止负数转换出现异常
							bufferData.byteData[mFilledSize++] = (byte) (out & 0xff);
							if (BITS_16 == mBits) {
								bufferData.byteData[mFilledSize++] = (byte) ((out >> 8) & 0xff);
							}

							d += per;
						} else {
							LogHelper.d(TAG, "sin gen force stop");
							break;
						}
					}
				} else {
					LogHelper.d(TAG, "get null buffer");
				}

				if (null != bufferData) {
					bufferData.setFilledSize(mFilledSize);
					sinGeneratorCallback.freeGenBuffer(bufferData);
				}
				mFilledSize = 0;

			}
		}
	}
}
