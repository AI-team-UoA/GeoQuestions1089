package eu.wdaqua.qanary.conceptidentifier;


public class Concept {
	private int begin;
	private int end;
	private String label;
	private String uri;

	public void setBegin(int begin) {
		this.begin = begin;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setURI(String uri){
		this.uri = uri;
	}

	public int getBegin() {
		return this.begin;
	}

	public int getEnd() {
		return this.end;
	}

	public String getURI() {
		return this.uri;
	}

	public String toString()
	{
		return "begin = "+begin+" end = "+ end+" uri = "+uri;
	}


}