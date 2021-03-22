import processing.core.PApplet;
import processing.core.PVector;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import javafx.util.*;
import processing.event.MouseEvent;

enum EventType
{RemoveAll, AddCell, TurnPowerOff, DeselectCells, ShiftTranslate, RequestCells, UndoAction}

enum UndoType {AddedCellGroup, RemovedCellGroup, ShiftedCellGroup, ChangedScale, Translated}

public class Main extends PApplet
{
    static final int WINDOW_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width - 315;
    static final int WINDOW_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height - 100;
    static int WIDTH = 8;
    static final int FRAME_RATE = 60;
    private static int generation = 0;
    static boolean displayGrid;

    private static ArrayList<String> swipeInfo = new ArrayList<>();
    private static final int UNDO_LIMIT = 50;

    private static UIManager uiManager;

    static final HashMap<String, Cell> activeCells = new HashMap<>();
    static final List<Cell> selectedCells = new ArrayList<>();
    private static final List<Cell> clipboard = new ArrayList<>();
    static final HashMap<String, ArrayList<String>> presets = new HashMap<>();

    static final LinkedList<Pair<EventType, String>> eventQueue = new LinkedList<>();
    private static final LinkedList<Pair<UndoType, ArrayList<String>>> undoStack = new LinkedList<>();

    private static PVector initial = new PVector(), prevTranslation = new PVector();
    static PVector translation = new PVector();
    private static PVector lineAnchorTranslated, selectionAnchorTranslated, dragAnchor;

    public static void main(String[] args)
    {
        Main instance = new Main();
        uiManager = new UIManager();
        PApplet.runSketch(new String[]{"Main"}, instance);
        PApplet.runSketch(new String[]{"UIManager"}, uiManager);
        PApplet.runSketch(new String[]{"UI.MiniMap"}, new MiniMap(instance));
    }

    @Override
    public void settings() {
        size(WINDOW_WIDTH, WINDOW_HEIGHT, P2D);
    }

