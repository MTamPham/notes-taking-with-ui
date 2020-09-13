package com.example;

import com.example.models.Note;
import com.example.services.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.annotation.PreDestroy;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

@SpringBootApplication
@EnableJpaAuditing // auto-fill data for AuditModel
@EnableJpaRepositories(basePackages = {"com.example.repositories"}) // missing this leads to failure in creating repository bean
@EntityScan(basePackages = {"com.example.models"}) // scanning entity
public class NotesTakingWithUiApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(NotesTakingWithUiApplication.class);
    private static final int PREFERRED_WIDTH = 350;
    private static final int PREFERRED_HEIGHT = 850;
    private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private static final int ADD_BUTTON_WIDTH = 50;
    private static final int ADD_BUTTON_HEIGHT = 50;
    private JDesktopPane desktop = new JDesktopPane();

    @Autowired
    private NoteService noteService;

    public static void main(String[] args) {
        // disable Spring banner with logo
        SpringApplication app = new SpringApplication(NotesTakingWithUiApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        // set headless to be false, otherwise it throws an exception
        // because it is true by default to not instantiate AWT
        app.setHeadless(false);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        JFrame frame = new JFrame("My Stickies");
        //frame.setSize(this.PREFERRED_WIDTH, this.PREFERRED_HEIGHT);
        frame.setSize(NotesTakingWithUiApplication.screenSize.width, NotesTakingWithUiApplication.screenSize.height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(desktop);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        desktop.setBackground(Color.DARK_GRAY);
        desktop.setLayout(null);
        JButton addButton = new JButton("+");
        addButton.setBounds(0, 0, NotesTakingWithUiApplication.ADD_BUTTON_WIDTH, NotesTakingWithUiApplication.ADD_BUTTON_HEIGHT);
        addButton.addActionListener(new AddButtonListener());
        desktop.add(addButton);

        List<Note> notes = noteService.getNotes();
        for (Note note : notes) {
            MyInternalFrame newFrame = createNoteFrame();
            newFrame.setId(note.getId().intValue());
            JTextArea textArea = getJTextArea(newFrame);
            if (textArea == null) {
                continue;
            }
            textArea.setText(note.getContent());
            newFrame.addInternalFrameListener(new MyInternalFrameClosingListener());
            newFrame.setVisible(true);
            desktop.add(newFrame);
        }
    }

    @PreDestroy
    public void onExit() {
        System.out.println("Shutting down gracefully");
        JInternalFrame[] desktopAllFrames = desktop.getAllFrames();
        System.out.println(String.format("There are %d frames in the desktop", desktopAllFrames.length));
        for (JInternalFrame desktopFrame : desktopAllFrames) {
            if (!desktopFrame.isVisible()) {
                continue;
            }
            JTextArea textArea = getJTextArea(desktopFrame);
            if (textArea != null) {
                MyInternalFrame castedFrame = (MyInternalFrame) desktopFrame;
                String text = textArea.getText();
                Note note = new Note();
                Long id = Long.valueOf(castedFrame.getId());
                boolean exist = noteService.existsById(id);
                if (exist) {
                    note.setId(id);
                    note.setContent(text);
                    noteService.updateNote(note);
                } else if (!"".equals(textArea.getText())) { // don't create if content is empty
                    note.setContent(text);
                    noteService.createNote(note);
                }
            }
        }
    }

    private JTextArea getJTextArea(JInternalFrame internalFrame) {
        MyInternalFrame castedFrame = (MyInternalFrame) internalFrame;
        Component[] components = castedFrame.getContentPane().getComponents();
        for (Component component : components) {
            if (component instanceof JScrollPane) {
                Component[] viewPortComponents = ((JScrollPane) component).getViewport().getComponents();
                for (Component c : viewPortComponents) {
                    if (c instanceof JTextArea) {
                        return (JTextArea) c;
                    }
                }
            }
        }
        return null;
    }

    class AddButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            MyInternalFrame newFrame = createNoteFrame();
            newFrame.addInternalFrameListener(new MyInternalFrameClosingListener());
            newFrame.setVisible(true);
            desktop.add(newFrame);
            try {
                newFrame.setSelected(true);
            } catch (java.beans.PropertyVetoException ex) {}
        }
    }

    class MyInternalFrameClosingListener extends InternalFrameAdapter {
        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            JPanel panel = new JPanel();
            panel.setSize(new Dimension(250, 100));
            panel.setLayout(null);
            int response = JOptionPane.showConfirmDialog(
                    panel,
                    "Are you sure you want to delete this note?",
                    "Confirmation",
                    JOptionPane.YES_NO_OPTION);
            if (response == 0) { // yes
                MyInternalFrame castedFrame = (MyInternalFrame) e.getInternalFrame();
                Long id = Long.valueOf(castedFrame.getId());
                if (id != 0) { // if id is zero that means this isn't stored to DB yet, do nothing
                    noteService.deleteNote(id);
                }
                e.getInternalFrame().setVisible(false);
            }
        }
    }

    private MyInternalFrame createNoteFrame() {
        MyInternalFrame newFrame = new MyInternalFrame();
        newFrame.setResizable(false);
        newFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JTextArea textArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(textArea);
        newFrame.add(scrollPane);

        return newFrame;
    }

    static class MyInternalFrame extends JInternalFrame {
        private static int openFrameCount = 0;
        private static final int xOffset = 30, yOffset = 30;
        private int id;

        public MyInternalFrame() {
            super("Note #" + (++openFrameCount),
                    false, //resizable
                    true, //closable
                    false, //maximizable
                    false);//iconifiable

            //...Then set the window size or call pack...
            setSize(350, 200);

            //Set the window's location.
            //int x = xOffset * openFrameCount, y = yOffset * openFrameCount;
            int x = xOffset * openFrameCount + ADD_BUTTON_HEIGHT, y = yOffset * openFrameCount;
            setLocation(x, y);
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}