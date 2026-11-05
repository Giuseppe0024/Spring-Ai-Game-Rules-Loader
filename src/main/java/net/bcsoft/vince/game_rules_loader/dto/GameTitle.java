package net.bcsoft.vince.game_rules_loader.dto;

public record GameTitle(String title) {

	//il titolo viene riscritto tutto in minuscolo e gli eventuali spazi sono sostituiti
	//da underscore così il valore verrà utilizzato per filtrare le ricerche
	public String getNormalizedTitle() {
		return title.toLowerCase().replace(" ", "_");
	}
}
