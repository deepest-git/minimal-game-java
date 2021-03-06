import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ball.Ball;

/*So now the file structure is
  |  
  |
  --Balls Jpanel
  |
  --interface for scoreboard
  |
  -- game class
  
*/

class Balls extends JPanel {

    scoreCallBack scoreCallBack;
    Image sky,meteor;
    private List<Ball> ballsUp;
    public int ballsCount = 10;

    public Balls(scoreCallBack callBack) {
        this.scoreCallBack=callBack;
        sky=Toolkit.getDefaultToolkit().getImage("./assets/sky.png");
        meteor=Toolkit.getDefaultToolkit().getImage("./assets/meteor.png");

        ballsUp = new ArrayList<Ball>(ballsCount);

        for (int index = 0; index < ballsCount; index++) {
            Ball ball=new Ball(meteor);
            ball.setSize(30, 30);
            ball.setBallValue(String.valueOf(5 + random(45)));
            ballsUp.add(ball);
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                super.mouseClicked(me);
                for (Ball ball : ballsUp) {
                    if (verifyBallClick(ball, me.getPoint())) {
                        ball.setRock();
                        ball.valueColor=new Color(0,0,0,Color.TRANSLUCENT);
                        scoreCallBack.ballClicked(ball);
                    }
                }
            }
        });

        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scoreCallBack.updateTimeLeft();
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.drawImage(sky, 0, 0,null);
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.valueOf("Score: "+scoreCallBack.getScore()), 10, 15);
        g2d.drawString(String.valueOf("Time left: "+scoreCallBack.getTimeLeft()), 145, 15);
        g2d.drawString(String.valueOf("Remaining Clicks: "+scoreCallBack.getRemClicks()), 270, 15);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Ball ball : ballsUp) {
            ball.paint(g2d);
        }
        g2d.dispose();
    }

    public List<Ball> getBalls() {
        return ballsUp;
    }

    private boolean verifyBallClick(Ball ball, Point point) {
        return (ball.getLocation().x-10 <= point.x && ball.getLocation().x+ball.getSize().width >= point.x+10) && (ball.getLocation().y-25 <= point.y && ball.getLocation().y+ball.getSize().height >= point.y);
    }

    public static int random(int maxRange) {
        return (int) Math.round((Math.random() * maxRange));
    }
}

interface scoreCallBack{
    public void ballClicked(Ball ball);
    public int getScore();
    public int getRemClicks();
    public int getTimeLeft();
    public void updateTimeLeft();
}

public class game implements scoreCallBack{

    static game Game;
    static int score;
    static int clicks=10;
    static int timeLeft=30;
    Thread engine;
    JFrame frame;

    public static void main(String[] args) {
        Game=new game();
    }

    public game() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException ex) {
                } catch (InstantiationException ex) {
                } catch (IllegalAccessException ex) {
                } catch (UnsupportedLookAndFeelException ex) {
                }

                frame = new JFrame("Ball Dropping Game");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout());
                scoreCallBack sBack=Game;
                Balls balls = new Balls(sBack);
                frame.add(balls);
                frame.setSize(400, 720);
                frame.setVisible(true);

                engine=new Thread(new DropEngine(balls));
                engine.start();

            }
        });
    }

    @Override
    public void ballClicked(Ball ball){
        if(setRemClicks())
        score+=Integer.valueOf(ball.getBallValue());
        System.out.println("Score "+"+"+ball.getBallValue()+": "+score);
        ball.setBallValue("");
    }

    @Override
    public int getScore(){
        return score;
    }

    @Override
    public int getRemClicks(){
        return clicks;
    }

    boolean setRemClicks(){
        boolean ret;
        if(clicks>0){
            --clicks;
            ret=true;
        }
        else{
            ret=false;
        }
        return ret;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void updateTimeLeft() {
        timeLeft--;
    }

    Thread gameOver= new Thread(){
        public void run(){
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    GameOverPanel gPanel = new GameOverPanel(50, 260, score);
                    frame.add(gPanel);
                    frame.revalidate();
                }
            });
        }
    };

    public static int random(int maxRange) {
        return (int) Math.round((Math.random() * maxRange));
    }

    public class DropEngine implements Runnable {

        private Balls parent;

        public DropEngine(Balls parent) {
            this.parent = parent;
        }

        @Override
        public void run() {

            int width = getParent().getWidth();
            int height = getParent().getHeight();
            int i = 1;

            // Randomize the starting position...
            for (Ball ball : getParent().getBalls()) {
                int x = 10 + random(width - 50);

                int y = 120 * i + random(30);
                i++;

                ball.setLocation(new Point(x, -y));

            }

            while (getParent().isVisible()) {
                
                // Repaint the balls pen...
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getParent().repaint();
                    }
                });
                
                // If any of time or clicks gets finished the game will be over
                if (timeLeft <= 0 || clicks <= 0) {
                    gameOver.run();
                    engine.stop();
                }
                
                // This is a little dangrous, as it's possible
                // for a repaint to occur while we're updating...
                for (Ball ball : getParent().getBalls()) {
                    if (ball.getLocation().y > height) {
                        int y = -50;
                        int x = 10 + random(width - 50);
                        ball.setLocation(new Point(x, y));
                        ball.setSize(35, 35);
                        ball.setBallValue(String.valueOf(5 + random(45)));
                    }
                    move(ball);
                }

                // Some small delay...
                try {
                    Thread.sleep(15);
                } catch (InterruptedException ex) {
                }

            }

        }

        public Balls getParent() {
            return parent;
        }

        public void move(Ball ball) {

            Point p = ball.getLocation();
            p.y++;

        }
    }
}


class GameOverPanel extends JPanel {

    int x, y, score;

    public GameOverPanel(int x,int y, int score){
        this.x=x;
        this.y=y;
        this.score = score;
        this.setForeground(Color.WHITE);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponents(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.fillRect(x, y, 280, 160);
        g2d.setColor(Color.RED);
        g2d.drawString("GAME OVER!", 155, 330);
        g2d.drawString("Your Score is " + score + ".", 145, 350);
        g2d.drawString("Play Again :)", 155, 370);
    }

}