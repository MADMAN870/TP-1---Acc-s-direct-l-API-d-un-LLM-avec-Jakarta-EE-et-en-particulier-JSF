package ma.emsi.emsi.llm;

/** Exception levée quand la requête vers l'API du LLM échoue. */
public class RequeteException extends Exception {

    private final String jsonReponse;

    public RequeteException(String message) {
        super(message);
        this.jsonReponse = null;
    }

    public RequeteException(String message, String jsonReponse) {
        super(message);
        this.jsonReponse = jsonReponse;
    }

    public RequeteException(String message, Throwable cause) {
        super(message, cause);
        this.jsonReponse = null;
    }

    public String getJsonReponse() {
        return jsonReponse;
    }
}