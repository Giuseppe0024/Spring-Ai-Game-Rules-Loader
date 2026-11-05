package net.bcsoft.vince.game_rules_loader;

import net.bcsoft.vince.game_rules_loader.dto.GameTitle;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Capitolo4Tests {

    @Test
    void titoloNormalizzato() {
        GameTitle gameTitle = new GameTitle("Burger Battle");
        assertThat(gameTitle.getNormalizedTitle()).isEqualTo("burger_battle");
    }

    @Test
    void metadatoTitolo() {
        Document documento = new Document("Ogni giocatore pesca 5 carte.");
        documento.getMetadata().put("gameTitle", "burger_battle");

        assertThat(documento.getMetadata().get("gameTitle")).isEqualTo("burger_battle");
    }

    @Test
    void divisioneInChunk() {
        Document documento = new Document("Ogni giocatore pesca 5 carte.");
        List<Document> chunks = new TokenTextSplitter().apply(List.of(documento));

        assertThat(chunks).isNotEmpty();
    }
}