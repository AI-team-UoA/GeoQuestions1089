package eu.wdaqua.qanary.tagmedisambiguate;

import java.util.ArrayList;

public class TagMeResponse {
	
	private Metrics metrics;
	private ArrayList<NedAnnotation> annotations;
	
	public TagMeResponse(Metrics metrics, ArrayList<NedAnnotation> annotations) {
		super();
		this.metrics = metrics;
		this.annotations = annotations;
	}

	@Override
	public String toString() {
		return "Response [metrics=" + metrics + ", annotations=" + annotations + "]";
	}

	public Metrics getMetrics() {
		return metrics;
	}

	public void setMetrics(Metrics metrics) {
		this.metrics = metrics;
	}

	public ArrayList<NedAnnotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(ArrayList<NedAnnotation> annotations) {
		this.annotations = annotations;
	}
	
	
	
	
}
