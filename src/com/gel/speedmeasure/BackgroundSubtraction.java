package com.gel.speedmeasure;

public class BackgroundSubtraction {
	private static final String TAG = "ImageProcessing.java";
	private static int width;
	private static int grayThreshold;
	private static int colorThreshold;
	private static int pixelTimeLimit;

	private static int[] backgroundYUV;
	private static int[] grayOutputMask;
	private static int[] colorOutputMask;
	private static long[] pixelTime;
	private static int size;
	private static boolean isGray;
	private static boolean isColor;
	private static boolean grayOrColor;

	public BackgroundSubtraction(int size, int width, int grayThreshold, int colorThreshold, int pixelTimeLimit, boolean isGray, boolean isColor, boolean grayOrColor) {
		this.width = width;
		this.grayThreshold = grayThreshold;
		this.colorThreshold = colorThreshold;
		this.pixelTimeLimit = pixelTimeLimit;
		this.size = size;
		this.grayOrColor = grayOrColor;
		this.isGray = isGray;
		this.isColor = isColor;

		pixelTime = new long[size + (size) / 2];
		backgroundYUV = new int[size + (size / 2)];
		grayOutputMask = new int[size];
		colorOutputMask = new int[size];
	}

	public static void subtract(byte[] curImg, int[] fgMask, long time) {
		int u, v, y1, y2, y3, y4;
		int topLeft, topRight, bottomLeft, bottomRight, uIndex, vIndex;
		int output1, output2, output3, output4, uDifference, vDifference;

		for (int i = 0, k = 0; i < size; i += 2, k += 2) {
			topLeft = i;
			topRight = i + 1;
			bottomLeft = width + i;
			bottomRight = width + i + 1;
			uIndex = size + k;

			vIndex = size + k + 1;
			v = curImg[vIndex] & 0xff;

			// gray and color mixes
			// or , and
			if (isGray && isColor) {
				// gray data
				y1 = curImg[i] & 0xff;
				y2 = curImg[topRight] & 0xff;
				y3 = curImg[bottomLeft] & 0xff;
				y4 = curImg[bottomRight] & 0xff;

				output1 = Math.abs(y1 - backgroundYUV[topLeft]);
				output2 = Math.abs(y2 - backgroundYUV[topRight]);
				output3 = Math.abs(y3 - backgroundYUV[bottomLeft]);
				output4 = Math.abs(y4 - backgroundYUV[bottomRight]);

				if (output1 > grayThreshold) {
					grayOutputMask[topLeft] = 0xffffffff;
					pixelTime[topLeft] += time;
					if (pixelTime[topLeft] >= pixelTimeLimit) {
						backgroundYUV[topLeft] = y1;
						pixelTime[topLeft] %= pixelTimeLimit;
					}
				} else {
					grayOutputMask[topLeft] = 0xff000000;
					if (pixelTime[topLeft] > 0) {
						pixelTime[topLeft] -= time;
					}
				}

				if (output2 > grayThreshold) {
					grayOutputMask[topRight] = 0xffffffff;
					pixelTime[topRight] += time;
					if (pixelTime[topRight] >= pixelTimeLimit) {
						backgroundYUV[topRight] = y2;
						pixelTime[topRight] %= pixelTimeLimit;
					}
				} else {
					grayOutputMask[topRight] = 0xff000000;
					if (pixelTime[topRight] > 0) {
						pixelTime[topRight] -= time;
					}
				}

				if (output3 > grayThreshold) {
					grayOutputMask[bottomLeft] = 0xffffffff;
					pixelTime[bottomLeft] += time;
					if (pixelTime[bottomLeft] >= pixelTimeLimit) {
						backgroundYUV[bottomLeft] = y3;
						pixelTime[bottomLeft] %= pixelTimeLimit;
					}
				} else {
					grayOutputMask[bottomLeft] = 0xff000000;
					if (pixelTime[bottomLeft] > 0) {
						pixelTime[bottomLeft] -= time;
					}
				}

				if (output4 > grayThreshold) {
					grayOutputMask[bottomRight] = 0xffffffff;
					pixelTime[bottomRight] += time;
					if (pixelTime[bottomRight] >= pixelTimeLimit) {
						backgroundYUV[bottomRight] = y4;
						pixelTime[bottomRight] %= pixelTimeLimit;
					}
				} else {
					grayOutputMask[bottomRight] = 0xff000000;
					if (pixelTime[bottomRight] > 0) {
						pixelTime[bottomRight] -= time;
					}
				}

				// color data
				u = curImg[uIndex] & 0xff;
				uDifference = Math.abs(u - backgroundYUV[uIndex]);
				vDifference = Math.abs(v - backgroundYUV[vIndex]);
				if (uDifference > colorThreshold || vDifference > colorThreshold) {
					colorOutputMask[topLeft] = 0xffffffff;
					colorOutputMask[topRight] = 0xffffffff;
					colorOutputMask[bottomLeft] = 0xffffffff;
					colorOutputMask[bottomRight] = 0xffffffff;
					pixelTime[uIndex] += time;
					if (pixelTime[uIndex] >= pixelTimeLimit) {
						backgroundYUV[uIndex] = u;
						backgroundYUV[vIndex] = v;
						pixelTime[uIndex] %= pixelTimeLimit;
					}
				} else {
					colorOutputMask[topLeft] = 0xff000000;
					colorOutputMask[topRight] = 0xff000000;
					colorOutputMask[bottomLeft] = 0xff000000;
					colorOutputMask[bottomRight] = 0xff000000;
					if (pixelTime[uIndex] > 0) {
						pixelTime[uIndex] -= time;
					}
				}

				// color and gray mixes
				if (grayOrColor) {
					if (grayOutputMask[topRight] == 0xffffffff || colorOutputMask[topRight] == 0xffffffff) {
						fgMask[topRight] = 0xffffffff;
					} else {
						fgMask[topRight] = 0xff000000;
					}
					if (grayOutputMask[topLeft] == 0xffffffff || colorOutputMask[topLeft] == 0xffffffff) {
						fgMask[topLeft] = 0xffffffff;
					} else {
						fgMask[topLeft] = 0xff000000;
					}
					if (grayOutputMask[bottomRight] == 0xffffffff || colorOutputMask[bottomRight] == 0xffffffff) {
						fgMask[bottomRight] = 0xffffffff;
					} else {
						fgMask[bottomRight] = 0xff000000;
					}
					if (grayOutputMask[bottomLeft] == 0xffffffff || colorOutputMask[bottomLeft] == 0xffffffff) {
						fgMask[bottomLeft] = 0xffffffff;
					} else {
						fgMask[bottomLeft] = 0xff000000;
					}
				} else {
					if (grayOutputMask[topRight] == 0xffffffff && colorOutputMask[topRight] == 0xffffffff) {
						fgMask[topRight] = 0xffffffff;
					} else {
						fgMask[topRight] = 0xff000000;
					}
					if (grayOutputMask[topLeft] == 0xffffffff && colorOutputMask[topLeft] == 0xffffffff) {
						fgMask[topLeft] = 0xffffffff;
					} else {
						fgMask[topLeft] = 0xff000000;
					}
					if (grayOutputMask[bottomRight] == 0xffffffff && colorOutputMask[bottomRight] == 0xffffffff) {
						fgMask[bottomRight] = 0xffffffff;
					} else {
						fgMask[bottomRight] = 0xff000000;
					}
					if (grayOutputMask[bottomLeft] == 0xffffffff && colorOutputMask[bottomLeft] == 0xffffffff) {
						fgMask[bottomLeft] = 0xffffffff;
					} else {
						fgMask[bottomLeft] = 0xff000000;
					}
				}
			} else if (isGray) { // gray only
				y1 = curImg[i] & 0xff;
				y2 = curImg[topRight] & 0xff;
				y3 = curImg[bottomLeft] & 0xff;
				y4 = curImg[bottomRight] & 0xff;

				output1 = Math.abs(y1 - backgroundYUV[topLeft]);
				output2 = Math.abs(y2 - backgroundYUV[topRight]);
				output3 = Math.abs(y3 - backgroundYUV[bottomLeft]);
				output4 = Math.abs(y4 - backgroundYUV[bottomRight]);

				if (output1 > grayThreshold) {
					fgMask[topLeft] = 0xffffffff;
					pixelTime[topLeft] += time;
					if (pixelTime[topLeft] >= pixelTimeLimit) {
						backgroundYUV[topLeft] = y1;
						pixelTime[topLeft] %= pixelTimeLimit;
					}
				} else {
					fgMask[topLeft] = 0xff000000;
					if (pixelTime[topLeft] > 0) {
						pixelTime[topLeft] -= time;
					}
				}

				if (output2 > grayThreshold) {
					fgMask[topRight] = 0xffffffff;
					pixelTime[topRight] += time;
					if (pixelTime[topRight] >= pixelTimeLimit) {
						backgroundYUV[topRight] = y2;
						pixelTime[topRight] %= pixelTimeLimit;
					}
				} else {
					fgMask[topRight] = 0xff000000;
					if (pixelTime[topRight] > 0) {
						pixelTime[topRight] -= time;
					}
				}

				if (output3 > grayThreshold) {
					fgMask[bottomLeft] = 0xffffffff;
					pixelTime[bottomLeft] += time;
					if (pixelTime[bottomLeft] >= pixelTimeLimit) {
						backgroundYUV[bottomLeft] = y3;
						pixelTime[bottomLeft] %= pixelTimeLimit;
					}
				} else {
					fgMask[bottomLeft] = 0xff000000;
					if (pixelTime[bottomLeft] > 0) {
						pixelTime[bottomLeft] -= time;
					}
				}

				if (output4 > grayThreshold) {
					fgMask[bottomRight] = 0xffffffff;
					pixelTime[bottomRight] += time;
					if (pixelTime[bottomRight] >= pixelTimeLimit) {
						backgroundYUV[bottomRight] = y4;
						pixelTime[bottomRight] %= pixelTimeLimit;
					}
				} else {
					fgMask[bottomRight] = 0xff000000;
					if (pixelTime[bottomRight] > 0) {
						pixelTime[bottomRight] -= time;
					}
				}
			} else if (isColor) { // color only?
				u = curImg[uIndex] & 0xff;
				uDifference = Math.abs(u - backgroundYUV[uIndex]);
				vDifference = Math.abs(v - backgroundYUV[vIndex]);
				if (uDifference > colorThreshold || vDifference > colorThreshold) {
					fgMask[topLeft] = 0xffffffff;
					fgMask[topRight] = 0xffffffff;
					fgMask[bottomLeft] = 0xffffffff;
					fgMask[bottomRight] = 0xffffffff;
					pixelTime[uIndex] += time;
					if (pixelTime[uIndex] >= pixelTimeLimit) {
						backgroundYUV[uIndex] = u;
						backgroundYUV[vIndex] = v;
						pixelTime[uIndex] %= pixelTimeLimit;
					}
				} else {
					fgMask[topLeft] = 0xff000000;
					fgMask[topRight] = 0xff000000;
					fgMask[bottomLeft] = 0xff000000;
					fgMask[bottomRight] = 0xff000000;
					if (pixelTime[uIndex] > 0) {
						pixelTime[uIndex] -= time;
					}
				}
			}

			if (i != 0 && (i + 2) % width == 0) {
				i += width;
			}
		}
	}

