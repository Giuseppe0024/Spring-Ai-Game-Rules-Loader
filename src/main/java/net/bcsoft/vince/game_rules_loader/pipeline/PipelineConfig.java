package net.bcsoft.vince.game_rules_loader.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import net.bcsoft.vince.game_rules_loader.dto.GameTitle;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Configuration
public class PipelineConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(PipelineConfig.class);

	// AVVIO della PIPELINE
	// catalog.lookup -> restituisce la funzione composta definita in
	// spring.cloud.function.definition
	// run() -> fa partire il controllo della cartella
	@Bean
	ApplicationRunner go(FunctionCatalog catalog) {
		Runnable composedFunction = catalog.lookup(null);
		return args -> {
			composedFunction.run();
		};
	}

	// Dai byte passa a Document
	// fileSupplier emette un byte[] per ogni file
	// Tika ne estrae il testo
	@Bean
	Function<Flux<byte[]>, Flux<Document>> documentReader() {
		return resourceFlux -> resourceFlux
				.map(fileBytes -> new TikaDocumentReader(new ByteArrayResource(fileBytes)).get().getFirst())
				.subscribeOn(Schedulers.boundedElastic());
	}

	// splitter -> dividere il test in chuck
	// entra un documento e viene fuori come lista di documenti più piccoli
	// divisi per numero di token, adatti al calcolo degli embedding
	@Bean
	Function<Flux<Document>, Flux<List<Document>>> splitter() {
		var splitter = new TokenTextSplitter();
		return documentFlux -> documentFlux.map(incoming -> splitter.apply(List.of(incoming)))
				.subscribeOn(Schedulers.boundedElastic());
	}

	// CHIEDERE all'LLM il nome del gioco
	@Value("classpath:/promptTemplates/nameOfTheGame.st")
	Resource nameOfTheGameTemplateResource;

	// LOGICA:
	// 1 - Prende solo il primo chunk, dove di solito compare il titolo del gioco
	// 2 - Lo inserisce nel template e chiama openai
	// 3 - Converte la risposta in GameTitle (Spring aggiunge al prompt le
	// istruzioni sul formato Json)
	// 4 - Se la risposta è UNKNOWN restituisce una lista vuota e il file viene
	// scartato
	// 5 - Altrimenti aggiunge gameTitle ai metadati di TUTTI i chunk
	@Bean
	Function<Flux<List<Document>>, Flux<List<Document>>> titleDeterminer(ChatClient.Builder chatClientBuilder) {
		var chatClient = chatClientBuilder.build();

		return documentListFlux -> documentListFlux.map(documents -> {
			if (!documents.isEmpty()) {
				var firstDocument = documents.getFirst();

				var gameTitle = chatClient.prompt().user(userSpec -> userSpec.text(nameOfTheGameTemplateResource)
						.param("document", firstDocument.getText())).call().entity(GameTitle.class);

				if (Objects.requireNonNull(gameTitle).title().equals("UNKNOWN")) {
					LOGGER.warn("Impossibile determinare il nome del gioco");
					documents = Collections.emptyList();
					return documents;
				}

				LOGGER.info("Il nome del gioco è " + gameTitle.title());
				documents = documents.stream().peek(document -> {
					document.getMetadata().put("gameTitle", gameTitle.getNormalizedTitle());
				}).toList();

			}
			return documents;

		});
	}

	// Scrivere su Qdrant
	@Bean
	Consumer<Flux<List<Document>>> vectorStoreConsumer(VectorStore vectorStore) {
		return documentFlux -> documentFlux.doOnNext(documents -> {
			if (!documents.isEmpty()) {
				var docCount = documents.size();
				LOGGER.info("Scrittura di {} documenti nel vector store", docCount);

				//calcola gli embedding con OpenAi e salva vettori e metadati su QDrant
				vectorStore.accept(documents);

				LOGGER.info("{} documenti sono stati scritti nel vector store", docCount);
			}
		}).subscribe(); //indispensabile per salvare i dati
	}

}
