package com.projecttango.experiments.javapointcloud;

public class Voxel {
	
	private float sdf;
	private int[] colorRgb = new int[3];
	private int weight;
	
	public float getSdf() {
		return sdf;
	}
	
	public void setSdf(float _sdf) {
		sdf = _sdf;
	}

	public int getWeight() {
		return weight;
	}
	
	public void setWeight(int _weight) {
		weight = _weight;
	}
	
	public int[] getColorRgb() {
		return colorRgb;
	}

	public void setColorRgb(int[] _colorRgb) {
		colorRgb = _colorRgb;
	}
	
	public int getColorRgbAt(int pos) {
		if (pos >= 0 && pos <= 3) {
			return colorRgb[pos];
		} else {
			throw new IndexOutOfBoundsException ("Array Index" + pos + " out of bounds");
		}
	}

	public void setColorRgbAt(int _colorRgb, int pos) {
		if (pos <= 0 || pos >= 3) {
			throw new IndexOutOfBoundsException ("Array Index" + pos + " out of bounds");
		} else if (_colorRgb < 0 || _colorRgb > 255) {
			throw new IndexOutOfBoundsException("colorRgb: " + _colorRgb + " out of bounds");
		} else {
			colorRgb[pos] = _colorRgb;
		}
	}
}