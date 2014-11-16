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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * @ClassName: com.libra.sinvoice.Buffer
 * @Description: 缓冲器
 * @author zhaokaiqiang
 * @date 2014-11-15 下午1:35:46
 * 
 */
public class Buffer {

	private final static String TAG = "Buffer";
	// 生产者队列
	private BlockingQueue<BufferData> mProducerQueue;
	// 消费者队列
	private BlockingQueue<BufferData> mConsumeQueue;
	// 缓冲区数量
	private int mBufferCount;
	// 缓冲区体积
	private int mBufferSize;

	public Buffer() {
		this(Common.DEFAULT_BUFFER_COUNT, Common.DEFAULT_BUFFER_SIZE);
	}

	public Buffer(int bufferCount, int bufferSize) {

		mBufferSize = bufferSize;
		mBufferCount = bufferCount;
		mProducerQueue = new LinkedBlockingQueue<BufferData>(mBufferCount);
		// we want to put the end buffer, so need to add 1
		mConsumeQueue = new LinkedBlockingQueue<BufferData>(mBufferCount + 1);

		// 初始化生产者队列
		for (int i = 0; i < mBufferCount; ++i) {
			try {
				mProducerQueue.put(new BufferData(mBufferSize));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void reset() {
		// 将生产者的空头结点剔除
		int size = mProducerQueue.size();
		for (int i = 0; i < size; ++i) {
			BufferData data = mProducerQueue.peek();
			if (null == data || null == data.byteData) {
				mProducerQueue.poll();
			}
		}

		// 将消费者中的非空数据添加到生产者当中
		size = mConsumeQueue.size();
		for (int i = 0; i < size; ++i) {
			BufferData data = mConsumeQueue.poll();
			if (null != data && null != data.byteData) {
				mProducerQueue.add(data);
			}
		}

		LogHelper.d(TAG, "reset ProducerQueue Size:" + mProducerQueue.size()
				+ "    ConsumeQueue Size:" + mConsumeQueue.size());
	}

	final public int getEmptyCount() {
		return mProducerQueue.size();
	}

	final public int getFullCount() {
		return mConsumeQueue.size();
	}

	// 获取生产者的头结点，阻塞式
	public BufferData getEmpty() {
		return getImpl(mProducerQueue);
	}

	// 加入到生产者中
	public boolean putEmpty(BufferData data) {
		return putImpl(data, mProducerQueue);
	}

	// 获取消费者的头结点
	public BufferData getFull() {
		return getImpl(mConsumeQueue);
	}

	// 加入到消费者中
	public boolean putFull(BufferData data) {
		return putImpl(data, mConsumeQueue);
	}

	// 获取队列的头结点
	private BufferData getImpl(BlockingQueue<BufferData> queue) {
		if (null != queue) {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// 将数据加入到队列中
	private boolean putImpl(BufferData data, BlockingQueue<BufferData> queue) {
		if (null != queue && null != data) {
			try {
				queue.put(data);
				return true;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	// when mData is null, means it is end of input
	public static class BufferData {
		// 数据容器
		public byte byteData[];
		// 填充体积
		private int mFilledSize;
		// 缓冲最大体积
		private int mMaxBufferSize;
		// 静态空缓冲区
		private static BufferData sEmptyBuffer = new BufferData(0);

		public BufferData(int maxBufferSize) {

			mMaxBufferSize = maxBufferSize;
			mFilledSize = 0;

			if (maxBufferSize > 0) {
				byteData = new byte[mMaxBufferSize];
			} else {
				byteData = null;
			}
		}

		/**
		 * 获取空的缓冲区
		 * 
		 * @return
		 */
		public static BufferData getEmptyBuffer() {
			return sEmptyBuffer;
		}

		// 重置填充数量
		final public void reset() {
			mFilledSize = 0;
		}

		final public int getMaxBufferSize() {
			return mMaxBufferSize;
		}

		// 设置填充数量
		final public void setFilledSize(int size) {
			mFilledSize = size;
		}

		final public int getFilledSize() {
			return mFilledSize;
		}
	}

}
