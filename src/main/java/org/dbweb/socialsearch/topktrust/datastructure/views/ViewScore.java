package org.dbweb.socialsearch.topktrust.datastructure.views;

public class ViewScore {
	
	private double wscore;
	private double bscore;
	
	public ViewScore(double wscore, double bscore){
		this.setWscore(wscore);
		this.setBscore(bscore);
	}

	public void setWscore(double wscore) {
		this.wscore = wscore;
	}

	public double getWscore() {
		return wscore;
	}

	public void setBscore(double bscore) {
		this.bscore = bscore;
	}

	public double getBscore() {
		return bscore;
	}
	
	public String toString(){
		return String.format("[%.5f,%.5f]",wscore,bscore);
	}
	

}
