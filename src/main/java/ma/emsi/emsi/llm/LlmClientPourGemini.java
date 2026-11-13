package ma.emsi.emsi.llm;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;

/**
 * Gère l'interface avec l'API de Gemini.
 * Son rôle est essentiellement de lancer une requête à chaque nouvelle
 * question qu'on veut envoyer à l'API.
 *
 * De portée dependent pour réinitialiser la conversation à chaque fois que
 * l'instance qui l'utilise est renouvelée.
 * Par exemple, si l'instance qui l'utilise est de portée View, la conversation est
 * réunitialisée à chaque fois que l'utilisateur quitte la page en cours.
 */
@Dependent
public class LlmClientPourGemini implements Serializable {
    // Clé pour l'API du LLM
    private final String key;
    // Client REST. Facilite les échanges avec une API REST.
    private Client clientRest; // Pour pouvoir le fermer
    // Représente un endpoint de serveur REST
    private final WebTarget target;
    // Remarque : éventuel problème si Client et Webtarget ne sont pas Serializable et si une passivation doit
    // avoir lieu. Mais c'est une autre histoire, liée à Jakarta et pas à l'IA...

    public LlmClientPourGemini() {
        // Récupère la clé secrète depuis une variable d'environnement
        this.key = System.getenv("GEMINI_KEY");

        // Client REST pour envoyer des requêtes
        this.clientRest = ClientBuilder.newClient();

        // Endpoint REST de l'API Gemini
        this.target = clientRest.target(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key="
                        + this.key
        );
    }
    /**
     * Envoie une requête à l'API de Gemini.
     * @param requestEntity le corps de la requête (en JSON).
     * @return réponse REST de l'API (corps en JSON).
     */
    public Response envoyerRequete(Entity requestEntity) {
        Invocation.Builder request = target.request(MediaType.APPLICATION_JSON_TYPE);
        // Envoie la requête POST au LLM
        return request.post(requestEntity);
    }

    @PreDestroy
    public void closeClient() {
        if (this.clientRest != null) {
            this.clientRest.close();
        }
    }
}

 
