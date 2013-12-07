package org.noxo;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.input.*;

//noxo 2012 // noxo@mbnet.fi

public class Game extends Application {

	private Vector <Sprite>collisionList;
	
	private int energy = 100;
	private int score=0;
	private boolean gameOver = false;
	
	private WritableImage imgStarL1;
	private WritableImage imgStarL2;

	private Image imgBackground;

	private static final int TYPE_DUMMY = 0;
	private static final int TYPE_ROCK = 1;
	private static final int TYPE_SHIP = 2;

	private SpriteManager spriteManager;
	private Sprite spriteSpaceShip;
	
	private Sprite spriteStarsL1[];
	private Sprite spriteStarsL2[];
	
	private AnimationTimer animTimer;

	private int spaceShipMoveH = 0;
	private int spaceShipMoveV = 0;

	private int spaceShipSpeedH = 0;
	private int spaceShipSpeedV = 0;

	private final int spaceShipSpeed = 15;
	private final int spaceShipStepSize = 1;
	
	private final int STAR_COUNT_L1 = 20;
	private final int STAR_COUNT_L2 = 10;
	
	private final int STAR_SPEED_L1 = 2;
	private final int STAR_SPEED_L2 = 5;
	
	private final Random random = new Random();
	
	private static Image imgRock[] = new Image[19];
	
	private final int MAX_ROCK_RATE = 10000;
	private final int ROCK_SPEED = 5;

	private int maxRocks = 6;

	private long lastRock = System.currentTimeMillis();
	
	private Canvas gameCanvas = new Canvas(1024,768);
	
	private final int pixelStarL1[] = { 
			0xFF969696,0xFF969696,
			0xFF969696,0xFF969696
	};

	private final int pixelStarL2[] = { 
			0xFFDEDEDE, 0xFFDEDEDE,
			0xFFDEDEDE, 0xFFDEDEDE
	};
	
	Timer fpsTimer = new Timer();
	long fps, frames;
	
	final double MOUSE_THRESHOLD = 50;
	
