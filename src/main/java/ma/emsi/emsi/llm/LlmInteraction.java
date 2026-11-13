package ma.emsi.emsi.llm;

/**
 * Résultat d'une interaction avec le LLM : le JSON envoyé, le JSON reçu,
 * et le texte de la réponse à afficher à l'utilisateur.
 */
public record LlmInteraction(String questionJson, String reponseJson, String reponseExtraite) {
}