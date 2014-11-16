package com.example.sinvoicedemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.SinVoicePlayer;
import com.libra.sinvoice.SinVoiceRecognition;

/**
 * 
 * @ClassName: com.example.sinvoicedemo.MainActivity
 * @Description: 声波通信
 * @author zhaokaiqiang
 * @date 2014-11-15 下午12:36:32
 * 
 */
public class MainActivity extends Activity implements
		SinVoiceRecognition.Listener, SinVoicePlayer.Listener {

	private final static String TAG = "MainActivity";
	// 最大数字
	private final static int MAX_NUMBER = 5;
	// 识别成功
	private final static int MSG_SET_RECG_TEXT = 1;
	// 开始识别
	private final static int MSG_RECG_START = 2;
	// 识别结束
	private final static int MSG_RECG_END = 3;

	private final static String CODEBOOK = "12345";

	private Handler mHanlder;
	// 播放
	private SinVoicePlayer mSinVoicePlayer;
	// 录音
	private SinVoiceRecognition mRecognition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mSinVoicePlayer = new SinVoicePlayer(CODEBOOK);
		mSinVoicePlayer.setListener(this);

		mRecognition = new SinVoiceRecognition(CODEBOOK);
		mRecognition.setListener(this);

		final TextView playTextView = (TextView) findViewById(R.id.play_text);
		mHanlder = new RegHandler((TextView) findViewById(R.id.regtext));

		// 开始播放声音
		findViewById(R.id.start_play).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String text = genText(7);
				playTextView.setText(text);
				mSinVoicePlayer.play(text);
			}
		});

		// 停止播放声音
		findViewById(R.id.stop_play).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mSinVoicePlayer.stop();
			}
		});

		// 开始声音识别
		findViewById(R.id.start_reg).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mRecognition.start();
			}
		});

		// 停止声音识别
		findViewById(R.id.stop_reg).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mRecognition.stop();
			}
		});
	}

	// 获取长度为count且最大值为MAX_NUMBER的随机数
	private String genText(int count) {
		StringBuilder sb = new StringBuilder();
		int pre = 0;
		while (count > 0) {
			int x = (int) (Math.random() * MAX_NUMBER + 1);
			if (Math.abs(x - pre) > 0) {
				sb.append(x);
				--count;
				pre = x;
			}
		}

		return sb.toString();
	}

	private static class RegHandler extends Handler {

		private StringBuilder mTextBuilder = new StringBuilder();
		private TextView mRecognisedTextView;

		public RegHandler(TextView textView) {
			mRecognisedTextView = textView;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SET_RECG_TEXT:
				char ch = (char) msg.arg1;
				mTextBuilder.append(ch);
				if (null != mRecognisedTextView) {
					mRecognisedTextView.setText(mTextBuilder.toString());
				}
				break;

			case MSG_RECG_START:
				mTextBuilder.delete(0, mTextBuilder.length());
				break;

			case MSG_RECG_END:
				LogHelper.d(TAG, "recognition end");
				break;
			}
		}
	}

	@Override
	public void onRecognitionStart() {
		mHanlder.sendEmptyMessage(MSG_RECG_START);
	}

	@Override
	public void onRecognition(char ch) {
		mHanlder.sendMessage(mHanlder.obtainMessage(MSG_SET_RECG_TEXT, ch, 0));
	}

	@Override
	public void onRecognitionEnd() {
		mHanlder.sendEmptyMessage(MSG_RECG_END);
	}

	@Override
	public void onPlayStart() {
		// 开始播放音频
		LogHelper.d(TAG, "start play");
	}

	@Override
	public void onPlayEnd() {
		// 结束播放
		LogHelper.d(TAG, "stop play");
	}

}
