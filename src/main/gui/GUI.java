package main.gui;

import main.game.*;
import main.game.Settings;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class GUI extends JFrame{

    private Game game;
    private ArrayList<BoardState> possibleMoves;
    private SquarePanel[] squares;

    public GUI(){
        start();
    }

    private void start(){
        settingsPopup();
        game = new Game();
        possibleMoves = new ArrayList<>();
        setup();
    }

    /**
     * Pop up dialog for user to choose game settings (e.g. AI difficulty, starting player etc)
     */
    private void settingsPopup(){
        // panel for options
        JPanel panel = new JPanel(new GridLayout(5,1));
        // difficulty slider
        JLabel text1 = new JLabel("Set Difficulty", 10);
        JSlider slider = new JSlider();
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMaximum(12);
        slider.setMinimum(1);
        slider.setValue(Settings.AI_DEPTH);
        // force takes option
        JRadioButton forceTakesButton = new JRadioButton("Force Takes");
        forceTakesButton.setSelected(Settings.FORCETAKES);
        // who gets first move?
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButton humanFirstRadioButton = new JRadioButton("You Play First");
        JRadioButton aiRadioButton = new JRadioButton("Computer Plays First");
        buttonGroup.add(humanFirstRadioButton);
        buttonGroup.add(aiRadioButton);
        aiRadioButton.setSelected(Settings.FIRSTMOVE== Player.AI);
        humanFirstRadioButton.setSelected(Settings.FIRSTMOVE== Player.HUMAN);
        // add components to panel
        panel.add(text1);
        panel.add(slider);
        panel.add(forceTakesButton);
        panel.add(humanFirstRadioButton);
        panel.add(aiRadioButton);
        // pop up
        int result = JOptionPane.showConfirmDialog(null, panel, "Game Settings",
                     JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        // process results
        if(result == JOptionPane.OK_OPTION){
            Settings.AI_DEPTH = slider.getValue();
            Settings.FIRSTMOVE = humanFirstRadioButton.isSelected() ? Player.HUMAN : Player.AI;
            Settings.FORCETAKES = forceTakesButton.isSelected();
        }
    }


    /**
     * Sets up initial GUI configuration.
     */
    public void setup()
    {
        switch (Settings.FIRSTMOVE){
            case AI:
                main.gui.Settings.AIcolour = Colour.WHITE;
                break;
            case HUMAN:
                main.gui.Settings.AIcolour = Colour.BLACK;
                break;
        }
        setupMenuBar();
        updateCheckerBoard();
        this.pack();
        this.setVisible(true);
        if (Settings.FIRSTMOVE == Player.AI){
            aiMove();
        }
    }

    /**
     * Updates the checkerboard GUI based on the game state.
     */
    private void updateCheckerBoard(){
        GridBagConstraints c = new GridBagConstraints();
        JPanel contentPane = new JPanel(new GridBagLayout());
        squares = new SquarePanel[game.getState().NO_SQUARES];
        for (int i = 0; i < game.getState().NO_SQUARES; i++){
            c.gridx = i % game.getState().SIDE_LENGTH;
            c.gridy = i / game.getState().SIDE_LENGTH;
            squares[i] = new SquarePanel(c.gridx, c.gridy);
            contentPane.add(squares[i], c);
        }
        addPieces();
        addGhostButtons();
        this.setContentPane(contentPane);
        this.pack();
        this.setVisible(true);
    }

    private void updateLater(){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateCheckerBoard();
            }
        });
    }


    /**
     * Add checker pieces to the GUI corresponding to the game state
     */
    private void addPieces(){
        for (int i = 0; i < game.getState().NO_SQUARES; i++){
            if(game.getState().getPiece(i) != null){
                Piece piece = game.getState().getPiece(i);
                CheckerButton button = new CheckerButton(i, piece);
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        onPieceClick(actionEvent);
                    }
                });
                squares[i].add(button);
            }
        }
    }

    /**
     * Add "ghost buttons" showing possible moves for the player
     */
    private void addGhostButtons(){
        for (BoardState state : possibleMoves){
            int newPos = state.getToPos();
            GhostButton button = new GhostButton(state);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    onGhostButtonClick(actionEvent);
                }
            });
            squares[newPos].add(button);
        }
    }

    /**
     * Sets up the menu bar component.
     */
    private void setupMenuBar(){

        // ensure exit method is called on window closing
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        onExitClick();
                    }
                }
        );
        // initialize components
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem restartItem = new JMenuItem("Restart");
        JMenuItem helpItem = new JMenuItem("Help");
        JMenuItem quitItem = new JMenuItem("Quit");
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");

        // add action listeners
        quitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                onExitClick();
            }
        });
        restartItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                onRestartClick();
            }
        });
        helpItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                onHelpClick();
            }
        });
        undoItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                onUndoClick();
            }
        });
        // add components to menu bar
        fileMenu.add(restartItem); fileMenu.add(helpItem); fileMenu.add(quitItem);
        menuBar.add(fileMenu);
        editMenu.add(undoItem);
        menuBar.add(editMenu);
        this.setJMenuBar(menuBar);
    }

    /***************************************************************/
    /*********************** ON CLICK METHODS **********************/

    /**
     * Occurs when user clicks on checker piece
     * @param actionEvent
     */
    private void onPieceClick(ActionEvent actionEvent){
        if(game.getTurn() == Player.HUMAN){
            CheckerButton button = (CheckerButton) actionEvent.getSource();
            int pos = button.getPosition();
            if(button.getPiece().getPlayer() == Player.HUMAN){
                possibleMoves = game.getValidMoves(Player.HUMAN, pos);
                updateCheckerBoard();
            }
        }
    }

    /**
     * Occurs when user clicks to move checker piece to new (ghost) location.
     * @param actionEvent
     */
    private void onGhostButtonClick(ActionEvent actionEvent){
        if (!game.isGameOver() && game.getTurn() == Player.HUMAN){
            GhostButton button = (GhostButton) actionEvent.getSource();
            game.playerMove(button.getBoardstate());
            possibleMoves = new ArrayList<>();
            updateCheckerBoard();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    aiMove();
                }
            });
        }
    }

    private void aiMove(){
        // perform AI move
        long startTime = System.nanoTime();
        game.aiMove();
        // compute time taken
        long aiMoveDurationInMs = (System.nanoTime() - startTime)/1000000;
        // compute necessary delay time (not less than zero)
        long delayInMs = Math.max(0, main.gui.Settings.AiMinPauseDurationInMs - aiMoveDurationInMs);
        // schedule delayed update
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        exec.schedule(new Runnable(){
            @Override
            public void run(){
                invokeAiUpdate();
            }
        }, delayInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Update checkerboard and trigger new AI move if necessary
     */
    private void invokeAiUpdate(){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateCheckerBoard();
                if (!game.isGameOver() && game.getTurn() == Player.AI){
                    aiMove();
                }
            }
        });
    }




    /**
     * Open dialog for restarting the program.
     */
    private void onRestartClick()
    {
        Object[] options = {"Yes",
                "No", };
        int n = JOptionPane.showOptionDialog(this, "Are you sure you want to restart?",
                "Restart game? ",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        if (n == 0){
            start();
        }
    }

    /**
     * Open dialog for quitting the program
     */
    private void onExitClick(){
        Object[] options = {"Yes",
                "No", };
        int n = JOptionPane.showOptionDialog(this,
                        "\nAre you sure you want to leave?",
                "Quit game? ",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        if (n == 0){
            // close logging file
            this.dispose();
            System.exit(0);
        }
    }

    /**
     * Open help dialog.
     */
    private void onHelpClick(){

        int n = JOptionPane.showOptionDialog(this,
                "\n 1. Click a piece \n " +
                       "2. Click where you want to move",
                "Instructions",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, null, null);
    }

    /**
     * Undo the last move
     */
    private void onUndoClick(){
        game.undo();
        updateCheckerBoard();
    }
}
