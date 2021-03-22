import javafx.util.Pair;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class UIManager extends PApplet
{
    final AtomicBoolean flag = new AtomicBoolean();
    private final int WINDOW_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height - 385;

    ButtonGroup chooseGameState;
    ButtonGroup chooseInputType;
    private HashMap<String, Button> clickButtons = new HashMap<>();
    ExpandableList choosePreset;
    static String[] presetNames;

    private HashMap<String, Slider> sliders = new HashMap<>();
    int frameDelay = 16, eraserSize = 1;

    @Override
    public void settings()
    {
        int WINDOW_WIDTH = 300;
        size(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    @Override
    public void setup()
    {
        textFont(createFont("fonts/FS.ttf", 50));
        surface.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - 310, 310);

        int iconW = 50;
        chooseGameState = new ButtonGroup(color(105, 235, 255));
        chooseGameState.addButton(new Button(185, 50, iconW, "play.png", this, "Play"));
        chooseGameState.addButton(new Button(255, 50, iconW, "pause.png", this, "Pause"));

        chooseInputType = new ButtonGroup(color(165, 255, 117));
        chooseInputType.addButton(new Button(115, 50, iconW, "line.png", this, "Line"));
        chooseInputType.addButton(new Button(45, 130, iconW, "electricity.png", this, "Electricity"));
        chooseInputType.addButton(new Button(115, 130, iconW, "rectangle.png", this, "Rectangle"));
        chooseInputType.addButton(new Button(75, 290, iconW, "eraser.png", this, "Eraser"));
        chooseInputType.addButton(new Button(45, 210, iconW, "blueprintFilled.png", this, "Custom"));
        chooseInputType.addButton(new Button(45, 50, iconW, "pencil.png", this, "Pencil"));

        clickButtons.put("Power", new Button(185, 130, iconW, "power.png", this, "Power"));
        clickButtons.put("Trash", new Button(255, 130, iconW, "trash.png", this, "Trash"));
        clickButtons.put("Undo", new Button(115, 210, iconW, "undo.png", this, "Undo"));
        clickButtons.put("Save", new Button(185, 210, iconW, "save.png", this, "Save"));
        clickButtons.put("Load", new Button(255, 210, iconW, "load.png", this, "Load"));
        clickButtons.put("Speed", new Button(225, 290, iconW, "speed.png", this, "Speed"));

        sliders.put("speedSlider", new HorizontalSlider(225, 370, width * 0.35f, 7, this));
        sliders.get("speedSlider").setHandle(0.6f);
        sliders.put("eraserSlider", new HorizontalSlider(75, 370, width * 0.35f, 7, this));
        sliders.get("eraserSlider").setHandle(0);

        Utils.loadPresets();
        if(presetNames.length > 0)
            choosePreset = new ExpandableList(presetNames, 65, 400, width * 0.45f, 35, 5, this);
        synchronized (flag)
        {
            flag.set(true);
            flag.notify();
        }
    }

    @Override
    public void draw()
    {
        background(25);
        chooseGameState.display();
        chooseInputType.display();

        for (Button button : clickButtons.values())
            button.display(false, 0);

        for (Slider slider : sliders.values())
            slider.display();

        if (chooseInputType.getKey().equals("Custom"))
        {
            if(choosePreset != null)
                choosePreset.display();
            else
            {
                fill(255);
                textSize(25);
                textAlign(CENTER, CENTER);
                text("No Presets", width / 2f, 760);
            }
        }

        displayText();
    }

    @Override
    public void mousePressed()
    {
        chooseGameState.mousePressed();
        chooseInputType.mousePressed();
        if (chooseInputType.getKey().equals("Custom") && choosePreset != null)
            choosePreset.mousePressed();

        synchronized (Main.eventQueue)
        {
            Main.eventQueue.push(new Pair<>(EventType.DeselectCells, ""));
        }

        if (clickButtons.get("Trash").check())
        {
            synchronized (Main.eventQueue)
            {
                Main.eventQueue.push(new Pair<>(EventType.RemoveAll, ""));
            }
        }

        if (clickButtons.get("Power").check())
        {
            synchronized (Main.eventQueue)
            {
                Main.eventQueue.push(new Pair<>(EventType.TurnPowerOff, ""));
            }
        }

        if (clickButtons.get("Save").check())
        {
            forceGameRunning(false);
            selectOutput("Save a file", "saveCallback");
        }

        if (clickButtons.get("Load").check())
        {
            forceGameRunning(false);
            selectOutput("Load a file", "loadCallback");
        }

        if (clickButtons.get("Undo").check())
        {
            synchronized (Main.eventQueue)
            {
                Main.eventQueue.push(new Pair<>(EventType.UndoAction, ""));
            }
        }

        for (Slider slider : sliders.values())
            slider.press();
    }

    @Override
    public void mouseReleased()
    {
        for (Slider slider : sliders.values())
            slider.release();
    }

    @Override
    public void mouseWheel(MouseEvent event)
    {
        if (chooseInputType.getKey().equals("Custom") && choosePreset != null)
            choosePreset.mouseWheel(event);
    }

    @Override
    public void keyPressed()
    {
        if (key == 'g' || key == 'G')
            Main.displayGrid = !Main.displayGrid;
        if (key == 'p' || key == 'P')
            switchGameState();
    }

    private void displayText()
    {
        textSize(30);
        fill(255);

        frameDelay = (int) pow(2, (1f - sliders.get("speedSlider").val()) * 4.91f);
        textAlign(CENTER, BOTTOM);
        text(Utils.round2F(1000f * frameDelay / Main.FRAME_RATE) + "ms", 225, 350);

        eraserSize = (int) map(sliders.get("eraserSlider").val(), 0, 1, 1, 10);
        text(eraserSize, 75, 350);
    }

    void switchGameState()
    {
        forceGameRunning(chooseGameState.getKey().equals("Pause"));
    }

    private void forceGameRunning(boolean state)
    {
        if (state)
            chooseGameState.forceCurrentButton("Play");
        else
            chooseGameState.forceCurrentButton("Pause");
    }

    public void saveCallback(File file)
    {
        if (file == null)
            return;
        try {
            Utils.save(file.getAbsolutePath(), this);
        } catch (RuntimeException e)
        {
            System.out.println("Could not save the file.");
        }
    }

    public void loadCallback(File file)
    {
        if (file == null)
            return;
        try {
            Utils.load(file.getAbsolutePath(), this);
        } catch (RuntimeException e) {
            System.out.println("Could not load the file at: " + file.getAbsolutePath());
        }
    }
}
