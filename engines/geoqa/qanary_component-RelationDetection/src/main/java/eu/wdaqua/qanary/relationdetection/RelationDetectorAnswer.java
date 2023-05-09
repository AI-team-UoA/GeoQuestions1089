package eu.wdaqua.qanary.relationdetection;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * represents the answer of a {@link RelationDetector} processing
 * 
 * @author AnBo
 *
 */
public class RelationDetectorAnswer {

	private boolean foundRelation;
	public String relationStringInQuestion = "";
	private GeospatialRelation geospatialRelation;
	private int indexBegin = -1;
	
	/**
	 * the identifier for the RDF entity representing the geospatial relation
	 */
	private URI geospatialRelationIdentifier;

	public RelationDetectorAnswer(boolean foundRelation, GeospatialRelation geospatialRelation,int index, String relationString) throws URISyntaxException {
		super();
		this.foundRelation = foundRelation;
		this.geospatialRelation = geospatialRelation;
		this.indexBegin = index;
		this.geospatialRelationIdentifier = new URI(geospatialRelation.getURI());
		this.relationStringInQuestion = relationString;
		//this.geospatialRelationIdentifier = new URI(geospatialRelation.getURI()+this.getGeospatialRelation().hashCode());
	}

	/**
	 * @return the foundRelation
	 */
	public boolean isFoundRelation() {
		return foundRelation;
	}

	/**
	 * @return the geospatialRelation
	 */
	public GeospatialRelation getGeospatialRelation() {
		return geospatialRelation;
	}
	
	/**
	 * 
	 */
	public URI getGeospatialRelationIdentifier(){
		return this.geospatialRelationIdentifier;
	}
	
	public int getIndexBegin(){
		return indexBegin;
	}

	/**
	 * check if the objects are equal
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(RelationDetectorAnswer other) {
		if(this.isFoundRelation() == other.isFoundRelation() && this.getGeospatialRelation().compareTo(other.getGeospatialRelation()) == 0){
			return true;
		} else {
			return false;
		}
	}
}
