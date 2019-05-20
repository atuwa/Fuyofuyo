package fuyofuyo;

import java.text.DecimalFormat;

public class FrameRate{

	private long basetime; //測定基準時間
	private int count; //フレーム数
	private float framerate; //フレームレート
	public DecimalFormat df;

	//フレームレートを取得
	public float getFrameRate(){
		return framerate;
	}
	public double getFrameRateDouble(){
		return actualFPS;
	}
	/**
	 * 描画時に呼ぶ
	 **/
	public void count(){
		++count; //フレーム数をインクリメント
		long now=System.currentTimeMillis(); //現在時刻を取得
		if(now-basetime>=500){ //１秒以上経過していれば
			framerate=((float) (count*500)/(float) (now-basetime))*2; //フレームレートを計算
			basetime=now; //現在時刻を基準時間に
			count=0; //フレーム数をリセット
		}
	}
	// 期待するFPS（1秒間に描画するフレーム数）
	public float FPS;

	// 1フレームで使える持ち時間
	private long PERIOD=(long) (1.0/FPS*1000000000); // 単位: ns

	// FPSを計算する間隔（1s = 10^9ns）
	private static long MAX_STATS_INTERVAL=1000000000L; // 単位: ns

	// FPS計算用
	private long calcInterval=0L; // in ns
	private long prevCalcTime;

	// フレーム数
	private long frameCount=0;

	// 実際のFPS
	private double actualFPS=0.0;

	private long beforeTime,afterTime,timeDiff,sleepTime;
	private long overSleepTime=0L;
	private int noDelays=0;
	private String Mode;

	public FrameRate(){
		this(30);
		basetime=System.currentTimeMillis(); //基準時間をセット
	}
	public FrameRate(float FPS){
		basetime=System.currentTimeMillis(); //基準時間をセット
		prevCalcTime=beforeTime=System.nanoTime();
		setFPS(FPS);
		df = new DecimalFormat("0.00");
	}
	public void setFPS(float fps){
		basetime=System.currentTimeMillis(); //基準時間をセット
		prevCalcTime=beforeTime=System.nanoTime();
		FPS=fps;
		PERIOD=(long) (1.0/FPS*1000000000); // 単位: ns
	}
	/*
	public String getFPS() {
	        return df.format(actualFPS);
	}
	 */
	/**
	 * 設定した(デフォルトは40)FPSの値になるようにスレッドのsleep時間を制御する
	 *
	 * @return 待機した時間(ナノ秒)<br>
	 * 待機しなかった場合-1
	 */
	public long sleep(){
		getSleepTime();
		if(sleepTime>0){
			/*
			 * 休止時間がとれる場合
			 */
			try{
				Thread.sleep(sleepTime/1000000L); // nano->ms
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
		notifyOverSleepTimeAndCalcFPS(); // sleep()の誤差を通知　※getSleepTime()に反映させる必要があるため
		if(sleepTime>0){
			return sleepTime;
		}
		return -1;
	}
	@Override
	public String toString() {
		return df.format(getFrameRateDouble());
	}
	private long getSleepTime(){
		afterTime=System.nanoTime();
		timeDiff=afterTime-beforeTime;
		// 前回のフレームの休止時間誤差も引いておく
		sleepTime=(PERIOD-timeDiff)-overSleepTime;

		return sleepTime;
	}

	private void notifyOverSleepTimeAndCalcFPS(){
		if(sleepTime>0){
			overSleepTime=(System.nanoTime()-afterTime)-sleepTime;
		}else{
			// 状態更新・レンダリングで時間を使い切ってしまい休止時間がとれない場合
			overSleepTime=0L;
			// 休止なしが16回以上続いたら
			if(++noDelays>=16&&Mode!="Game"){
				Thread.yield(); // 他のスレッドを強制実行
				noDelays=0;
			}
		}
		beforeTime=System.nanoTime();

		// FPSを計算
		calcFPS();
	}

	/**
	 * FPSの計算
	 */
	private void calcFPS(){
		frameCount++;
		calcInterval+=PERIOD;

		// 1秒おきにFPSを再計算するようにする
		if(calcInterval>=MAX_STATS_INTERVAL){
			long timeNow=System.nanoTime();
			// 実際の経過時間を測定
			long realElapsedTime=timeNow-prevCalcTime; // 単位: ns

			// 実際のFPSを計算
			// realElapsedTimeの単位はnsなのでsに変換する
			actualFPS=((double) frameCount/realElapsedTime)*1000000000L;
			frameCount=0L;
			calcInterval=0L;
			prevCalcTime=timeNow;
		}
	}
	public FrameRate setMode(String s){
		Mode=s;
		return this;
	}
	public String getMode(){
		return Mode;
	}
}
