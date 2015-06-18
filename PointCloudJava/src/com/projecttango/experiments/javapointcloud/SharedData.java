package com.projecttango.experiments.javapointcloud;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.opengl.Matrix;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangoutils.ModelMatCalculator;

public class SharedData {

	private FloatBuffer currentXyzIjCoord = FloatBuffer.allocate(1000);
	private FloatBuffer prevXyzIjCoord;
	private ModelMatCalculator modelMatCalculator;
	private TangoPoseData currentPose = new TangoPoseData();
	private TangoPoseData prevPose;
	private ArrayList<FloatBuffer> lastKPointClouds = new ArrayList<FloatBuffer>();
	private ArrayList<TangoPoseData> lastKPose = new ArrayList<TangoPoseData>();
	private double[] cameraTransform = new double [9];
	private int[][] screen; 
	private double[] distortion = new double[3];
	private int height;
	private int width;
	private double cx,cy,fx,fy;

	public SharedData(ModelMatCalculator _modelMatCalc)  {
		modelMatCalculator = _modelMatCalc;
	}

	public void setXyzijCoord(FloatBuffer points) {

		prevXyzIjCoord = currentXyzIjCoord;
		currentXyzIjCoord = FloatBuffer.allocate(points.capacity());

		float [] other_model_mat = modelMatCalculator.getPointCloudModelMatrixCopy();
		ArrayList<float[]> triples = new ArrayList<float[]>();

		for(int a=0; a < points.capacity(); a+=3) {
			float[] triple = new float[4];
			triple[0] = points.get(a); triple[1] = points.get(a+1);
			triple[2] = points.get(a+2);
			triple[3] = 1;
			triples.add(triple);
		}

		for (int b=0; b<triples.size();b++) {
			float[] temp = new float[4];
			float[] temp1 = new float[3];

			Matrix.multiplyMV(temp, 0, other_model_mat, 0, triples.get(b), 0);

			temp1[0] = temp[0];
			temp1[1] = temp[1];
			temp1[2] = temp[2];

			currentXyzIjCoord.put(temp1);
		}
	}

	public FloatBuffer getXyzijCoord() {
		return currentXyzIjCoord;
	}

	public void setPose(TangoPoseData poseData) {
		prevPose = currentPose;
		currentPose = poseData;
	}

	public TangoPoseData getPose() {
		return currentPose;
	}

	public void updateLastKPointClouds(int k) {
		if (lastKPointClouds.size() ==  k) {
			lastKPointClouds.remove(0);
		}
		lastKPointClouds.add(getXyzijCoord());
	}

	// Multiply by k stuff here
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

	public void createCameraTransformMatrix(double cx,double cy, double fx,double fy) {
		this.cx = cx;
		this.cy = cy;
		this.fx = fx;
		this.fy = fy;
	}

	public double[] getCameraTransformMatrix() { 
		return cameraTransform;
	}

	public void convertTo2D(FloatBuffer points) {

		int outsideCount = 0;
		int stackedCount = 0;
		int pointsInserted = 0;
		double totalPoints = points.capacity() / 3;

		ArrayList<double[]> pixelCoords = new ArrayList<double[]>();

		for(int a=0; a < points.capacity(); a+=3) {
			double[] pixel = new double[3];
			pixel[0] = points.get(a) / points.get(a+2); 
			pixel[1] = points.get(a+1) / points.get(a+2);
			pixel[2] = 1;

			double norm_sqd = pixel[0]*pixel[0] + pixel[1]*pixel[1];

			double r2 = norm_sqd;
			double r4 = r2 * r2;
			double r6 = r2 * r4;

			// Multiply by k stuff here
			pixel[0] *= (1.0 + distortion[0]*r2 + distortion[1]*r4 + distortion[2]*r6);
			pixel[1] *= (1.0 + distortion[0]*r2 + distortion[1]*r4 + distortion[2]*r6);

			pixel[0] = (pixel[0] * fx) + cx;
			pixel[1] = (pixel[1] * fy) + cy;

			//			if(pixel[1] > 150 && pixel[1] < 160)
			//				System.out.println("xCoord: " + pixel[0] + ", yCoord: " + pixel[1]);

			int xCoord = (int)Math.round(pixel[0]);
			int yCoord = (int)Math.round(pixel[1]);
			pixel[0] = xCoord;
			pixel[1] = yCoord;
			//System.out.println("xCoord: " + pixel[0] + ", yCoord: " + pixel[1]);

			pixelCoords.add(pixel);

			if(xCoord < 0 || xCoord > width-1 || yCoord < 0 || yCoord > height-1) {
				outsideCount++;
			} else if (screen[xCoord][yCoord] == 1) {
				stackedCount++;
			} else {
				screen[xCoord][yCoord] = 1;
				pointsInserted++;
			}
		}
		System.out.println("Total Pixels: " + pixelCoords.size());
		System.out.println("Individual Points: " + pointsInserted + " or " + pointsInserted/totalPoints*100 + "%");

		System.out.println("Points outside screen: " + outsideCount +
				" or " + (outsideCount / totalPoints)*100 + "%");
		System.out.println("Points stacked: " + stackedCount +
				" or " + (stackedCount / totalPoints)*100 + "%");
		getNeighborCount(pixelCoords, totalPoints);

		//Arrays.fill(screen, 0);
		for (int s=0; s<width; s++) {
			for (int t=0; t<height; t++) {
				screen[s][t] = 0;
			}
		}
	}

	public void createScreen(int _width, int _height) {
		width = _width;
		height = _height;
		screen = new int[width][height];
	}

	public int[][] getScreenResolution(int _width, int height) {
		return screen;
	}

	public void setDistortion(double[] _distortion) {
		distortion = _distortion;
	}

	public void getNeighborCount(ArrayList<double[]> pixelCoords, double totalPoints) {
		int nonFullNeighbors = 0;
		int fullNeighbors = 0;

		for (int i=0; i < pixelCoords.size(); i++) {
			int x = (int)pixelCoords.get(i)[0];
			int y = (int)pixelCoords.get(i)[1];
			if (x > 0 && x < width-1 && y > 0 && y < height-1) {
				if (screen[x][y-1]==1 && screen[x][y+1]==1 && screen[x-1][y]==1 && screen[x+1][y]==1) {
					fullNeighbors++;
				} else {
					nonFullNeighbors++;
				}
			}
		}

		/**
		for (int i=1; i < width-1; i++){
			for (int j=1; j < height-1; j++){
				if (screen[i][j] == 1) {
					if (screen[i][j-1] == 1 && screen[i][j+1] == 1 &&
							screen[i-1][j] == 1 && screen[i+1][j] == 1) {
						fullNeighbors++;
					} else {
						nonFullNeighbors++;
					}
				}
			}
		}
		 */

		System.out.println("Pixels without full neighbors: " + nonFullNeighbors + 
				" or " + (nonFullNeighbors / totalPoints)*100 + "%");
		System.out.println("Pixels with full neighbors: " + fullNeighbors + 
				" or " + (fullNeighbors / totalPoints)*100 + "%");
		System.out.println("____________________________");
	}
}