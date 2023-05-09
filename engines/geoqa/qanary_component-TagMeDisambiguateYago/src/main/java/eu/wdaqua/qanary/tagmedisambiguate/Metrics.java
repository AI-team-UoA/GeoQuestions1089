package eu.wdaqua.qanary.tagmedisambiguate;

public class Metrics {
	private int time_tokenize;
	private int time_spot;
	private int time_disambiguation;
	private int time_evaluation;
	
	public Metrics(int time_tokenize, int time_spot, int time_disambiguation, int time_evaluation) {
		super();
		this.time_tokenize = time_tokenize;
		this.time_spot = time_spot;
		this.time_disambiguation = time_disambiguation;
		this.time_evaluation = time_evaluation;
	}

	public int getTime_tokenize() {
		return time_tokenize;
	}

	public void setTime_tokenize(int time_tokenize) {
		this.time_tokenize = time_tokenize;
	}

	public int getTime_spot() {
		return time_spot;
	}

	public void setTime_spot(int time_spot) {
		this.time_spot = time_spot;
	}

	public int getTime_disambiguation() {
		return time_disambiguation;
	}

	public void setTime_disambiguation(int time_disambiguation) {
		this.time_disambiguation = time_disambiguation;
	}

	public int getTime_evaluation() {
		return time_evaluation;
	}

	public void setTime_evaluation(int time_evaluation) {
		this.time_evaluation = time_evaluation;
	}

	@Override
	public String toString() {
		return "Metrics [time_tokenize=" + time_tokenize + ", time_spot=" + time_spot + ", time_disambiguation="
				+ time_disambiguation + ", time_evaluation=" + time_evaluation + "]";
	}
	
	
	
	
}