    @Override
    public void setup()
    {
        textFont(createFont("fonts/FS.ttf", 25));
        frameRate(FRAME_RATE);
        surface.setLocation(0, 25);
        surface.setResizable(true);

        synchronized (uiManager.flag)
        {
            while (!uiManager.flag.get())
            {
                try {
                    uiManager.flag.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        uiManager.loadCallback(new File("data/saves/autosave.ww"));
    }

    @Override
    public void dispose()
    {
        uiManager.saveCallback(new File("data/saves/autosave.ww"));
    }

    @Override
    public void draw()
    {
        background(25);
        pushMatrix();

        if (mousePressed && mouseButton == RIGHT)
            translation = new PVector(mouseX, mouseY).sub(initial);
        translate(translation.x, translation.y);

        handleEvents();

        if (mousePressed && mouseButton == LEFT)
        {
            if (uiManager.chooseInputType.getKey().equals("Eraser"))
                eraseCells();
            else if (uiManager.chooseInputType.getKey().equals("Pencil"))
                addCell(mouseX, mouseY, State.Wire, false, true);
        }

        if (uiManager.chooseGameState.getKey().equals("Play") && frameCount % uiManager.frameDelay == 0)
        {
            generation++;

            for (Cell cell : activeCells.values())
                cell.setNextState();

            for (Cell cell : activeCells.values())
                cell.setState();
        }

        synchronized (activeCells)
        {
            try
            {
                for (Cell cell : activeCells.values())
                    cell.display(translation);
                for (Cell cell : selectedCells)
                    cell.drawOutline(color(150, 0, 255));
            } catch (ConcurrentModificationException ignored) { }
        }

        switch (uiManager.chooseInputType.getKey())
        {
            case "Line":
                PVector endPoint = new PVector(floor((mouseX - translation.x) / WIDTH), floor((mouseY - translation.y) / WIDTH));
                ArrayList<PVector> line = Utils.getPixelsInALine(lineAnchorTranslated, endPoint);
                fill(255, 255, 0, 100);
                noStroke();
                for (PVector v : line)
                    square(v.x * WIDTH, v.y * WIDTH, WIDTH);
                break;
            case "Rectangle":
                if (selectionAnchorTranslated != null)
                {
                    endPoint = new PVector(floor((mouseX - translation.x)), floor((mouseY - translation.y)));
                    PVector middlePoint = endPoint.copy().add(selectionAnchorTranslated).div(2f);
                    float selectionWidth = abs(endPoint.x - selectionAnchorTranslated.x);
                    float selectionHeight = abs(endPoint.y - selectionAnchorTranslated.y);

                    pushStyle();
                    rectMode(CENTER);
                    stroke(255);
                    strokeWeight(1);
                    fill(150, 100);
                    rect(middlePoint.x, middlePoint.y, selectionWidth, selectionHeight);
                    popStyle();
                }
                else if (!mousePressed)
                {
                    int cellUnderMouseX = floor((mouseX - translation.x) / WIDTH) * WIDTH;
                    int cellUnderMouseY = floor((mouseY - translation.y) / WIDTH) * WIDTH;
                    Cell cellUnderMouse = activeCells.get("" + cellUnderMouseX + "_" + cellUnderMouseY);
                    if (cellUnderMouse != null && cellUnderMouse.selected)
                        cursor(MOVE);
                    else
                        cursor(ARROW);
                }
                else if (dragAnchor != null)
                {
                    PVector currentMouseLocation = new PVector(mouseX, mouseY);
                    PVector delta = currentMouseLocation.copy().sub(dragAnchor);
                    for (Cell cell : selectedCells)
                        cell.shiftCell(delta);
                    dragAnchor = currentMouseLocation.copy();
                }
                break;
            case "Eraser":
                displayEraserPreview();
                break;
            case "Custom":
                if(uiManager.choosePreset == null)
                    break;
                ArrayList<String> preset = presets.get(uiManager.choosePreset.currentItem());
                Cell[] presetPreview = Utils.constructCellsFromPreset(preset, this);
                for (Cell cell : presetPreview)
                    cell.display(translation, mouseX, mouseY, 150);
                break;
        }

        popMatrix();
        if (displayGrid)
            Utils.displayGrid(this);
        Utils.displayInformation(this, generation);
    }

    @Override
    public void mousePressed()
    {
        if (mouseButton == RIGHT)
        {
            initial = new PVector(mouseX, mouseY).sub(translation);
            return;
        }
        if (uiManager.chooseInputType.getKey().equals("Electricity"))
            addCell(mouseX, mouseY, State.Head, false, true);
        else if (uiManager.chooseInputType.getKey().equals("Line"))
        {
            lineAnchorTranslated = new PVector(floor((mouseX - translation.x) / WIDTH), floor((mouseY - translation.y) / WIDTH));
        }
        else if (uiManager.chooseInputType.getKey().equals("Rectangle") && selectionAnchorTranslated == null)
        {
            int cellUnderMouseX = floor((mouseX - translation.x) / WIDTH) * WIDTH;
            int cellUnderMouseY = floor((mouseY - translation.y) / WIDTH) * WIDTH;
            Cell cellUnderMouse = activeCells.get("" + cellUnderMouseX + "_" + cellUnderMouseY);
            if (cellUnderMouse != null && cellUnderMouse.selected)
            {
                dragAnchor = new PVector(mouseX, mouseY);
                for (Cell cell : selectedCells)
                {
                    cell.safeRemoveNonSelected();
                    cell.clearNonSelectedNeighbours();
                }
            }
            else
            {
                selectionAnchorTranslated = new PVector(mouseX - translation.x, mouseY - translation.y);
                Utils.deselectAllCells();
            }
        }
        else if (uiManager.chooseInputType.getKey().equals("Custom") && uiManager.choosePreset != null)
        {
            ArrayList<String> preset = presets.get(uiManager.choosePreset.currentItem());
            for (String parameters : preset)
            {
                String[] params = split(parameters, " ");
                int x = Integer.parseInt(params[0]) * Main.WIDTH, y = Integer.parseInt(params[1]) * Main.WIDTH;
                State state = params.length > 2 ? State.values()[Integer.parseInt(params[2])] : State.Wire;
                addCell(x + mouseX, y + mouseY, state, true, true);
            }
            addCellGroup();
        }
    }

    @Override
    public void mouseReleased()
    {
        if (mouseButton == RIGHT)
        {
            PVector deltaTranslation = translation.copy().sub(prevTranslation);
            prevTranslation = translation.copy();

            if(deltaTranslation.mag() == 0)
                return;
            swipeInfo.clear();
            swipeInfo.add(String.valueOf(deltaTranslation.x));
            swipeInfo.add(String.valueOf(deltaTranslation.y));
            shiftTranslation();
            return;
        }
        else if(uiManager.chooseInputType.getKey().equals("Pencil") && swipeInfo.size() > 0)
            addCellGroup();
        else if(uiManager.chooseInputType.getKey().equals("Eraser") && swipeInfo.size() > 0)
            removeCellGroup();
        if (uiManager.chooseInputType.getKey().equals("Line"))
        {
            PVector endPoint = new PVector(floor((mouseX - translation.x) / WIDTH), floor((mouseY - translation.y) / WIDTH));
            ArrayList<PVector> line = Utils.getPixelsInALine(lineAnchorTranslated, endPoint);
            for (PVector v : line)
                addCell(floor(v.x) * WIDTH, floor(v.y) * WIDTH, State.Wire, false, false);
            lineAnchorTranslated = null;
            addCellGroup();
        }
        else if (uiManager.chooseInputType.getKey().equals("Rectangle") && selectionAnchorTranslated != null)
        {
            PVector endPoint = new PVector(mouseX - translation.x, mouseY - translation.y);
            float left = min(endPoint.x, selectionAnchorTranslated.x);
            float top = min(endPoint.y, selectionAnchorTranslated.y);
            float selectionWidth = abs(endPoint.x - selectionAnchorTranslated.x);
            float selectionHeight = abs(endPoint.y - selectionAnchorTranslated.y);
            Utils.selectCellsWithinRectangle(new PVector(left, top), max(selectionWidth, 0), max(selectionHeight, 0));

            selectionAnchorTranslated = null;
        }
        else if (uiManager.chooseInputType.getKey().equals("Rectangle") && dragAnchor != null)
        {
            for (Cell cell : selectedCells)
            {
                int gridLockedX = round((float) cell.x / WIDTH) * WIDTH;
                int gridLockedY = round((float) cell.y / WIDTH) * WIDTH;
                cell.x = gridLockedX;
                cell.y = gridLockedY;
            }
            renameCells(selectedCells, true);
            for (Cell cell : selectedCells)
                cell.setNeighbours();
            dragAnchor = null;
        }
    }

    @Override
    public void keyPressed()
    {
        if (key == 'c' || key == 'C')
            copyCells();
        else if (key == 'v' || key == 'V')
            pasteCells();
        else if (key == 'a' || key == 'A')
            Utils.selectAllCells();
        else if (key == 'g' || key == 'G')
            displayGrid = !displayGrid;
        else if (key == 'p' || key == 'P')
            uiManager.switchGameState();
        else if(key == 'z' || key == 'Z')
        {
            try {
                undo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (key == DELETE)
        {
            for (Cell cell : selectedCells)
                removeCell(cell.key);
            removeCellGroup();
            Utils.deselectAllCells();
        }
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        scaleWorld(-event.getCount(), true, mouseX, mouseY);
    }

    private Cell addCell(int x, int y, State state, boolean forceStates, boolean addTranslate)
    {
        float offsetX = addTranslate ? translation.x : 0;
        float offsetY = addTranslate ? translation.y : 0;
        int mouseXGridLocked = floor((x - offsetX) / WIDTH) * WIDTH;
        int mouseYGridLocked = floor((y - offsetY) / WIDTH) * WIDTH;

        String key = "" + mouseXGridLocked + "_" + mouseYGridLocked;
        Cell occupant = activeCells.get(key);
        Cell added = null;

        if (forceStates)
            added = new Cell(mouseXGridLocked, mouseYGridLocked, state, key, this);
        else
        {
            if (state.equals(State.Wire))
                added = new Cell(mouseXGridLocked, mouseYGridLocked, state, key, this);
            else if (state.equals(State.Head) && occupant != null && occupant.state.equals(State.Wire))
                added = new Cell(mouseXGridLocked, mouseYGridLocked, state, key, this);
        }

        if (added != null)
        {
            if(occupant != null && occupant.state.equals(State.Wire) && state.equals(State.Wire))
                return null;
            if (occupant != null)
                occupant.safeRemove();
            synchronized (activeCells) {
                activeCells.put(key, added);
            }
            swipeInfo.add(key);
        }

        return added;
    }

    private void displayEraserPreview()
    {
        noStroke();
        fill(255, 150);

        int mouseXGridLocked = floor((mouseX - translation.x) / WIDTH);
        int mouseYGridLocked = floor((mouseY - translation.y) / WIDTH);
        ArrayList<PVector> eraserPixels = Utils.getPixelsWithinRange(mouseXGridLocked, mouseYGridLocked, uiManager.eraserSize - 1);
        for (PVector v : eraserPixels)
            square(v.x * WIDTH, v.y * WIDTH, WIDTH);
    }

    private void eraseCells()
    {
        int mouseXGridLocked = floor((mouseX - translation.x) / WIDTH);
        int mouseYGridLocked = floor((mouseY - translation.y) / WIDTH);
        ArrayList<PVector> eraserPixels = Utils.getPixelsWithinRange(mouseXGridLocked, mouseYGridLocked, uiManager.eraserSize - 1);
        for (PVector v : eraserPixels)
            removeCell((int) (v.x * WIDTH), (int) (v.y * WIDTH));
    }

    private void removeCell(int x, int y)
    {
        String key = "" + x + "_" + y;
        Cell occupant = activeCells.get(key);

        if (occupant != null)
        {
            swipeInfo.add("" + occupant.x + " " + occupant.y + " " + occupant.state);
            occupant.safeRemove();
            activeCells.remove(key);
        }
    }

    private void removeCell(String key)
    {
        Cell occupant = activeCells.get(key);

        if (occupant != null)
        {
            swipeInfo.add("" + occupant.x + " " + occupant.y + " " + occupant.state);
            occupant.safeRemove();
            activeCells.remove(key);
        }
    }

    private void handleEvents()
    {
        synchronized (eventQueue)
        {
            Pair<EventType, String> nextEvent;
            while (true)
            {
                nextEvent = eventQueue.pollLast();
                if (nextEvent == null)
                    break;
                switch (nextEvent.getKey())
                {
                    case RemoveAll:
                        removeAll();
                        break;
                    case AddCell:
                        try
                        {
                            String[] cellDataArr = PApplet.split(nextEvent.getValue(), " ");
                            int x = Integer.parseInt(cellDataArr[0]);
                            int y = Integer.parseInt(cellDataArr[1]);
                            State state = State.valueOf(cellDataArr[2]);
                            addCell(x, y, state, true, true);
                        } catch (IllegalArgumentException e)
                        {
                            println("Could not parse cell parameters.");
                        }
                        break;
                    case TurnPowerOff:
                        turnPowerOff();
                        break;
                    case DeselectCells:
                        Utils.deselectAllCells();
                        break;
                    case ShiftTranslate:
                        String[] delta = PApplet.split(nextEvent.getValue(), " ");
                        translation.add(new PVector(Integer.parseInt(delta[0]), Integer.parseInt(delta[1])));
                        break;
                    case RequestCells:
                        MiniMap.copyCells(activeCells);
                        break;
                    case UndoAction:
                        try {
                            undo();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
    }

    private void undo() throws Exception
    {
        Pair<UndoType, ArrayList<String>> lastEvent = undoStack.pollFirst();
        if (lastEvent == null)
            return;

        switch (lastEvent.getKey())
        {
            case AddedCellGroup:
                ArrayList<String> cellKeys = lastEvent.getValue();
                synchronized (activeCells)
                {
                    for(String key : cellKeys)
                    {
                        if(activeCells.get(key) == null)
                            return;
                        activeCells.get(key).safeRemove();
                        activeCells.remove(key);
                    }
                }
                Utils.deselectAllCells();
                break;

            case RemovedCellGroup:
                ArrayList<String> cellParameters = lastEvent.getValue();
                synchronized (activeCells)
                {
                    for (String param : cellParameters)
                    {
                        String[] info = split(param, " ");
                        addCell(Integer.parseInt(info[0]), Integer.parseInt(info[1]), State.valueOf(info[2]), true, false);
                    }
                }
                swipeInfo.clear();
                Utils.deselectAllCells();
                break;
            case ShiftedCellGroup:
                ArrayList<String> shiftInfo = lastEvent.getValue();
                List<Cell> shiftedCells = new ArrayList<>();
                synchronized (activeCells)
                {
                    for (String infoString : shiftInfo)
                    {
                        String[] info = split(infoString, " ");
                        Cell cell = activeCells.get(info[0]);
                        if(cell == null)
                            continue;
                        cell.shiftCell(new PVector(Integer.parseInt(info[1]), Integer.parseInt(info[2])).mult(-1));
                        shiftedCells.add(cell);
                    }
                }
                renameCells(shiftedCells, false);
                for (Cell cell : shiftedCells)
                {
                    cell.safeRemoveNonSelected();
                    cell.setNeighbours();
                }
                break;
            case ChangedScale:
                ArrayList<String> dirArr = lastEvent.getValue();
                scaleWorld(-Integer.parseInt(dirArr.get(0)), false, Integer.parseInt(dirArr.get(1)), Integer.parseInt(dirArr.get(2)));
                break;
            case Translated:
                ArrayList<String> translationArr = lastEvent.getValue();
                PVector deltaTranslation = new PVector(Float.parseFloat(translationArr.get(0)), Float.parseFloat(translationArr.get(1)));
                translation.sub(deltaTranslation);
                prevTranslation = translation.copy();
                break;
        }
    }

    private void renameCells(List<Cell> targetGroup, boolean canUndo)
    {
        List<String> keysOfCellsToBeRemoved = new ArrayList<>();
        List<Pair<String, Cell>> pairsToBeAdded = new ArrayList<>();
        List<String> cellShiftInfo = new ArrayList<>();

        for(Cell cell : targetGroup)
        {
            String key = cell.key;
            String properKey = "" + cell.x + "_" + cell.y;
            if (!key.equals(properKey))
            {
                String[] oldCoords = split(key, "_");
                int shiftX = cell.x - Integer.parseInt(oldCoords[0]), shiftY = cell.y - Integer.parseInt(oldCoords[1]);
                cellShiftInfo.add(properKey + " " + shiftX + " " + shiftY);
                cell.key = properKey;
                keysOfCellsToBeRemoved.add(key);
                pairsToBeAdded.add(new Pair<>(properKey, cell));
            }
        }

        for (String key : keysOfCellsToBeRemoved)
            activeCells.remove(key);
        for (Pair<String, Cell> pair : pairsToBeAdded)
            activeCells.put(pair.getKey(), pair.getValue());

        if(canUndo)
        {
            swipeInfo.clear();
            swipeInfo.addAll(cellShiftInfo);
        }
        shiftCellGroup();
    }

    private void copyCells()
    {
        clipboard.clear();
        //clipboard.addAll(selectedCells);
        for(Cell cell : selectedCells)
        {
            cell.cellWidthWhenCopied = WIDTH;
            clipboard.add(cell);
        }
    }

    private void pasteCells()
    {
        if (clipboard.size() == 0)
            return;
        int mouseXGridLocked = floor((float) mouseX / WIDTH) * WIDTH;
        int mouseYGridLocked = floor((float) mouseY / WIDTH) * WIDTH;

        Cell referenceCell = clipboard.get(0);
        int minSumXY = Integer.MAX_VALUE;
        for (Cell cell : clipboard)
            if (cell.x + cell.y < minSumXY)
            {
                minSumXY = cell.x + cell.y;
                referenceCell = cell;
            }

        Utils.deselectAllCells();
        for (Cell cell : clipboard)
        {
            int pastedX = mouseXGridLocked + (int)((cell.x - referenceCell.x) * (double)WIDTH / cell.cellWidthWhenCopied);
            int pastedY = mouseYGridLocked + (int)((cell.y - referenceCell.y) * (double)WIDTH / cell.cellWidthWhenCopied);
            Cell addedCell = addCell(pastedX, pastedY, cell.state, true, true);
            if (addedCell == null)
                continue;
            addedCell.selected = true;
            selectedCells.add(addedCell);
        }
        addCellGroup();
    }

    private void scaleWorld(int dir, boolean canUndo, int targetX, int targetY)
    {
        dir /= abs(dir);
        float mult = pow(2, dir);
        int newWidth = round(WIDTH * mult);
        if(newWidth < 2 || newWidth > 128)
            return;

        List<String> cellInfo = new ArrayList<>();
        for(Cell cell : activeCells.values())
            cellInfo.add("" + floor((float)cell.x / WIDTH) + " " + floor((float)cell.y / WIDTH) + " " + cell.state + " " + cell.selected);
        WIDTH = newWidth;
        Utils.deselectAllCells();
        activeCells.clear();
        for (String param : cellInfo)
        {
            String[] info = split(param, " ");
            Cell cell = addCell(Integer.parseInt(info[0]) * WIDTH, Integer.parseInt(info[1]) * WIDTH, State.valueOf(info[2]), true, false);
            if(cell == null)
                continue;
            if(Boolean.parseBoolean(info[3]))
            {
                cell.selected = true;
                selectedCells.add(cell);
            }
        }
        PVector shift = new PVector(targetX, targetY);
        if(dir > 0)
            translation.mult(mult).sub(shift);
        else
            translation.add(shift).mult(mult);
        prevTranslation = translation.copy();

        if(canUndo)
        {
            swipeInfo.clear();
            swipeInfo.add(String.valueOf(dir));
            swipeInfo.add(String.valueOf(targetX));
            swipeInfo.add(String.valueOf(targetY));
            changeScale();
        }
    }

    private void turnPowerOff()
    {
        for (Cell cell : activeCells.values())
            cell.forceState(State.Wire);
    }

    private void removeAll()
    {
        String[] keys = activeCells.keySet().toArray(new String[0]);
        for(String key : keys)
            removeCell(key);
        removeCellGroup();
    }

    private void shiftTranslation() {
        addUndoAction(UndoType.Translated);
    }

    private void changeScale() {
        addUndoAction(UndoType.ChangedScale);
    }

    private void shiftCellGroup() {
        addUndoAction(UndoType.ShiftedCellGroup);
    }

    private void addCellGroup() {
        addUndoAction(UndoType.AddedCellGroup);
    }

    private void removeCellGroup() {
        addUndoAction(UndoType.RemovedCellGroup);
    }

    private void addUndoAction(UndoType type)
    {
        if(swipeInfo.size() == 0)
            return;
        undoStack.push(new Pair<>(type, (ArrayList<String>)swipeInfo.clone()));
        if(undoStack.size() > UNDO_LIMIT)
            undoStack.removeLast();
        swipeInfo.clear();
    }
}
