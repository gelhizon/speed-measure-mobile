package com.gel.speedmeasure;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class BlobFinder {
	private static final String TAG = null;
	private static int previewWidth;
	private static int previewHeight;

	private static int[] labelBuffer;

	private static int[] labelTable;
	private static int[] xMinTable;
	private static int[] xMaxTable;
	private static int[] yMinTable;
	private static int[] yMaxTable;
	private static int[] massTable;

	static class Blob {
		public int xMin;
		public int xMax;
		public int yMin;
		public int yMax;
		public int mass;

		public Blob(int xMin, int xMax, int yMin, int yMax, int mass) {
			this.xMin = xMin;
			this.xMax = xMax;
			this.yMin = yMin;
			this.yMax = yMax;
			this.mass = mass;
		}
	}

	public BlobFinder(int previewWidth, int previewHeight) {
		this.previewWidth = previewWidth;
		this.previewHeight = previewHeight;

		labelBuffer = new int[previewWidth * previewHeight];

		// The maximum number of blobs is given by an image filled with equally
		// spaced single pixel
		// blobs. For images with less blobs, memory will be wasted, but this
		// approach is simpler and
		// probably quicker than dynamically resizing arrays
		int tableSize = previewWidth * previewHeight / 4;

		labelTable = new int[tableSize];
		xMinTable = new int[tableSize];
		xMaxTable = new int[tableSize];
		yMinTable = new int[tableSize];
		yMaxTable = new int[tableSize];
		massTable = new int[tableSize];
	}

	public static List<Blob> detectBlobs(byte[] srcData, int minBlobMass, int maxBlobMass, byte matchVal, List<Blob> blobList) {

		// This is the neighboring pixel pattern. For position X, A, B, C & D
		// are checked
		// A B C
		// D X

		int srcPtr = 0;
		int aPtr = -previewWidth - 1;
		int bPtr = -previewWidth;
		int cPtr = -previewWidth + 1;
		int dPtr = -1;

		int label = 1;

		// Iterate through pixels looking for connected regions. Assigning
		// labels
		for (int y = 0; y < previewHeight; y++) {
			for (int x = 0; x < previewWidth; x++) {
				labelBuffer[srcPtr] = 0;

				// Check if on foreground pixel
				if (srcData[srcPtr] == matchVal) {
					// Find label for neighbors (0 if out of range)
					int aLabel = (x > 0 && y > 0) ? labelTable[labelBuffer[aPtr]] : 0;
					int bLabel = (y > 0) ? labelTable[labelBuffer[bPtr]] : 0;
					int cLabel = (x < previewWidth - 1 && y > 0) ? labelTable[labelBuffer[cPtr]] : 0;
					int dLabel = (x > 0) ? labelTable[labelBuffer[dPtr]] : 0;

					// Look for label with least value
					int min = Integer.MAX_VALUE;
					if (aLabel != 0 && aLabel < min)
						min = aLabel;
					if (bLabel != 0 && bLabel < min)
						min = bLabel;
					if (cLabel != 0 && cLabel < min)
						min = cLabel;
					if (dLabel != 0 && dLabel < min)
						min = dLabel;

					// If no neighbors in foreground
					if (min == Integer.MAX_VALUE) {
						labelBuffer[srcPtr] = label;
						labelTable[label] = label;

						// initialize min/max x,y for label
						yMinTable[label] = y;
						yMaxTable[label] = y;
						xMinTable[label] = x;
						xMaxTable[label] = x;
						massTable[label] = 1;

						label++;
					}

					// neighbor found
					else {
						// Label pixel with lowest label from neighbors
						labelBuffer[srcPtr] = min;

						// Update min/max x,y for label
						yMaxTable[min] = y;
						massTable[min]++;
						if (x < xMinTable[min])
							xMinTable[min] = x;
						if (x > xMaxTable[min])
							xMaxTable[min] = x;

						if (aLabel != 0)
							labelTable[aLabel] = min;
						if (bLabel != 0)
							labelTable[bLabel] = min;
						if (cLabel != 0)
							labelTable[cLabel] = min;
						if (dLabel != 0)
							labelTable[dLabel] = min;
					}
				}

				srcPtr++;
				aPtr++;
				bPtr++;
				cPtr++;
				dPtr++;
			}
		}

		// Iterate through labels pushing min/max x,y values towards minimum
		// label
		if (blobList == null)
			blobList = new ArrayList<Blob>();

		for (int i = label - 1; i > 0; i--) {
			if (labelTable[i] != i) {
				if (xMaxTable[i] > xMaxTable[labelTable[i]])
					xMaxTable[labelTable[i]] = xMaxTable[i];
				if (xMinTable[i] < xMinTable[labelTable[i]])
					xMinTable[labelTable[i]] = xMinTable[i];
				if (yMaxTable[i] > yMaxTable[labelTable[i]])
					yMaxTable[labelTable[i]] = yMaxTable[i];
				if (yMinTable[i] < yMinTable[labelTable[i]])
					yMinTable[labelTable[i]] = yMinTable[i];
				massTable[labelTable[i]] += massTable[i];

				int l = i;
				while (l != labelTable[l])
					l = labelTable[l];
				labelTable[i] = l;
			} else {
				// Ignore blobs that butt against corners
				if (i == labelBuffer[0])
					continue; // Top Left
				if (i == labelBuffer[previewWidth])
					continue; // Top Right
				if (i == labelBuffer[(previewWidth * previewHeight) - previewWidth + 1])
					continue; // Bottom Left
				if (i == labelBuffer[(previewWidth * previewHeight) - 1])
					continue; // Bottom Right

				if (massTable[i] >= minBlobMass && (massTable[i] <= maxBlobMass || maxBlobMass == -1)) {
					Blob blob = new Blob(xMinTable[i], xMaxTable[i], yMinTable[i], yMaxTable[i], massTable[i]);
					blobList.add(blob);
				}
			}
		}
		return blobList;
	}

	public static void detectBlobs(int[] srcData, int minBlobMass, int maxBlobMass, int matchVal, List<Blob> blobList) {

		// This is the neighboring pixel pattern. For position X, A, B, C & D
		// are checked
		// A B C
		// D X

		int srcPtr = 0;
		int aPtr = -previewWidth - 1;
		int bPtr = -previewWidth;
		int cPtr = -previewWidth + 1;
		int dPtr = -1;

		int label = 1;

		// Iterate through pixels looking for connected regions. Assigning
		// labels
		for (int y = 0; y < previewHeight; y++) {
			for (int x = 0; x < previewWidth; x++) {
				labelBuffer[srcPtr] = 0;

				// Check if on foreground pixel
				if (srcData[srcPtr] == matchVal) {
					// Find label for neighbors (0 if out of range)
					int aLabel = (x > 0 && y > 0) ? labelTable[labelBuffer[aPtr]] : 0;
					int bLabel = (y > 0) ? labelTable[labelBuffer[bPtr]] : 0;
					int cLabel = (x < previewWidth - 1 && y > 0) ? labelTable[labelBuffer[cPtr]] : 0;
					int dLabel = (x > 0) ? labelTable[labelBuffer[dPtr]] : 0;

					// Look for label with least value
					int min = Integer.MAX_VALUE;
					if (aLabel != 0 && aLabel < min)
						min = aLabel;
					if (bLabel != 0 && bLabel < min)
						min = bLabel;
					if (cLabel != 0 && cLabel < min)
						min = cLabel;
					if (dLabel != 0 && dLabel < min)
						min = dLabel;

					// If no neighbors in foreground
					if (min == Integer.MAX_VALUE) {
						labelBuffer[srcPtr] = label;
						labelTable[label] = label;

						// Initialize min/max x,y for label
						yMinTable[label] = y;
						yMaxTable[label] = y;
						xMinTable[label] = x;
						xMaxTable[label] = x;
						massTable[label] = 1;

						label++;
					}

					// neighbor found
					else {
						// Label pixel with lowest label from neighbors
						labelBuffer[srcPtr] = min;

						// Update min/max x,y for label
						yMaxTable[min] = y;
						massTable[min]++;
						if (x < xMinTable[min])
							xMinTable[min] = x;
						if (x > xMaxTable[min])
							xMaxTable[min] = x;
						if (aLabel != 0)
							labelTable[aLabel] = min;
						if (bLabel != 0)
							labelTable[bLabel] = min;
						if (cLabel != 0)
							labelTable[cLabel] = min;
						if (dLabel != 0)
							labelTable[dLabel] = min;
					}
				}

				srcPtr++;
				aPtr++;
				bPtr++;
				cPtr++;
				dPtr++;
			}
		}

		// Iterate through labels pushing min/max x,y values towards minimum
		// label
		for (int i = label - 1; i > 0; i--) {
			if (labelTable[i] != i) {
				if (xMaxTable[i] > xMaxTable[labelTable[i]])
					xMaxTable[labelTable[i]] = xMaxTable[i];
				if (xMinTable[i] < xMinTable[labelTable[i]])
					xMinTable[labelTable[i]] = xMinTable[i];
				if (yMaxTable[i] > yMaxTable[labelTable[i]])
					yMaxTable[labelTable[i]] = yMaxTable[i];
				if (yMinTable[i] < yMinTable[labelTable[i]])
					yMinTable[labelTable[i]] = yMinTable[i];
				massTable[labelTable[i]] += massTable[i];

				int l = i;
				while (l != labelTable[l])
					l = labelTable[l];
				labelTable[i] = l;
			} else {
				// Ignore blobs that butt against corners
				if (i == labelBuffer[0])
					continue; // Top Left
				if (i == labelBuffer[previewWidth])
					continue; // Top Right
				if (i == labelBuffer[(previewWidth * previewHeight) - previewWidth + 1])
					continue; // Bottom Left
				if (i == labelBuffer[(previewWidth * previewHeight) - 1])
					continue; // Bottom Right

				if (massTable[i] >= minBlobMass && (massTable[i] <= maxBlobMass || maxBlobMass == -1) && (xMinTable[i] != 0 || xMaxTable[i] != previewWidth - 1)) {
					Blob blob = new Blob(xMinTable[i], xMaxTable[i], yMinTable[i], yMaxTable[i], massTable[i]);
					blobList.add(blob);
				}
			}
		}

	}
}
