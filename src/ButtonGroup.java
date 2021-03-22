import java.util.ArrayList;
import java.util.List;

class ButtonGroup
{
    private List<Button> buttons = new ArrayList<>();
    private Button currentButton = null;
    private int tintColor;

    ButtonGroup(int tintColor)
    {
        this.tintColor = tintColor;
    }

    void addButton(Button button)
    {
        buttons.add(button);
        currentButton = button;
    }

    void display()
    {
        for (Button button : buttons)
            button.display(button.equals(currentButton), tintColor);
    }

    void mousePressed()
    {
        for (Button button : buttons)
            if (button.check())
                currentButton = button;
    }

    void forceCurrentButton(String key)
    {
        for (Button button : buttons)
            if (button.getKey().equals(key))
            {
                currentButton = button;
                break;
            }
    }

    String getKey()
    {
        if (currentButton == null)
            return null;
        return currentButton.getKey();
    }
}
