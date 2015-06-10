package com.projecttango.experiments.javapointcloud;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.opengl.Matrix;

import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangoutils.ModelMatCalculator;

public class SharedData {
	private FloatBuffer currentXyzIjCoord = FloatBuffer.allocate(1000);
	private FloatBuffer prevXyzIjCoord;
	private ModelMatCalculator modelMatCalculator = new ModelMatCalculator();
	private TangoPoseData currentPose = new TangoPoseData();
	private TangoPoseData universalPose;
	private TangoPoseData prevPose;
	private ArrayList<FloatBuffer> lastKPointClouds = new ArrayList<FloatBuffer>();
	private ArrayList<TangoPoseData> lastKPose = new ArrayList<TangoPoseData>();

	public void setXyzijCoord(FloatBuffer points) {
		prevXyzIjCoord = currentXyzIjCoord;
		float [] convertedArrayMatrix = modelMatCalculator.getmStart2mOpengl(currentPose.getTranslationAsFloats(), currentPose.getRotationAsFloats());

		ArrayList<float[]> triples = new ArrayList<float[]>();

		for(int a=0; a < points.capacity(); a+=3){
			float[] triple = new float[4];
			triple[0] = points.get(a);
			triple[1] = points.get(a+1);
			triple[2] = points.get(a+2);
			triple[3] = 1;
			triples.add(triple);
		}
		
		currentXyzIjCoord = FloatBuffer.allocate(points.capacity()*2);
		
		for (int b=0; b<triples.size();b++){
			float[] temp = new float[4];
			Matrix.multiplyMV(temp, 0, convertedArrayMatrix, 0, triples.get(b), 0);
			currentXyzIjCoord.put(temp);
		}


	}

	public FloatBuffer getXyzijCoord() {
		return currentXyzIjCoord;
	}

	public void setPose(TangoPoseData poseData) {
		prevPose = currentPose;
		currentPose = poseData;
		printTangoPoseData(currentPose);
	}

	public void setUniversalPose(TangoPoseData poseData) {
		universalPose = poseData;
	}

	public TangoPoseData getPose() {
		System.out.println("Getting currentPose = " + currentPose);
		return currentPose;
	}

	public void printTangoPoseData(TangoPoseData poseData) {
		System.out.println("Current rotation: " + poseData.rotation);
		System.out.println("Current translation: " + poseData.translation);
	}

	public void updateLastKPointClouds(int k) {
		if (lastKPointClouds.size() ==  k) {
			lastKPointClouds.remove(0);
		}
		lastKPointClouds.add(getXyzijCoord());
	}

	public ArrayList<FloatBuffer> getLastKPointClouds() {
		return lastKPointClouds;
	}

	public void updateLastKPose(int k, TangoPoseData poseData) {
		if (lastKPose.size() ==  k) {
			lastKPose.remove(0);
		}
		lastKPose.add(poseData);
	}

	public ArrayList<TangoPoseData> getLastKPose() {
		return lastKPose;
	}

	public boolean isXyzSameAsPrevious() {
		if (prevXyzIjCoord == currentXyzIjCoord) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isPoseSameAsPrevious() {
		if (prevPose == currentPose) {
			return false;
		} else {
			return true;
		}
	}
}
