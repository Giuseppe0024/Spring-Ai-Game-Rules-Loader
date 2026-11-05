# Spring-Ai-Game-Rules-Loader

Pipeline RAG (Retrieval-Augmented Generation) con Spring AI per il caricamento, chunking e salvataggio vettoriale delle regole dei giochi da tavolo.

## Tecnologie usate
- Java 25
- Spring Boot
- Spring AI (Vector Store, TokenTextSplitter, Prompt Templating)
- Gradle

## Struttura e Componenti
- **DTO**: `GameTitle` per la normalizzazione del titolo dei giochi
- **Pipeline**: `PipelineConfig` per la configurazione del flusso di ingestione dei documenti
- **Prompt Templates**: `nameOfTheGame.st`
- **Test**: `Capitolo4Tests` per la validazione di chunking e metadati
