package com.trackthebird.twittersearch;

/*	By Naveenu	*/

import com.google.gson.annotations.SerializedName;

// Twitter Search Results
public class TSSearchResults {

	@SerializedName("statuses")
	private TSSearches statuses;

	@SerializedName("search_metadata")
	private TSSearchMetadata metadata;


	public TSSearches getStatuses() {
		return statuses;
	}

	public void setStatuses(TSSearches statuses) {
		this.statuses = statuses;
	}

	public TSSearchMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(TSSearchMetadata metadata) {
		this.metadata = metadata;
	}
}
