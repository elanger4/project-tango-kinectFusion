/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.tangoutils.renderables;

import android.opengl.Matrix;

/**
 * Base class for all self-drawing OpenGL objects used in Tango Java examples.
 * Contains common logic for handling the MVP matrices.
 */
public abstract class Renderable {

    private float[] mModelMatrix = new float[16];
    private float[] mMvMatrix = new float[16];
    private float[] mMvMatrix1 = new float[16];
    private float[] mMvpMatrix = new float[16];
    private float[] mMvpMatrix4Model = new float[16];

    /**
     * Applies the view and projection matrices and draws the Renderable.
     * 
     * @param viewMatrix
     *            the view matrix to map from world space to camera space.
     * @param projectionMatrix
     *            the projection matrix to map from camera space to screen
     *            space.
     */
    public abstract void draw(float[] viewMatrix, float[] projectionMatrix);

    public synchronized void updateMvpMatrix(float[] viewMatrix,
            float[] projectionMatrix) {
        // Compose the model, view, and projection matrices into a single mvp
        // matrix
    	float[] mIdentityMatrix = new float[16];
        Matrix.setIdentityM(mIdentityMatrix, 0);
        Matrix.setIdentityM(mMvMatrix, 0);
        Matrix.setIdentityM(mMvMatrix1, 0);
        Matrix.setIdentityM(mMvpMatrix, 0);
        Matrix.setIdentityM(mMvpMatrix4Model, 0);
        //Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.multiplyMM(mMvMatrix, 0, viewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMvMatrix1, 0, viewMatrix, 0, mIdentityMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, projectionMatrix, 0, mMvMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix4Model, 0, projectionMatrix, 0, mMvMatrix1, 0);
        // ^^^ Find a way to update the mMvMatrix for the points once 
        //     they've been transformed into the correct coordinate system
    }

    /**
    public synchronized void updateMvpMatrix1(float[] viewMatrix,
            float[] projectionMatrix) {
        // Compose the model, view, and projection matrices into a single mvp
        // matrix
        Matrix.setIdentityM(mMvMatrix, 0);
        Matrix.setIdentityM(mMvpMatrix, 0);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.multiplyMM(mMvMatrix, 0, viewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, projectionMatrix, 0, mMvMatrix, 0);
        // ^^^ Find a way to update the mMvMatrix for the points once 
        //     they've been transformed into the correct coordinate system
     
    }
    */
    public float[] getModelMatrix() {
        return mModelMatrix;
    }

    public void setModelMatrix(float[] modelMatrix) {
        mModelMatrix = modelMatrix;
    }

    public float[] getMvMatrix() {
        return mMvMatrix;
    }

    public float[] getMvpMatrix() {
        return mMvpMatrix;
    }

    public float[] getMvpMatrix4Model() {
        return mMvpMatrix4Model;
    }
}
