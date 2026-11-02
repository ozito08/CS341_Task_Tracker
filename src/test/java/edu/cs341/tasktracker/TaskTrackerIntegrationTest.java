package edu.cs341.tasktracker;

import edu.cs341.tasktracker.db.Database;
import edu.cs341.tasktracker.model.*;
import edu.cs341.tasktracker.repository.*;
import edu.cs341.tasktracker.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class TaskTrackerIntegrationTest {
    @TempDir Path temp;
    private AuthService auth;
    private TaskService tasks;
    private Database db;
    @BeforeEach void setup() throws Exception {
        db=new Database("jdbc:sqlite:"+temp.resolve("integration.db"));
        db.initialize();
        UserRepository users=new UserRepository(db);
        auth=new AuthService(users,new PasswordHasher());
        tasks=new TaskService(new TaskRepository(db),users);
    }
    @Test void registerLoginDuplicateAndInvalidCredential() throws Exception {
        User u=auth.register("alice","Password123".toCharArray());
        assertEquals(u.getId(),auth.login("ALICE","Password123".toCharArray()).getId());
        assertThrows(IllegalArgumentException.class,()->auth.register("Alice","Password123".toCharArray()));
        assertThrows(IllegalArgumentException.class,()->auth.login("alice","WrongPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class,()->auth.register("x","tooShort".toCharArray()));
    }
    @Test void createAssignmentVisibilityEditToggleAndDelete() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        User bob=auth.register("bob","Password123".toCharArray());
        User carol=auth.register("carol","Password123".toCharArray());
        TaskItem created=tasks.create(alice,bob.getId(),"Write tests","Integration tests","HIGH","2026-12-01");
        assertEquals(1,tasks.list(alice).size());
        assertEquals(1,tasks.list(bob).size());
        assertEquals(0,tasks.list(carol).size());
        assertThrows(SecurityException.class,()->tasks.toggle(carol,created.getId()));
        assertThrows(SecurityException.class,()->tasks.edit(bob,created.getId(),bob.getId(),"Changed","","LOW",null));
        assertThrows(SecurityException.class,()->tasks.delete(bob,created.getId()));
        tasks.toggle(bob,created.getId());
        assertTrue(tasks.list(alice).get(0).isCompleted());
        tasks.edit(alice,created.getId(),carol.getId(),"Updated","Details","MEDIUM",null);
        assertEquals(0,tasks.list(bob).size());
        assertEquals(1,tasks.list(carol).size());
        assertEquals("Updated",tasks.list(carol).get(0).getTitle());
        tasks.delete(alice,created.getId());
        assertTrue(tasks.list(alice).isEmpty());
    }
    @Test void validationAndForeignKey() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        assertThrows(IllegalArgumentException.class,()->tasks.create(alice,999,"Title","","LOW",null));
        assertThrows(IllegalArgumentException.class,()->tasks.create(alice,alice.getId(),"  ","","LOW",null));
        assertThrows(IllegalArgumentException.class,()->tasks.create(alice,alice.getId(),"Title","","URGENT",null));
        assertThrows(IllegalArgumentException.class,()->tasks.create(alice,alice.getId(),"Title","","HIGH","31-12-2026"));
    }
    @Test void schemaInitializationIsIdempotent() throws Exception {assertDoesNotThrow(()->db.initialize());}
    @Test void taskCanBeReopenedByAssigneeAndUnrelatedEditsFail() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        User bob=auth.register("bob","Password123".toCharArray());
        User carol=auth.register("carol","Password123".toCharArray());
        TaskItem t=tasks.create(alice,bob.getId(),"Double toggle","","LOW",null);
        tasks.toggle(bob,t.getId());
        assertTrue(tasks.list(alice).get(0).isCompleted());
        tasks.toggle(bob,t.getId());
        assertFalse(tasks.list(bob).get(0).isCompleted());
        assertThrows(SecurityException.class,()->tasks.edit(carol,t.getId(),carol.getId(),"Illegal edit","","HIGH",null));
        assertThrows(SecurityException.class,()->tasks.delete(carol,t.getId()));
        assertEquals("Double toggle",tasks.list(alice).get(0).getTitle());
    }
    @Test void persistenceAcrossDatabaseRecreation() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        tasks.create(alice,alice.getId(),"Persistent","saved","HIGH","2026-12-31");
        Database reopened=new Database("jdbc:sqlite:"+temp.resolve("integration.db"));
        reopened.initialize();
        UserRepository userRepo=new UserRepository(reopened);
        User signedIn=new AuthService(userRepo,new PasswordHasher()).login("alice","Password123".toCharArray());
        TaskService restored=new TaskService(new TaskRepository(reopened),userRepo);
        assertEquals(1,restored.list(signedIn).size());
        assertEquals("Persistent",restored.list(signedIn).get(0).getTitle());
    }
    @Test void sqlInjectionTextIsStoredLiterally() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        String literal="'); DROP TABLE users; --";
        tasks.create(alice,alice.getId(),literal,"Special \"quotes\" and 'apostrophe'","HIGH",null);
        assertEquals(literal,tasks.list(alice).get(0).getTitle());
        assertEquals(alice.getId(),auth.login("alice","Password123".toCharArray()).getId());
    }
    @Test void dateAndTitleBoundaryValidation() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        assertThrows(IllegalArgumentException.class,()->tasks.create(alice,alice.getId(),"x".repeat(121),"","LOW",null));
        assertThrows(IllegalArgumentException.class,()->tasks.create(alice,alice.getId(),"Title","","LOW","2026-02-30"));
        assertDoesNotThrow(()->tasks.create(alice,alice.getId(),"x".repeat(120),"","LOW","2028-02-29"));
        assertEquals(1,tasks.list(alice).size());
    }
    @Test void foreignKeyConstraintsAreEnforcedPerJdbcConnection() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        try(java.sql.Connection c=db.connect();java.sql.PreparedStatement ps=c.prepareStatement(
                "INSERT INTO tasks(creator_id,assignee_id,title) VALUES(?,?,?)")) {
            ps.setLong(1,alice.getId());ps.setLong(2,999999);ps.setString(3,"Invalid assignee");
            assertThrows(SQLException.class,ps::executeUpdate);
        }
    }
    @Test void activeSessionControllerIsInvalidatedOnLogoutOrFailedLogin() throws Exception {
        UserRepository users=new UserRepository(db);
        edu.cs341.tasktracker.controller.TaskTrackerController controller=
            new edu.cs341.tasktracker.controller.TaskTrackerController(
              new AuthService(users,new PasswordHasher()),new TaskService(new TaskRepository(db),users),users);
        assertThrows(SecurityException.class,controller::tasks);
        controller.register("alice","Password123".toCharArray());
        assertEquals("alice",controller.currentUser().getUsername());
        assertThrows(IllegalArgumentException.class,()->controller.login("alice","WrongPassword".toCharArray()));
        assertThrows(SecurityException.class,controller::tasks);
        controller.login("alice","Password123".toCharArray());
        controller.logout();
        assertThrows(SecurityException.class,controller::currentUser);
    }

    @Test void unauthorizedDirectSqlWritesCannotBypassRepository() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        User bob=auth.register("bob","Password123".toCharArray());
        User carol=auth.register("carol","Password123".toCharArray());
        TaskItem t=tasks.create(alice,bob.getId(),"Protected task","","MEDIUM",null);
        TaskRepository repository=new TaskRepository(db);
        assertEquals(0,repository.edit(t.getId(),carol.getId(),carol.getId(),"Unauthorized","","HIGH",null));
        assertEquals(0,repository.toggleCompleted(t.getId(),carol.getId()));
        assertEquals(0,repository.delete(t.getId(),carol.getId()));
        assertEquals("Protected task",tasks.list(alice).get(0).getTitle());
    }
    @Test void assignmentToSelfAndToOtherUserAreBothSupported() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        User bob=auth.register("bob","Password123".toCharArray());
        tasks.create(alice,alice.getId(),"Self assigned","","LOW",null);
        tasks.create(alice,bob.getId(),"Assigned to Bob","","HIGH",null);
        assertEquals(2,tasks.list(alice).size());
        assertEquals(1,tasks.list(bob).size());
    }
    @Test void rejectedAccountsAndDatesDoNotCreateRows() throws Exception {
        assertThrows(IllegalArgumentException.class,()->auth.register("invalid user","Password123".toCharArray()));
        assertThrows(IllegalArgumentException.class,()->auth.register("ab","Password123".toCharArray()));
        User alice=auth.register("alice","Password123".toCharArray());
        assertThrows(IllegalArgumentException.class,()->tasks.create(alice,alice.getId(),"Bad date","","LOW","2026-13-01"));
        assertEquals(0,tasks.list(alice).size());
    }
    @Test void creatorCanReassignExistingTaskAndAssigneeDoesNotGainDeletePermission() throws Exception {
        User alice=auth.register("alice","Password123".toCharArray());
        User bob=auth.register("bob","Password123".toCharArray());
        TaskItem task=tasks.create(alice,alice.getId(),"Transferable","","LOW",null);
        tasks.edit(alice,task.getId(),bob.getId(),"Transferred","","HIGH","2026-11-30");
        assertEquals(1,tasks.list(bob).size());
        assertThrows(SecurityException.class,()->tasks.delete(bob,task.getId()));
        assertEquals("2026-11-30",tasks.list(bob).get(0).getDueDate());
    }

}
