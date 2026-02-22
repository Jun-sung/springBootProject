package com.safehome.util;


public class TileBboxUtil {

  // return [minLat, minLng, maxLat, maxLng]
  public static double[] bbox(int z, int x, int y) {
    double minLng = tile2lon(x, z);
    double maxLng = tile2lon(x + 1, z);
    double minLat = tile2lat(y + 1, z);
    double maxLat = tile2lat(y, z);
    return new double[]{minLat, minLng, maxLat, maxLng};
  }

  private static double tile2lon(int x, int z) {
    return x / Math.pow(2.0, z) * 360.0 - 180;
  }

  private static double tile2lat(int y, int z) {
    double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
    return Math.toDegrees(Math.atan(Math.sinh(n)));
  }
}