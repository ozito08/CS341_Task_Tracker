package edu.cs341.tasktracker;

import edu.cs341.tasktracker.db.Database;
import edu.cs341.tasktracker.controller.TaskTrackerController;
import edu.cs341.tasktracker.repository.*;
import edu.cs341.tasktracker.service.*;
import edu.cs341.tasktracker.ui.MainFrame;
import javax.swing.*;

public final class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()-> {
            try {
                String path=System.getProperty("tasktracker.db","tasks.db");
                Database db=new Database("jdbc:sqlite:"+path);
                db.initialize();
                UserRepository users=new UserRepository(db);
                TaskRepository tasks=new TaskRepository(db);
                new MainFrame(new TaskTrackerController(
                    new AuthService(users,new PasswordHasher()),new TaskService(tasks,users),users)).setVisible(true);
            } catch(Exception e) {
                JOptionPane.showMessageDialog(null,"Could not start Task Tracker: "+e.getMessage(),
                    "Startup error",JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
