package eu.wdaqua.qanary.geosparqlgenerator;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.wdaqua.qanary.geosparqlgenerator.GeoSparqlGenerator.Concept;
import eu.wdaqua.qanary.geosparqlgenerator.GeoSparqlGenerator.Entity;
import eu.wdaqua.qanary.geosparqlgenerator.GeoSparqlGenerator.Property;
import eu.wdaqua.qanary.geosparqlgenerator.GeoSparqlGenerator.SpatialRelation;

public class DependencyTreeNode {
	public DependencyTreeNode m_parent;
	public String m_name;
	public String posTag;
	public int startIndex = -1;
	public int endIndex = -1;
	public ArrayList<Concept> conceptList = new ArrayList<Concept>();
	public ArrayList<Entity> entityList = new ArrayList<Entity>();
	public ArrayList<SpatialRelation> relationList = new ArrayList<SpatialRelation>();
	public ArrayList<Property> propertyList = new ArrayList<Property>();
	public List<String> annotationsInstance = new ArrayList<String>();
	public List<String> annotationsRelations = new ArrayList<String>();
	public List<String> annotationsConcepts = new ArrayList<String>();
	public List<DependencyTreeNode> m_children;
	public boolean leafnode = false;

	public DependencyTreeNode() {}
	public DependencyTreeNode(DependencyTreeNode parent, String name,String postag) {
		this.m_parent = parent;
		this.m_name = name;
		this.posTag = postag;
		this.m_children = new ArrayList<DependencyTreeNode>();
	}
	
	public boolean isLeafNode() {
		return leafnode;
	}
	
		
	public void addAnnotationInstance(String annotation) {
		annotationsInstance.add(annotation);
	}

	public void addAnnotationRelation(String annotation) {
		annotationsRelations.add(annotation);
	}

	public void addAnnotationConcept(String annotation) {
		annotationsConcepts.add(annotation);
	}

	public DependencyTreeNode parent() {
		return this.m_parent;
	}

	public String value() {
		return this.m_name;
	}

	public String getPosTag() {
		return this.posTag;
	}

	public void mergeNodes(DependencyTreeNode tobeMergedNode) {

		this.m_name += tobeMergedNode.m_name;
		
		
		
		
		if (tobeMergedNode.m_children.size() > 0) {
			this.m_children.addAll(tobeMergedNode.m_children);
		}

	}

	public List<DependencyTreeNode> children() {
		return this.m_children;
	}

	public void addChild(DependencyTreeNode node) {
		this.m_children.add(node);
	}

	public boolean isRoot() {
		return this.m_parent == null;
	}

	public boolean isLeave() {
		return this.m_children.size() == 0;
	}

	@Override
	public String toString() {
		return this.m_name;
	}

	/**
	 * Convert this node (and its children, recursively) to JSON object.
	 * 
	 * @return JSONObject.
	 */
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		obj.put("type", this.m_name);
		obj.put("headDep", "");
		JSONArray children = new JSONArray();

		for (DependencyTreeNode child : this.m_children) {
			children.put(child.toJSON());
		}

		obj.put("children", children);

		return obj;
	}

	/**
	 * Get the minimum (CoreLabel/word) index in this subtree.
	 * 
	 * @return the minimum index in the subtree. In case no core label the min index
	 *         of the "parent" CoreLabel (design decision of where to place the
	 *         CoreLabels).
	 */
//	public Integer getMinIndex() {
//		Integer idx = (this.m_label == null ? this.m_parent.m_label : m_label)
//				.get(CoreAnnotations.IndexAnnotation.class) - 1;// :
//																// this.m_label.get(CoreAnnotations.IndexAnnotation.class);
//		for (int i = 0; i < this.m_children.size(); i++) {
//			int childIdx = this.m_children.get(i).getMinIndex();
//
//			if (childIdx < idx) {
//				idx = childIdx;
//			}
//		}
//
//		return idx;
//	}
//
//	/**
//	 * Get the maximum (CoreLabel/word) index in this subtree.
//	 * 
//	 * @return the maximum index in the subtree. In case no core label the max index
//	 *         of the "parent" CoreLabel (design decision of where to place the
//	 *         CoreLabels).
//	 */
//	public Integer getMaxIndex() {
//		Integer idx = (this.m_label == null ? this.m_parent.m_label : m_label)
//				.get(CoreAnnotations.IndexAnnotation.class) - 1;// :
//																// this.m_label.get(CoreAnnotations.IndexAnnotation.class);
//		for (int i = 0; i < this.m_children.size(); i++) {
//			int childIdx = this.m_children.get(i).getMaxIndex();
//
//			if (childIdx > idx) {
//				idx = childIdx;
//			}
//		}
//
//		return idx;
//	}

//	public IntPair getSpan() {
//		return new IntPair(this.getMinIndex(), this.getMaxIndex());
//	}
}