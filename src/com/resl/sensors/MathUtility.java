package com.resl.sensors;

import Jama.LUDecomposition;
import Jama.Matrix;

public class MathUtility
{
	/**
	 * This method calculates the inverse of a matrix. Please ensure that the matrix is a square matrix
	 *  
 	 * @param resultMatrix
	 * @param inputMatrix
	 */
	public static void calculateInverse(float [] resultMatrix, float inputMatrix[])
	{
		int size = (int) Math.sqrt(inputMatrix.length);
		
        double [][] values = new double[size][size];
        double [][] rhsIdentity = new double[size][size];
        
        // Copy values to the values and rhsIdentity matrix
        for (int i = 0; i < size; i++)
        {
        	for (int j = 0; j < size; j++)
            {
        		values[i][j] = inputMatrix[i * size + j];
        		
        		if ( i == j)
        		{
        			rhsIdentity[i][j] = 1;
        		}
        		else
        		{
        			rhsIdentity[i][j] = 0;
        		}
            }
        }
        
        Matrix a = new Matrix(values);
        LUDecomposition luDecomposition = new LUDecomposition(a);
        
        Matrix b = new Matrix(rhsIdentity);
        Matrix x = luDecomposition.solve(b);
        
        for (int i = 0; i < x.getRowDimension(); i++)
        {
        	for (int j = 0; j < x.getColumnDimension(); j++)
            {
        		resultMatrix[i * x.getColumnDimension() + j] = (float) x.get(i, j);
            }
        }
	}
}
