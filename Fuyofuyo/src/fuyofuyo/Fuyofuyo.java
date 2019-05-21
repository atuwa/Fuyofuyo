package fuyofuyo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.LineBorder;

public class Fuyofuyo extends JFrame{

	private static double x,y=0;
	private int move=0;
	private boolean jflag;
	private double moved;
	private static double time;
	private static double scale=0.5;
	private static BufferedImage img;
	private static URL[] imagepath;
	private static int fps;
	private static int imgIndex;
	private boolean lockd;
	public static FrameRate Fps=new FrameRate();
	private TrayIcon trayIcon;
	public static boolean trayModeUseable;
	private boolean closed;

	public Fuyofuyo(boolean trayMode) throws IOException{
		super("ふよふよさせるやつ");
		Canvas sc=new Canvas();
		setContentPane(sc);
		JRootPane root=this.getRootPane();
		root.setBorder(new LineBorder(Color.black, 0));
		root.setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
		JLayeredPane layeredPane = root.getLayeredPane();
		Component c = layeredPane.getComponent(1);
		if (c instanceof JComponent) {
			JComponent orgTitlePane = (JComponent) c;
			orgTitlePane.removeAll();
			orgTitlePane.setLayout(new BorderLayout());
			orgTitlePane.add(new JPanel(new BorderLayout()));
		}
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500,500);
		Mouse ml=new Mouse();
		addMouseListener(ml);
		addMouseMotionListener(ml);
		addMouseWheelListener(ml);
		addKeyListener(new Keybord());
		//背景色を透明にします。
		//ウィンドウ装飾を無くしておかないとjre1.7からはエラーが発生します。
		//setBackground(new Color(0,0,0,0));
		Fps.setFPS(fps=30);//基本30fps
		setAlwaysOnTop(true);//デフォルト最前面固定
		setIconImage(img);
		bound();
		//ウィンドウ範囲を示す枠をつけておく
		//this.getRootPane().setBorder(new LineBorder(Color.black, 2));
		if(trayMode)try{
			tray();
			setType(Type.UTILITY);
			trayModeUseable=true;
		}catch(Exception e){
			trayIcon=null;
			e.printStackTrace();
			System.err.println("タスクトレイ動作モードが使えないみたいです");
			System.err.println("ウィンドウ動作モードで起動します");
		}
		setVisible(true);
		new Thread("位置更新") {
			@Override
			public void run() {
				while(!closed) {
					move();
					Fps.count();
					Fps.sleep();
				}
			}
		}.start();
	}
	private void tray()throws Exception{
		PopupMenu popup = new PopupMenu(); //ポップアップメニューを生成
		trayIcon=new TrayIcon(img,"ふよふよさせるやつ",popup);
		//ポップアップメニューの中身を作成
		MenuItem item0 = new MenuItem("最前面");
		item0.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(true);
			}
		});
		MenuItem item1 = new MenuItem("非表示");
		item1.addActionListener(new ActionListener(){
			private Checker c;
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean v=isVisible();
				setVisible(!v);
				if(v) {
					item1.setLabel("表示");
					c=new Checker(trayIcon);
					c.start();
				}else{
					item1.setLabel("非表示");
					if(c!=null)c.interrupt();
				}
			}
		});
		MenuItem item2 = new MenuItem("終了");
		item2.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				 System.exit(0);
			}
		});
		MenuItem item3 = new MenuItem("切替");
		item3.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				nextCharcter();
			}
		});
		MenuItem item4 = new MenuItem("ウィンドウモード");
		item4.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				mode(false);
			}
		});
		popup.add(item0);
		popup.add(item1);
		popup.add(item3);
		popup.add(item2);
		popup.add(item4);
		trayIcon.setImageAutoSize(true); // リサイズ
		SystemTray.getSystemTray().add(trayIcon);
	}
	private static class Checker extends Thread{
		private TrayIcon tray;
		public Checker(TrayIcon trayIcon){
			super("放置チェッカー");
			tray=trayIcon;
		}
		public void run() {
			try{
				Thread.sleep(30*60*1000);
			}catch(InterruptedException e){return;}
			tray.displayMessage("放置されています","しばらく表示されていません",MessageType.INFO);
			try{
				Thread.sleep(30*60*1000);
			}catch(InterruptedException e){return;}
			tray.displayMessage("放置されています","非表示の状態で1時間放置されたので終了します",MessageType.INFO);
			System.exit(0);
		}
	}
	public void nextCharcter(){
		while(true) {
			try{
				imgIndex++;
				if(imgIndex>=imagepath.length)imgIndex=0;
				if(imagepath[imgIndex]!=null)img=ImageIO.read(imagepath[imgIndex]);
				break;
			}catch(IOException err) {

			}
		}
		bound();
		setIconImage(img);
		if(trayIcon!=null)trayIcon.setImage(img);
	}
	private static String[] load() throws IOException {
		ArrayList<String> list=new ArrayList<String>();
		//String[] path= {"mona.png","youmu.png","youmu1.png","sizuha.png"};//ここに書き足すと増やせる
		URL pathtxt=ClassLoader.getSystemResource("path.txt");
		if(pathtxt==null)return new String[0];
		InputStream is=pathtxt.openStream();
		InputStreamReader isr=new InputStreamReader(is,StandardCharsets.UTF_8);
		BufferedReader br=new BufferedReader(isr);
		try {
			while(true) {
				String line=br.readLine();
				if(line==null)break;
				list.add(line);
			}
		}catch(FileNotFoundException fnf){

		}finally {
			br.close();
		}
		return list.toArray(new String[list.size()]);
	}
	//TODO 内部画像編集エディタ作る
	public static void main(String[] args) throws IOException {
		if(args==null)args=new String[0];
		try{
			String[] path=load();
			imagepath=new URL[path.length+args.length];
			if(imagepath.length<1) {
				System.err.println("1つ以上の画像ファイルをpath.txtに指定してください");
				System.exit(0);
			}
			for(int i=0;i<args.length;i++)	imagepath[i]=new File(args[i]).toURI().toURL();
			for(int i=0;i<path.length;i++)imagepath[args.length+i]=ClassLoader.getSystemResource(path[i]);
		}catch(IOException e){
			e.printStackTrace();
			return;
		}
		img=ImageIO.read(imagepath[0]);
		//OSのウィンドウ装飾を無くして、Look&Feelの装飾にしておきます。
		JFrame.setDefaultLookAndFeelDecorated(true);
		new Fuyofuyo(true);
	}
	public class Canvas extends JPanel {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			try{
				render(g);
			}catch(Throwable t) {
				t.printStackTrace();
				System.exit(0);
			}
		}
	}
	public void move() {
		if(move==0&!lockd) {
			y+=Math.sin(time/(fps/3))*2;//*(size/30000d);
		}
		if(move==1) {
			x-=3.5;
		}else if(move==2) {
			x+=3.5;
		}
		if(jflag){
			y-=Math.sin(moved)*3;
			moved-=0.1;
		    if(moved<0)jflag=false;
		}else {
			if(moved<1) {
				y+=Math.sin(moved)*3;
				moved+=0.1;
			}
		}
		time++;
		this.setLocation((int)x,(int) y);
	}
	public void render(Graphics g){
		if(img!=null)g.drawImage(img,0,0,(int)(img.getWidth()*scale),(int)(img.getHeight()*scale),null);
	}
	public void mode(final boolean task) {
		new Thread("モード切替") {
			public void run() {
				close();
				try{
					new Fuyofuyo(task);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}.start();
	}
	public void close(){
		closed=true;
		setVisible(false);
		SystemTray.getSystemTray().remove(trayIcon);
		this.dispose();
	}
	protected void bound() {
		Rectangle b=getBounds();
		setBounds(b.x,b.y,(int)(img.getWidth()*scale),(int)(img.getHeight()*scale));
		b=getBounds();
		/*
		if(b.width*b.height>921600) {//ハーフHD解像度相当以上
			if(b.width*b.height>2073600) {//フルHD解像度相当以上
				Fps.setFPS(fps=10);
			}else Fps.setFPS(fps=20);
		}else Fps.setFPS(fps=30);
		*/
		System.out.println("大きさ"+b.width+"x"+b.height+"="+(b.width*b.height)+"px");
		//背景色を透明にします。
		//ウィンドウ装飾を無くしておかないとjre1.7からはエラーが発生します。
		setBackground(new Color(0,0,0,0));
	}
	private class Mouse extends MouseAdapter{
		int px,py;
		public void mouseWheelMoved(MouseWheelEvent e){
			int oldW=(int)(img.getWidth()*scale);
			int oldH=(int)(img.getHeight()*scale);
			double oldScale=scale;
			scale+=e.getWheelRotation()/20d;
			int newW=(int)(img.getWidth()*scale);
			int newH=(int)(img.getHeight()*scale);
			if(newW<35||newH<35)scale=oldScale;
			else if(scale>10)scale=10;
			//System.out.println("幅差"+(oldW-newW));
			//System.out.println("高差"+(oldH-newH));
			if(scale!=oldScale) {
				x+=(oldW-newW)/2;
				y+=(oldH-newH)/2;
				setLocation((int)x,(int) y);
			}
			System.out.println("拡大率"+scale+"倍");
			bound();
		}
		public void mouseReleased(MouseEvent e){
			lockd=false;
		}
		public void mousePressed(MouseEvent e){
			px=e.getX();
			py=e.getY();
			lockd=true;
			if(e.getButton()!=MouseEvent.BUTTON1) {
				System.exit(0);
			}
		}
		@Override
		public void mouseMoved(MouseEvent e){
			//System.out.println("mouseMoved");
		}
		@Override
		public void mouseDragged(MouseEvent e){
			//System.out.println("mouseDragged");
			Rectangle b=getBounds();
			y=b.y-py+e.getY();
			x=b.x-px+e.getX();
			setBounds((int)x,(int)y,b.width,b.height);
		}
	}
	public class Keybord extends KeyAdapter{
		public void keyReleased(KeyEvent e){
			int kc=e.getKeyCode();
			if(kc==KeyEvent.VK_LEFT||kc==KeyEvent.VK_RIGHT) {
				move=0;
			}
		}
		public void keyPressed(KeyEvent e){
			int kc=e.getKeyCode();
			if(kc==KeyEvent.VK_LEFT) {
				move=1;
			}else if(kc==KeyEvent.VK_RIGHT) {
				move=2;
			}else if(kc==KeyEvent.VK_SPACE){
				moved=1;
				jflag=true;
			}else if(kc==KeyEvent.VK_SHIFT){
				if(trayIcon==null&&!trayModeUseable);
				else mode(trayIcon==null);
			}else if(kc==KeyEvent.VK_ESCAPE) {
				System.exit(0);
			}else if(kc==KeyEvent.VK_ENTER) {
				setAlwaysOnTop(!isAlwaysOnTop());
			}else if(kc==KeyEvent.VK_UP) {
				nextCharcter();
			}else if(kc==KeyEvent.VK_DOWN){
				while(true) {
					try{
						imgIndex--;
						if(imgIndex<0)imgIndex=imagepath.length-1;
						if(imagepath[imgIndex]!=null)img=ImageIO.read(imagepath[imgIndex]);
						break;
					}catch(IOException err) {

					}
				}
				bound();
				setIconImage(img);
				if(trayIcon!=null)trayIcon.setImage(img);
			}
		}
	}
}