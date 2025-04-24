package org.kickmyb.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kickmyb.server.account.BadCredentialsException;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.account.ServiceAccount;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.kickmyb.transfer.SignupRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = KickMyBServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@ActiveProfiles("test")
class ServiceTaskTests {


    @Autowired
    private ServiceTask serviceTask;

    @Autowired
    private ServiceAccount serviceAccount;

    @Test
    void testAjouterTacheOk() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing,
            ServiceAccount.UsernameTooShort, ServiceAccount.PasswordTooShort,
            ServiceAccount.UsernameAlreadyTaken, BadCredentialsException {

        // on crée un compte
        SignupRequest req = new SignupRequest();
        req.username = "alice";
        req.password = "Passw0rd!";
        serviceAccount.signup(req);

        // on récupère l'utilisateur
        MUser alice = serviceTask.userFromUsername("alice");

        // on crée une tâche
        AddTaskRequest addTaskRequest = new AddTaskRequest();
        addTaskRequest.name = "Tâche 1";
        addTaskRequest.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        // on ajoute la tâche à l'utilisateur
        serviceTask.addOne(addTaskRequest, alice);

        // on vérifie que la tâche a bien été ajoutée
        assertEquals(1, serviceTask.home(alice.id).size());
    }
    @Test
    void testSuppressionTacheIdCorrect() throws Exception {
        // Créer un utilisateur
        SignupRequest req = new SignupRequest();
        req.username = "alice";
        req.password = "Passw0rd!";
        serviceAccount.signup(req);

        // Récupérer l'utilisateur
        MUser alice = serviceTask.userFromUsername("alice");

        // Ajouter une tâche
        AddTaskRequest addTaskRequest = new AddTaskRequest();
        addTaskRequest.name = "Tâche 1";
        addTaskRequest.deadline = Date.from(new Date().toInstant().plusSeconds(3600));
        serviceTask.addOne(addTaskRequest, alice);

        // Vérifier que la tâche a été ajoutée
        alice = serviceTask.userFromUsername("alice"); // Re-récupérer l'utilisateur
        assertEquals(1, serviceTask.home(alice.id).size());

        // Supprimer la tâche
        long taskId = serviceTask.home(alice.id).get(0).id;
        serviceTask.delete(taskId, alice);

        // Vérifier que la tâche a été supprimée
        assertEquals(0, serviceTask.home(alice.id).size());
    }

    @Test
    void testSuppressionTacheIdIncorrect() throws Exception {
        // Créer un utilisateur
        SignupRequest req = new SignupRequest();
        req.username = "alice";
        req.password = "Passw0rd!";
        serviceAccount.signup(req);

        // Récupérer l'utilisateur
        MUser alice = serviceTask.userFromUsername("alice");

        // Tenter de supprimer une tâche avec un ID incorrect
        long invalidTaskId = 999L;
        try {
            serviceTask.delete(invalidTaskId, alice);
            fail("Une exception aurait dû être levée pour un ID incorrect");
        } catch (IllegalArgumentException e) {
            assertEquals("Task not found", e.getMessage());
        }
    }

    @Test
    void testControleAccesSuppression() throws Exception {
        // Créer Alice
        SignupRequest reqAlice = new SignupRequest();
        reqAlice.username = "alice";
        reqAlice.password = "Passw0rd!";
        serviceAccount.signup(reqAlice);

        // Récupérer Alice
        MUser alice = serviceTask.userFromUsername("alice");

        // Ajouter une tâche pour Alice
        AddTaskRequest addTaskRequest = new AddTaskRequest();
        addTaskRequest.name = "Tâche 1";
        addTaskRequest.deadline = Date.from(new Date().toInstant().plusSeconds(3600));
        serviceTask.addOne(addTaskRequest, alice);

        // Vérifier que la tâche a été ajoutée
        alice = serviceTask.userFromUsername("alice"); // Re-récupérer Alice
        assertEquals(1, serviceTask.home(alice.id).size());
        long taskId = serviceTask.home(alice.id).get(0).id;

        // Créer Bob
        SignupRequest reqBob = new SignupRequest();
        reqBob.username = "bob";
        reqBob.password = "Passw0rd!";
        serviceAccount.signup(reqBob);

        // Récupérer Bob
        MUser bob = serviceTask.userFromUsername("bob");

        // Tenter de supprimer la tâche d'Alice avec Bob
        try {
            serviceTask.delete(taskId, bob);
            assertEquals(1, serviceTask.home(alice.id).size());
            fail("Une exception aurait dû être levée pour un contrôle d'accès");
        } catch (SecurityException e) {
            assertEquals(1, serviceTask.home(alice.id).size());
            assertEquals("You do not have permission to delete this task", e.getMessage());
        }
        // Vérifier que la tâche d'Alice est toujours là
        assertEquals(1, serviceTask.home(alice.id).size());
    }


