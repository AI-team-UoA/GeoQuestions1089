package eu.wdaqua.qanary.tagmedisambiguate;

public class NedAnnotation {
	private String spot;
	private int id;
	private String title;
	private int start;
	private int end;
	private double rho;
	
	@Override
	public String toString() {
		return "NedAnnotation [spot=" + spot + ", id=" + id + ", title=" + title + ", start=" + start + ", end=" + end
				+ ", rho=" + rho + "]";
	}

	public NedAnnotation(String spot, int id, String title, int start, int end, double rho) {
		super();
		this.spot = spot;
		this.id = id;
		this.title = title;
		this.start = start;
		this.end = end;
		this.rho = rho;
	}

	public String getSpot() {
		return spot;
	}

	public void setSpot(String spot) {
		this.spot = spot;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public double getRho() {
		return rho;
	}

	public void setRho(double rho) {
		this.rho = rho;
	}
	
	
	
}
