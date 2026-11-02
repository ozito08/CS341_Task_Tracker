package edu.cs341.tasktracker.ui;

import edu.cs341.tasktracker.model.*;
import edu.cs341.tasktracker.controller.TaskTrackerController;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

@SuppressWarnings("serial") // Swing forms are not persisted through Java serialization.
public final class MainFrame extends JFrame {
    private final TaskTrackerController controller;
    private final CardLayout screens = new CardLayout();
    private final JPanel body = new JPanel(screens);
    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final TaskModel model = new TaskModel();
    private final JTable taskTable = new JTable(model);
    private final JLabel welcome = new JLabel();
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All", "Open", "Completed", "High priority"});

    public MainFrame(TaskTrackerController controller) {
        super("CS341 | Collaborative Task Tracker");
        this.controller=controller;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900,550));
        setSize(new Dimension(1100,650));
        setLocationRelativeTo(null);
        createLoginScreen();createTaskScreen();
        setContentPane(body);screens.show(body,"login");
    }
    private void createLoginScreen() {
        JPanel login = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(0,1,6,6));
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Sign in / register"),
                BorderFactory.createEmptyBorder(18,22,20,22)));
        JLabel heading=new JLabel("TASK TRACKER",SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD,23f));
        form.add(heading);form.add(new JLabel("Username"));form.add(username);
        form.add(new JLabel("Password (min. 8 characters for registration)"));form.add(password);
        JButton signIn=new JButton("Log in");JButton signUp=new JButton("Create account");
        signIn.addActionListener(e->authenticate(false));signUp.addActionListener(e->authenticate(true));
        password.addActionListener(e->authenticate(false));
        form.add(signIn);form.add(signUp);
        login.add(form);body.add(login,"login");
    }
    private void authenticate(boolean register) {
        char[] secret=password.getPassword();
        try {
            User current=register?controller.register(username.getText(),secret):controller.login(username.getText(),secret);
            password.setText("");
            welcome.setText("Signed in: "+current.getUsername());
            refresh();screens.show(body,"tasks");
            if(register)JOptionPane.showMessageDialog(this,"Account created successfully.");
        }catch(Exception ex){error(ex);}
        finally {Arrays.fill(secret,'\0');}
    }
    private void createTaskScreen() {
        JPanel content=new JPanel(new BorderLayout(10,10));
        content.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        JPanel top=new JPanel(new BorderLayout());
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD,16f));
        JButton logout=new JButton("Log out");
        logout.addActionListener(e->{controller.logout();model.setRows(Collections.emptyList());
            taskTable.clearSelection(); filter.setSelectedIndex(0); password.setText(""); screens.show(body,"login");});
        top.add(welcome,BorderLayout.WEST);top.add(logout,BorderLayout.EAST);
        content.add(top,BorderLayout.NORTH);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.setRowHeight(26);
        taskTable.setAutoCreateRowSorter(true);
        content.add(new JScrollPane(taskTable),BorderLayout.CENTER);
        JPanel commands=new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));
        JButton add=new JButton("New task");add.addActionListener(e->showEditor(null));
        JButton edit=new JButton("Edit task");edit.addActionListener(e->{TaskItem t=selected();if(t!=null)showEditor(t);});
        JButton toggle=new JButton("Toggle complete");toggle.addActionListener(e->action("toggle"));
        JButton delete=new JButton("Delete");delete.addActionListener(e->action("delete"));
        JButton refresh=new JButton("Refresh");refresh.addActionListener(e->refresh());
        filter.addActionListener(e->applyFilter());
        commands.add(add);commands.add(edit);commands.add(toggle);commands.add(delete);commands.add(refresh);
        commands.add(new JLabel("Filter:"));commands.add(filter);
        JPanel bottom=new JPanel(new BorderLayout());bottom.add(commands,BorderLayout.NORTH);
        bottom.add(new JLabel("Creator: edit/delete. Creator and assignee: toggle complete. Visible: created or assigned tasks."),BorderLayout.SOUTH);
        content.add(bottom,BorderLayout.SOUTH);
        body.add(content,"tasks");
    }
    private void applyFilter(){
        if(taskTable.getRowSorter()==null)return;
        @SuppressWarnings("unchecked") TableRowSorter<TaskModel> sorter=(TableRowSorter<TaskModel>)taskTable.getRowSorter();
        String f=(String)filter.getSelectedItem();
        if("Open".equals(f))sorter.setRowFilter(RowFilter.regexFilter("^Open$",4));
        else if("Completed".equals(f))sorter.setRowFilter(RowFilter.regexFilter("^Done$",4));
        else if("High priority".equals(f))sorter.setRowFilter(RowFilter.regexFilter("^HIGH$",5));
        else sorter.setRowFilter(null);
    }
    private TaskItem selected(){
        int row=taskTable.getSelectedRow();
        if(row<0){JOptionPane.showMessageDialog(this,"Select a task first.");return null;}
        return model.getTask(taskTable.convertRowIndexToModel(row));
    }
    private void action(String type){
        TaskItem t=selected();if(t==null)return;
        try{
            if("toggle".equals(type))controller.toggle(t.getId());
            else if(JOptionPane.showConfirmDialog(this,"Delete selected task?","Confirm delete",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                controller.delete(t.getId());
            refresh();
        }catch(Exception e){error(e);}
    }
    private void refresh(){
        try{model.setRows(controller.tasks());applyFilter();}
        catch(Exception e){error(e);}
    }
    private void showEditor(TaskItem existing){
        if(existing!=null && existing.getCreatorId()!=controller.currentUser().getId()){
            error(new SecurityException("Only the task creator can edit."));return;
        }
        try {
            List<User> people=controller.allUsers();
            JComboBox<User> assignee=new JComboBox<>(people.toArray(new User[0]));
            for(User person:people)if(person.getId()==(existing==null?controller.currentUser().getId():existing.getAssigneeId()))assignee.setSelectedItem(person);
            JTextField title=new JTextField(existing==null?"":existing.getTitle(),25);
            JTextArea description=new JTextArea(existing==null?"":existing.getDescription(),4,25);
            description.setLineWrap(true);description.setWrapStyleWord(true);
            JComboBox<String> priority=new JComboBox<>(new String[]{"LOW","MEDIUM","HIGH"});
            priority.setSelectedItem(existing==null?"MEDIUM":existing.getPriority());
            JTextField dueDate=new JTextField(existing==null||existing.getDueDate()==null?"":existing.getDueDate());
            JPanel form=new JPanel(new GridLayout(0,1,4,3));
            form.add(new JLabel("Title (required)"));form.add(title);
            form.add(new JLabel("Description"));form.add(new JScrollPane(description));
            form.add(new JLabel("Assigned to"));form.add(assignee);
            form.add(new JLabel("Priority"));form.add(priority);
            form.add(new JLabel("Deadline (YYYY-MM-DD, optional)"));form.add(dueDate);
            if(JOptionPane.showConfirmDialog(this,form,existing==null?"New task":"Edit task",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
                User selected=(User)assignee.getSelectedItem();
                if(selected==null)throw new IllegalArgumentException("Register a user before assigning tasks.");
                if(existing==null)controller.create(selected.getId(),title.getText(),description.getText(),
                    (String)priority.getSelectedItem(),dueDate.getText());
                else controller.edit(existing.getId(),selected.getId(),title.getText(),description.getText(),
                    (String)priority.getSelectedItem(),dueDate.getText());
                refresh();
            }
        } catch(Exception e){error(e);}
    }
    private void error(Exception e){
        JOptionPane.showMessageDialog(this,e.getMessage()==null?"Unexpected error":e.getMessage(),
            "Task Tracker",JOptionPane.ERROR_MESSAGE);
    }
    @SuppressWarnings("serial")
    private static final class TaskModel extends AbstractTableModel {
        private final String[] columns={"ID","Title","Description","Assigned ID","Status","Priority","Deadline","Created by ID"};
        private List<TaskItem> tasks=Collections.emptyList();
        void setRows(List<TaskItem> rows){tasks=new ArrayList<>(rows);fireTableDataChanged();}
        TaskItem getTask(int row){return tasks.get(row);}
        @Override public int getRowCount(){return tasks.size();}
        @Override public int getColumnCount(){return columns.length;}
        @Override public String getColumnName(int c){return columns[c];}
        @Override public Object getValueAt(int row,int col){
            TaskItem t=tasks.get(row);
            switch(col){case 0:return t.getId();case 1:return t.getTitle();case 2:return t.getDescription();
                case 3:return t.getAssigneeId();case 4:return t.isCompleted()?"Done":"Open";
                case 5:return t.getPriority();case 6:return t.getDueDate()==null?"":t.getDueDate();
                default:return t.getCreatorId();}
        }
        @Override public Class<?> getColumnClass(int col){return col==0||col==3||col==7?Long.class:String.class;}
    }
}