    @Test
    void testAjouterTacheNomVideKo() throws ServiceAccount.UsernameTooShort, ServiceAccount.PasswordTooShort,
            ServiceAccount.UsernameAlreadyTaken, BadCredentialsException {

        // on crée un compte
        SignupRequest req = new SignupRequest();
        req.username = "alice";
        req.password = "Passw0rd!";
        serviceAccount.signup(req);

        // on récupère l'utilisateur
        MUser alice = serviceTask.userFromUsername("alice");

        // on crée une tâche avec un nom vide
        AddTaskRequest addTaskRequest = new AddTaskRequest();
        addTaskRequest.name = "";
        addTaskRequest.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        // on essaie d'ajouter la tâche à l'utilisateur
        try{
            serviceTask.addOne(addTaskRequest, alice);
        } catch (Exception e) {
        }
        // on vérifie que la tâche n'a pas été ajoutée
        assertEquals(0, serviceTask.home(alice.id).size());
    }

    @Test
    void testAjouterTacheNomTropCourtKo() throws ServiceAccount.UsernameTooShort, ServiceAccount.PasswordTooShort,
            ServiceAccount.UsernameAlreadyTaken, BadCredentialsException {

        // on crée un compte
        SignupRequest req = new SignupRequest();
        req.username = "alice";
        req.password = "Passw0rd!";
        serviceAccount.signup(req);

        // on récupère l'utilisateur
        MUser alice = serviceTask.userFromUsername("alice");

        // on crée une tâche avec un nom trop court
        AddTaskRequest addTaskRequest = new AddTaskRequest();
        addTaskRequest.name = "t";
        addTaskRequest.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        // on essaie d'ajouter la tâche à l'utilisateur
        try{
            serviceTask.addOne(addTaskRequest, alice);
        } catch (Exception e) {
        }
        // on vérifie que la tâche n'a pas été ajoutée
        assertEquals(0, serviceTask.home(alice.id).size());
    }

    @Test
    void testAjouterTacheNomExistantKo() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing,
            ServiceAccount.UsernameTooShort, ServiceAccount.PasswordTooShort,
            ServiceAccount.UsernameAlreadyTaken, BadCredentialsException {

        // on crée un compte
        SignupRequest req = new SignupRequest();
        req.username = "alice";
        req.password = "Passw0rd!";
        serviceAccount.signup(req);

        // on récupère l'utilisateur
        MUser alice = serviceTask.userFromUsername("alice");

        // on crée 2 tâches avec le même nom
        AddTaskRequest addTaskRequest1 = new AddTaskRequest();
        AddTaskRequest addTaskRequest2 = new AddTaskRequest();
        addTaskRequest1.name = "Tâche 1";
        addTaskRequest2.name = "Tâche 1";
        addTaskRequest1.deadline = Date.from(new Date().toInstant().plusSeconds(3600));
        addTaskRequest2.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        // on ajoute la tâche 1 à l'utilisateur
        serviceTask.addOne(addTaskRequest1, alice);

        // on vérifie que la tâche a bien été ajoutée
        assertEquals(1, serviceTask.home(alice.id).size());

        // on essaie d'ajouter la tâche 2 à l'utilisateur
        try{
            serviceTask.addOne(addTaskRequest2, alice);
        } catch (Exception e) {
        }
        // on vérifie que la tâche 2 n'a pas été ajoutée
        assertEquals(1, serviceTask.home(alice.id).size());
    }
}
