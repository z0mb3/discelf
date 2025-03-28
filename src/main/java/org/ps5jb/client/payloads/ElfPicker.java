package org.ps5jb.client.payloads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.*;
import org.havi.ui.event.*;
import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;

/**
 * Component which renders UI to pick ELF
 */
public class ElfPicker extends HContainer
        implements UserEventListener, HKeyListener, HFocusListener {

    protected int elfSelectedIndex;

    protected boolean aborted;
    protected boolean canceled;
    protected boolean accepted;

    protected ElfPicker() {
        elfSelectedIndex = -1;

        canceled = false;
        accepted = false;
        aborted = false;
    }

    @Override
    public void focusGained(FocusEvent focusEvent)
    {
        if (focusEvent.getSource() instanceof Component) {
            Component source = (Component) focusEvent.getSource();
            if ("ELFlist".equals(source.getName())) {
                HListGroup listGroup = (HListGroup) source;
                elfSelectedIndex = listGroup.getCurrentIndex();
            }
        }
    }

    @Override
    public void focusLost(FocusEvent focusEvent)
    {
        if (focusEvent.getSource() instanceof Component) {
            Component source = (Component) focusEvent.getSource();
            if ("ELFlist".equals(source.getName())) {
                HListGroup listGroup = (HListGroup) source;
                elfSelectedIndex = listGroup.getCurrentIndex();
            }
        }
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public boolean isFocusTraversable() {
        return true;
    }

    @Override
    public void paint(Graphics graphics) {
        if (isShowing()) {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
        super.paint(graphics);
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
            switch (userEvent.getCode()) {
                case HRcEvent.VK_COLORED_KEY_0: // red
                    aborted = true;
                    break;
                case HRcEvent.VK_COLORED_KEY_1: // green
                    accepted = true;
                    break;
                case HRcEvent.VK_COLORED_KEY_3: // yellow
                    canceled = true;
                    break;
                case KeyEvent.VK_0:
                    break;
                case KeyEvent.VK_1:
                    break;
                case KeyEvent.VK_2:
                    break;
                case KeyEvent.VK_3:
                    break;
                case KeyEvent.VK_4:
                    break;
                case KeyEvent.VK_DOWN:
                    if (!(userEvent.getSource() instanceof Component)) {
                        getComponent("ELFlist").requestFocus();
                    }
                    break;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        // Do nothing
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getSource() instanceof Component) {
            Component source = (Component) keyEvent.getSource();
            if ("host".equals(source.getName())) {
                HSinglelineEntry textCtrl = (HSinglelineEntry) source;
                if (keyEvent.getKeyCode() == KeyEvent.VK_UP) {
                    incrementInt(textCtrl, 1, 0, 255);
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
                    incrementInt(textCtrl, -1, 0, 255);
                } else if (keyEvent.getKeyCode() == 461) {
                    // Square button deletes previous char
                    int caretPos = textCtrl.getCaretCharPosition();
                    String ip = textCtrl.getTextContent(HState.NORMAL_STATE);
                    if ((caretPos > 0 && ip.charAt(caretPos - 1) == '.') ||
                            (caretPos > 1 && ip.charAt(caretPos - 2) == '.') ||
                            (caretPos == 1)) {
                        // Do nothing
                    } else {
                        textCtrl.deletePreviousChar();
                    }
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        // Do nothing
    }

    protected void incrementInt(HSinglelineEntry control, int delta, int min, int max) {
        String ip = control.getTextContent(HState.NORMAL_STATE);
        int caretPos = control.getCaretCharPosition();
        int prevDot = 0;
        int nextDot = ip.indexOf('.');
        while (nextDot != -1 && nextDot < caretPos) {
            prevDot = nextDot;
            nextDot = ip.indexOf('.', nextDot + 1);
        }
        if (nextDot == -1) {
            nextDot = ip.length();
        }

        String ipComponent = ip.substring(prevDot == 0 ? 0 : prevDot + 1, nextDot);
        int componentVal;
        try {
            componentVal = Integer.parseInt(ipComponent) + delta;
        } catch (NumberFormatException e) {
            componentVal = min;
        }

        if (componentVal >= min && componentVal <= max) {
            String newIp = ip.substring(0, prevDot == 0 ? 0 : prevDot + 1) + componentVal + ip.substring(nextDot);
            control.setTextContent(newIp, HState.NORMAL_STATE);
            control.setCaretCharPosition(caretPos);
        }
    }

    protected Component getComponent(String name) {
        Component result = null;
        for (int i = 0; i < getComponentCount(); ++i) {
            Component comp = getComponent(i);
            if (name.equals(comp.getName())) {
                result = comp;
                break;
            }
        }
        return result;
    }


    protected void setStaticText(String componentName, String value) {
        Component comp = getComponent(componentName);
        HStaticText textComp = (HStaticText) comp;
        Font font = textComp.getFont();
        if (font == null) {
            font = getFont();
        }
        FontMetrics fm = getFontMetrics(font);
        textComp.setTextContent(value, HState.NORMAL_STATE);
        textComp.setSize(fm.stringWidth(value), textComp.getHeight());
    }

    /**
     * Renders the component and blocks execution until RED button is pressed in the menu.
     *
     * @return True if user decided to proceed with the rest of execution. False to abort.
     */
    public int render() {
        EventManager.getInstance().addUserEventListener(this, new OverallRepository());
        try {
            HScene scene = HSceneFactory.getInstance().getDefaultHScene();
            scene.add(this, BorderLayout.CENTER, 0);
            try {
                scene.validate();

                while (!this.canceled && !this.accepted && !this.aborted) {
                    scene.repaint();
                    Thread.yield();
                }
            } finally {
                this.setVisible(false);
                scene.remove(this);
            }

            // Apply changes
            if (accepted)
            {
                applySelection();
            }
            else if (canceled)
            {
            }
        } finally {
            EventManager.getInstance().removeUserEventListener(this);
        }

        if (aborted) return -1;

        return elfSelectedIndex;
    }

    /**
     * Once rendering is done, apply selected changes.
     */
    protected void applySelection()
    {
        //println("applySelection");
    }

    /**
     * Constructs an instance of ElfPicker component
     * which can be added to the scene for rendering.
     *
     * @return New ElfPicker component instance.
     */
    public static ElfPicker createComponent(String[] listElf) {
        ElfPicker elfpicker = new ElfPicker();
        elfpicker.setSize(Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
        elfpicker.setFont(new Font(null, Font.PLAIN, 18));
        elfpicker.setBackground(Color.lightGray);
        elfpicker.setForeground(Color.black);
        elfpicker.setVisible(true);

        final Font font = elfpicker.getFont();
        final FontMetrics fm = elfpicker.getFontMetrics(font);
        final Font valueFont = new Font(null, Font.BOLD, 22);
        final FontMetrics vfm = elfpicker.getFontMetrics(valueFont);
        final Font hintFont = new Font(null, Font.ITALIC, 14);
        final FontMetrics hfm = elfpicker.getFontMetrics(hintFont);

        final int horizonalLabelSpace = 5;
        final int horizonalControlSpace = 20;
        final int verticalLabelSpace = 1;
        final int verticalControlSpace = 20;
        final int labelHeight = fm.getHeight();
        final int controlHeight = 50;

        String text1 = "Select a ELF from the list. Then press TRIANGLE-Button and confirm with GREEN. The ELF files need to be in the folder 'elf-payloads' on root of the BR disc.";
        HStaticText text1ctrl = new HStaticText(text1, 80, 80, fm.stringWidth(text1), labelHeight);
        text1ctrl.setForeground(Color.black);
        text1ctrl.setBordersEnabled(false);
        elfpicker.add(text1ctrl);

        HListElement[] elfElements = new HListElement[listElf.length];
        for( int i = 0; i < listElf.length; i++)
        {
            elfElements[i] = new HListElement(listElf[i]);
        }
        HListGroup elfListCtrl = new HListGroup(elfElements, text1ctrl.getX(), text1ctrl.getY() + text1ctrl.getHeight() + verticalLabelSpace, 600, 7*controlHeight);
        elfListCtrl.addHFocusListener(elfpicker);
        elfListCtrl.setMultiSelection(false);
        elfListCtrl.setName("ELFlist");
        elfListCtrl.setForeground(Color.black);
        elfListCtrl.setBackground(Color.white);
        elfpicker.add(elfListCtrl);
        elfListCtrl.requestFocus();

        // Set how to switch focus between inputs
        elfListCtrl.setMove(KeyEvent.VK_RIGHT, elfListCtrl);
        elfListCtrl.setMove(KeyEvent.VK_LEFT, elfListCtrl);

        return elfpicker;
    }
}