	@Override
	public void start(final Stage stage) throws Exception {
		
		Group gameNode = new Group(gameCanvas);
		Scene gameScene = new Scene(gameNode);
	
		gameScene.setOnKeyPressed( new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				handleKeyPressed(event);
			}

		});

		gameScene.setOnMouseClicked(new EventHandler<MouseEvent>() {

			public void handle(MouseEvent e) {
				
				if (spriteSpaceShip != null) {

					double mx = e.getX();
					double my = e.getY();
					double sx = spriteSpaceShip.getX();
					double sy = spriteSpaceShip.getY();

					if (Math.abs(mx-sx) > MOUSE_THRESHOLD)
						spaceShipMoveH = mx < sx ? -1 : 1;

					if (Math.abs(my-sy) > MOUSE_THRESHOLD)
						spaceShipMoveV = my < sy ? -1 : 1;
					
				}
			}
		});

		//stage.setFullScreen(true);
		stage.setScene(gameScene);
		stage.show();
		
		initialize();
		initGame();
		
		animTimer = new AnimationTimer() {
			
			@Override
			public void handle(long arg0) {
				gameLoop();
			}
			
		};
		
		animTimer.start();
		
	}
	
	private void startFPS() {

		final long start = System.currentTimeMillis();

		fpsTimer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				long now = System.currentTimeMillis();
				long execTime = now - start;
				fps = frames * 1000 / execTime;
			}
		}, 1000, 1000);
		
	}
	
	private void gameLoop() {
		
		updateGame();
		
		if (energy <= 0) {
			gameOver = true;
			animTimer.stop();
			fpsTimer.cancel();
		}	
		
		renderGame();
		
		if (frames == 0)
			startFPS();
		
		frames++;
		
	}
	
	private void updateGame() {
		moveStars();
		moveRocks();
		moveSpaceShip();
		checkCollision();
		score++;
	}

	private void renderGame() {
		
		
		GraphicsContext gc = gameCanvas.getGraphicsContext2D();
		
		double w = gameCanvas.getWidth();
		double h = gameCanvas.getHeight();
		
		// background
		gc.drawImage(imgBackground,0,0,w,h);
		
		// sprites
		spriteManager.processSprites(gc);
		 
		// energy bar

		gc.setFill(Color.rgb(0,255,0));
		double el = 100 * (  energy / 100f );
		gc.fillRect(w-250,30,el,10);
		
		// score
		gc.setFill(Color.rgb(255, 255, 255));
		gc.fillText(String.valueOf(score),w-250, 55);
		
		// fps
		
		gc.fillText("FPS: " + fps,30, 30);

		// gameover
		if (gameOver) {
			gc.setFill(Color.rgb(255, 0,0));
			gc.fillText("Game over",w-250, 30);
		}

	}
	
	private void initialize() {
		
		imgBackground = new Image("hdspace2.jpg");
		spriteManager = new SpriteManager(imgBackground);
		
		Image imgSpaceShip = new Image("spacecraft.png");
		spriteSpaceShip = new Sprite(imgSpaceShip);
		
		imgStarL1 = new WritableImage(2, 2);
		imgStarL1.getPixelWriter().setPixels(0,0,2,2,PixelFormat.getIntArgbInstance(),
				pixelStarL1, 0, 2);

		imgStarL2 = new WritableImage(2, 2);
		imgStarL2.getPixelWriter().setPixels(0,0,2,2,PixelFormat.getIntArgbInstance(),
				pixelStarL2, 0, 2);
		
		for (int i=0;i<=17;i++) {
			Log.v("TAG", " rock: AsteroidFrame" + (i) + ".png");
			imgRock[i] =  new Image("AsteroidAnimated" + i + ".png");
		}
		
	}
	
	private void initGame() {
		
		double w = gameCanvas.getWidth();
		double h = gameCanvas.getHeight();
		spriteManager.reset();
		
		// stars
		
		spriteStarsL1 = new Sprite[STAR_COUNT_L1];
		
		for (int i=0;i<spriteStarsL1.length;i++) {
			int x = random.nextInt((int)w);
			int y = random.nextInt((int)h);
			Sprite s = new Sprite(imgStarL1);
			s.move(x, y);
			spriteStarsL1[i] = s;
			spriteManager.addSprite(s);
		}

		spriteStarsL2 = new Sprite[STAR_COUNT_L2];
		
		for (int i=0;i<spriteStarsL2.length;i++) {
			int x = random.nextInt((int)w);
			int y = random.nextInt((int)h);
			Sprite s = new Sprite(imgStarL2);
			s.move(x, y);
			spriteStarsL2[i] = s;
			spriteManager.addSprite(s);
		}

		// spaceship
		spriteManager.addSprite(spriteSpaceShip);
		spriteSpaceShip.move(w / 2, h / 2);

		energy = 100;
		score = 0;
		
	}
	
	
	private void checkCollision() {
		
		collisionList = spriteManager.getCollidedSprites(spriteSpaceShip);
		
		for (int i=0;i<collisionList.size();i++) {
			
			Sprite s = collisionList.elementAt(i);
			
			if (s.getTypeId() == TYPE_ROCK) {
				
				double rx = s.getX();
				
				energy -= 1;
				spaceShipSpeedH += 4;
				spaceShipSpeedV += rx < spriteSpaceShip.getX() ? 3 : -3;
				
			}
		}
		
	}
	
	private void moveRocks() {
		
		long t = System.currentTimeMillis();
		
		double w = gameCanvas.getWidth();
		double h = gameCanvas.getHeight();
		
		Vector <Sprite>sprites = spriteManager.getSprites();
		
		int rockCount = 0;
		
		for (int i=sprites.size()-1;i>0;i--) {
			
			Sprite s = sprites.elementAt(i);
			
			if (s.getTypeId() == TYPE_ROCK) {
				
				double x = s.getX();
				double y = s.getY();
				y += ROCK_SPEED;
				s.move(x, y);
				
				if (y > h) {
					spriteManager.removeSprite(s);
					continue;
				}
				
				rockCount++;
			}
		}
		
		if (t - lastRock > random.nextInt(MAX_ROCK_RATE) 
				&& rockCount < maxRocks) {
			
			lastRock = t;
			int x = random.nextInt( (int) w);
			int y = 0;
			Sprite rock = new Sprite(imgRock, x, y);
			rock.setTypeId(TYPE_ROCK);
			rock.setAnimSpeed(50);
			spriteManager.addSprite(rock);
			
		}
	}
	
	private void moveStars() {
		
		double h = gameCanvas.getHeight();
		double w = gameCanvas.getWidth();
		
		for (int i=0;i<spriteStarsL1.length;i++) {
			
			double x = spriteStarsL1[i].getX();
			double y = spriteStarsL1[i].getY();
			
			y += STAR_SPEED_L1;
			
			if (y > h) {
				x = random.nextInt((int)w);
				y = random.nextInt((int)h);
			}

			spriteStarsL1[i].setX(x);
			spriteStarsL1[i].setY(y);

		}

		for (int i=0;i<spriteStarsL2.length;i++) {
			
			double x = spriteStarsL2[i].getX();
			double y = spriteStarsL2[i].getY();
			
			y += STAR_SPEED_L2;
			
			if (y > h) {
				x = random.nextInt((int)w);
				y = random.nextInt((int)h);
			}

			spriteStarsL2[i].setX(x);
			spriteStarsL2[i].setY(y);

		}

	}
	
	private void moveSpaceShip() {

		double spX = spriteSpaceShip.getX();
		double spY = spriteSpaceShip.getY();

		if (spaceShipMoveH != 0) {
			spaceShipSpeedH = spaceShipMoveH < 0 ? -spaceShipSpeed
					: spaceShipSpeed;
			spaceShipMoveH = 0;
		}

		if (spaceShipMoveV != 0) {
			spaceShipSpeedV = spaceShipMoveV < 0 ? -spaceShipSpeed
					: spaceShipSpeed;
			spaceShipMoveV = 0;
		}

		if (spaceShipSpeedH != 0) {
			if (spaceShipSpeedH < 0) {
				spaceShipSpeedH += spaceShipStepSize;
				spaceShipSpeedH = spaceShipSpeedH > 0 ? 0 : spaceShipSpeedH;
			} else {
				spaceShipSpeedH -= spaceShipStepSize;
				spaceShipSpeedH = spaceShipSpeedH < 0 ? 0 : spaceShipSpeedH;
			}
		}

		if (spaceShipSpeedV != 0) {
			if (spaceShipSpeedV < 0) {
				spaceShipSpeedV += spaceShipStepSize;
				spaceShipSpeedV = spaceShipSpeedV > 0 ? 0 : spaceShipSpeedV;
			} else {
				spaceShipSpeedV -= spaceShipStepSize;
				spaceShipSpeedV = spaceShipSpeedV < 0 ? 0 : spaceShipSpeedV;
			}
		}
		
		double newX = spX + spaceShipSpeedH;
		double newY = spY + spaceShipSpeedV;
		
		newX = newX < 0 ? 0 : newX;
		newY = newY < 0 ? 0 : newY;
		
		double sw = spriteSpaceShip.getWidth();
		double sh = spriteSpaceShip.getHeight();
		double w = gameCanvas.getWidth();
		double h = gameCanvas.getHeight();
		
		if (newX+sw > w)
			newX = w-sw;
		
		if (newY+sh > h)
			newY = h-sh;
		
		spriteSpaceShip.setX(newX);
		spriteSpaceShip.setY(newY);
		
	}
	
	public void handleKeyPressed(KeyEvent event) {
		
		if (event.getCode() == KeyCode.LEFT) {
			spaceShipMoveV = -1;
		}
		if (event.getCode() == KeyCode.RIGHT) {
			spaceShipMoveV = 1;
		}
		if (event.getCode() == KeyCode.UP) {
			spaceShipMoveH = -1;
		}
		if (event.getCode() == KeyCode.DOWN) {
			spaceShipMoveH = 1;
		}
		if (event.getCode() == KeyCode.ESCAPE) {
			animTimer.stop();
			fpsTimer.cancel();
			Platform.exit();
		}

	}

	public static void main(String arg[]) {
		launch(arg);
	}
	
}
