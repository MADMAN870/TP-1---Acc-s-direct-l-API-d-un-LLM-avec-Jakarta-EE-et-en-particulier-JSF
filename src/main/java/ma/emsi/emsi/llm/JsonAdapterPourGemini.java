package ma.emsi.emsi.llm;
import jakarta.ws.rs.client.Entity;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * Construit le JSON des requêtes pour Gemini et extrait la réponse du JSON retourné.
 * Cette classe conserve l'historique de la conversation (portée session), car l'API est sans état.
 */
@SessionScoped
public class JsonAdapterPourGemini implements Serializable {

    @Inject
    private LlmClientPourGemini llmClient;

    private String systemRole = "You are a helpful assistant.";
    /** JSON de la conversation (rôle système + questions/réponses précédentes). null = conversation pas commencée. */
    private String conversationJson;

    /** Définit le rôle système et recommence une nouvelle conversation. */
    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
        this.conversationJson = null;
    }

    /** Seule méthode publique utilisée par le backing bean. */
    public LlmInteraction envoyerRequete(String question) throws RequeteException {
        JsonObject requete = (conversationJson == null)
                ? creerRequeteJson(systemRole)
                : Json.createReader(new StringReader(conversationJson)).readObject();

        requete = ajouteMessage(requete, "user", question);
        String requeteJson = requete.toString();

        Response response;
        try {
            response = llmClient.envoyerRequete(Entity.json(requeteJson));
        } catch (Exception e) {
            throw new RequeteException("Impossible de joindre l'API : " + e.getMessage(), e);
        }

        String reponseJson = response.readEntity(String.class);
        if (response.getStatus() != 200) {
            throw new RequeteException(
                    "Erreur HTTP " + response.getStatus() + " : " + reponseJson, reponseJson);
        }

        String texte = extractReponse(reponseJson);

        // On mémorise la question ET la réponse (rôle "model") pour la prochaine requête.
        conversationJson = ajouteMessage(requete, "model", texte).toString();

        return new LlmInteraction(prettyPrinting(requeteJson), prettyPrinting(reponseJson), texte);
    }

    private JsonObject creerRequeteJson(String role) {
        return Json.createObjectBuilder()
                .add("system_instruction", Json.createObjectBuilder()
                        .add("parts", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder().add("text", role))))
                .add("contents", Json.createArrayBuilder())
                .build();
    }

    private JsonObject ajouteMessage(JsonObject requete, String role, String texte) {
        JsonObject message = Json.createObjectBuilder()
                .add("role", role)
                .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("text", texte)))
                .build();
        JsonObjectBuilder builder = Json.createObjectBuilder(requete);
        builder.add("contents", Json.createArrayBuilder(requete.getJsonArray("contents")).add(message));
        return builder.build();
    }

    private String extractReponse(String reponseJson) throws RequeteException {
        try {
            JsonObject racine = Json.createReader(new StringReader(reponseJson)).readObject();
            return racine.getJsonArray("candidates").getJsonObject(0)
                    .getJsonObject("content").getJsonArray("parts")
                    .getJsonObject(0).getString("text");
        } catch (Exception e) {
            throw new RequeteException("Réponse JSON inattendue : " + reponseJson, reponseJson);
        }
    }

    private String prettyPrinting(String json) {
        JsonObject objet = Json.createReader(new StringReader(json)).readObject();
        StringWriter sw = new StringWriter();
        Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true))
                .createWriter(sw).write(objet);
        return sw.toString();
    }
}