	public static int getGrayThreshold() {
		return grayThreshold;
	}

	public static void setGrayThreshold(int grayThreshold) {
		BackgroundSubtraction.grayThreshold = grayThreshold;
	}

	public static int getColorThreshold() {
		return colorThreshold;
	}

	public static void setColorThreshold(int colorThreshold) {
		BackgroundSubtraction.colorThreshold = colorThreshold;
	}

	public static int getPixelTimeLimit() {
		return pixelTimeLimit;
	}

	public static void setPixelTimeLimit(int pixelTimeLimit) {
		BackgroundSubtraction.pixelTimeLimit = pixelTimeLimit;
	}

	public static int[] getBackgroundYUV() {
		return backgroundYUV;
	}

	public static void setBackgroundYUV(int[] backgroundYUV) {
		BackgroundSubtraction.backgroundYUV = backgroundYUV;
	}

	public static boolean isGrayOrColor() {
		return grayOrColor;
	}

	public static void setGrayOrColor(boolean grayOrColor) {
		BackgroundSubtraction.grayOrColor = grayOrColor;
	}

	public static boolean isGray() {
		return isGray;
	}

	public static void setGray(boolean isGray) {
		BackgroundSubtraction.isGray = isGray;
	}

	public static boolean isColor() {
		return isColor;
	}

	public static void setColor(boolean isColor) {
		BackgroundSubtraction.isColor = isColor;
	}

}
