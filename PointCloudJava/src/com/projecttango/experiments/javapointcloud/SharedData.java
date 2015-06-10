package com.projecttango.experiments.javapointcloud;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.opengl.Matrix;

import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangoutils.ModelMatCalculator;

public class SharedData {
	private float[] currentXyzIjCoord = new float[16];
	private float[] prevXyzIjCoord;
	private ModelMatCalculator modelMatCalculator = new ModelMatCalculator();
	private TangoPoseData currentPose = new TangoPoseData();
	private TangoPoseData universalPose;
	private TangoPoseData prevPose;
	private ArrayList<FloatBuffer> lastKPointClouds = new ArrayList<FloatBuffer>();
	private ArrayList<TangoPoseData> lastKPose = new ArrayList<TangoPoseData>();

	public void setXyzijCoord(FloatBuffer points) {
		float [] convertedMatrix = modelMatCalculator.getmStart2mOpengl(currentPose.getRotationAsFloats());
		//mulitply points by convertedMatrix
		float[] triples = new float[4];
	      points.flip();
	      points.get(triples);
		Matrix.multiplyMV(currentXyzIjCoord, 0, triples, 0, convertedMatrix, 0);

		prevXyzIjCoord = currentXyzIjCoord;
		System.out.println("Set currentXyzIjCoord = " + currentXyzIjCoord);
	}

	public float []getXyzijCoord() {
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

	public void updateLastKPointClouds(int k, TangoXyzIjData xyzIj) {
		if (lastKPointClouds.size() ==  k) {
			lastKPointClouds.remove(0);
		}
		lastKPointClouds.add(xyzIj.xyz);
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
