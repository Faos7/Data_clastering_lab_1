import java.util.Arrays;
import java.util.Random;

/**
 * Created by faos7 on 16.10.17.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("\nBegin k-means clustering\n");

        double[][] rawData = new double[20][];
        rawData[0] = new double[] { 65.0, 220.0 };
        rawData[1] = new double[] { 73.0, 160.0 };
        rawData[2] = new double[] { 59.0, 110.0 };
        rawData[3] = new double[] { 61.0, 120.0 };
        rawData[4] = new double[] { 75.0, 150.0 };
        rawData[5] = new double[] { 67.0, 240.0 };
        rawData[6] = new double[] { 68.0, 230.0 };
        rawData[7] = new double[] { 70.0, 220.0 };
        rawData[8] = new double[] { 62.0, 130.0 };
        rawData[9] = new double[] { 66.0, 210.0 };
        rawData[10] = new double[] { 77.0, 190.0 };
        rawData[11] = new double[] { 75.0, 180.0 };
        rawData[12] = new double[] { 74.0, 170.0 };
        rawData[13] = new double[] { 70.0, 210.0 };
        rawData[14] = new double[] { 61.0, 110.0 };
        rawData[15] = new double[] { 58.0, 100.0 };
        rawData[16] = new double[] { 66.0, 230.0 };
        rawData[17] = new double[] { 59.0, 120.0 };
        rawData[18] = new double[] { 68.0, 210.0 };
        rawData[19] = new double[] { 61.0, 130.0 };

        System.out.println("Raw unclustered data:\n");
        System.out.println("    Height Weight");
        System.out.println("-------------------");
        ShowData(rawData, 1, true, true);

        int numClusters = 3;
        System.out.println("\nSetting numClusters to " + numClusters);

        int[] clustering = Cluster(rawData, numClusters);

        System.out.println("\nK-means clustering complete\n");

        System.out.println("Final clustering in internal form:\n");
        ShowVector(clustering, true);

        System.out.println("Raw data by cluster:\n");
        ShowClustered(rawData, clustering, numClusters, 1);

        System.out.println("\nEnd k-means clustering\n");
    }

    public static int[] Cluster(double[][] rawData, int numClusters)
    {
        // k-means clustering
        // index of return is tuple ID, cell is cluster ID
        // ex: [2 1 0 0 2 2] means tuple 0 is cluster 2, tuple 1 is cluster 1, tuple 2 is cluster 0, tuple 3 is cluster 0, etc.
        // an alternative clustering DS to save space is to use the .NET BitArray class
        double[][] data = Normalized(rawData); // so large values don't dominate

        boolean changed = true; // was there a change in at least one cluster assignment?
        boolean success = true; // were all means able to be computed? (no zero-count clusters)

        // init clustering[] to get things started
        // an alternative is to initialize means to randomly selected tuples
        // then the processing loop is
        // loop
        //    update clustering
        //    update means
        // end loop
        int[] clustering = InitClustering(data.length, numClusters, 0); // semi-random initialization
        double[][] means = Allocate(numClusters, data[0].length); // small convenience

        int maxCount = data.length * 10; // sanity check
        int ct = 0;
        while (changed == true && success == true && ct < maxCount)
        {
            ++ct; // k-means typically converges very quickly
            success = UpdateMeans(data, clustering, means); // compute new cluster means if possible. no effect if fail
            changed = UpdateClustering(data, clustering, means); // (re)assign tuples to clusters. no effect if fail
        }
        // consider adding means[][] as an out parameter - the final means could be computed
        // the final means are useful in some scenarios (e.g., discretization and RBF centroids)
        // and even though you can compute final means from final clustering, in some cases it
        // makes sense to return the means (at the expense of some method signature uglinesss)
        //
        // another alternative is to return, as an out parameter, some measure of cluster goodness
        // such as the average distance between cluster means, or the average distance between tuples in
        // a cluster, or a weighted combination of both
        return clustering;
    }

    private static double[][] Normalized(double[][] rawData)
    {
        // normalize raw data by computing (x - mean) / stddev
        // primary alternative is min-max:
        // v' = (v - min) / (max - min)

        // make a copy of input data
        double[][] result = new double[rawData.length][];
        for (int i = 0; i < rawData.length; ++i)
        {
//            result[i] = new double[rawData[i].length];
//            TODO find which kind of Array copy we need
            result[i] = Arrays.copyOf(rawData[i], rawData[i].length);
//            Array.Copy(rawData[i], result[i], rawData[i].length);
        }

        for (int j = 0; j < result[0].length; ++j) // each col
        {
            double colSum = 0.0;
            for (int i = 0; i < result.length; ++i)
                colSum += result[i][j];
            double mean = colSum / result.length;
            double sum = 0.0;
            for (int i = 0; i < result.length; ++i)
                sum += (result[i][j] - mean) * (result[i][j] - mean);
            double sd = sum / result.length;
            for (int i = 0; i < result.length; ++i)
                result[i][j] = (result[i][j] - mean) / sd;
        }
        return result;
    }

    private static int[] InitClustering(int numTuples, int numClusters, int randomSeed)
    {
        // init clustering semi-randomly (at least one tuple in each cluster)
        // consider alternatives, especially k-means++ initialization,
        // or instead of randomly assigning each tuple to a cluster, pick
        // numClusters of the tuples as initial centroids/means then use
        // those means to assign each tuple to an initial cluster.
        Random random = new Random(randomSeed);
        int[] clustering = new int[numTuples];
        for (int i = 0; i < numClusters; ++i) // make sure each cluster has at least one tuple
            clustering[i] = i;
        for (int i = numClusters; i < clustering.length; ++i)
            clustering[i] = random.nextInt(numClusters); // other assignments random
        return clustering;
    }

    private static double[][] Allocate(int numClusters, int numColumns)
    {
        // convenience matrix allocator for Cluster()
        double[][] result = new double[numClusters][];
        for (int k = 0; k < numClusters; ++k)
            result[k] = new double[numColumns];
        return result;
    }

    private static boolean UpdateMeans(double[][] data, int[] clustering, double[][] means)
    {
        // returns false if there is a cluster that has no tuples assigned to it
        // parameter means[][] is really a ref parameter

        // check existing cluster counts
        // can omit this check if InitClustering and UpdateClustering
        // both guarantee at least one tuple in each cluster (usually true)
        int numClusters = means.length;
        int[] clusterCounts = new int[numClusters];
        for (int i = 0; i < data.length; ++i)
        {
            int cluster = clustering[i];
            ++clusterCounts[cluster];
        }

        for (int k = 0; k < numClusters; ++k)
            if (clusterCounts[k] == 0)
                return false; // bad clustering. no change to means[][]

        // update, zero-out means so it can be used as scratch matrix
        for (int k = 0; k < means.length; ++k)
            for (int j = 0; j < means[k].length; ++j)
                means[k][j] = 0.0;

        for (int i = 0; i < data.length; ++i)
        {
            int cluster = clustering[i];
            for (int j = 0; j < data[i].length; ++j)
                means[cluster][j] += data[i][j]; // accumulate sum
        }

        for (int k = 0; k < means.length; ++k)
            for (int j = 0; j < means[k].length; ++j)
                means[k][j] /= clusterCounts[k]; // danger of div by 0
        return true;
    }

    private static boolean UpdateClustering(double[][] data, int[] clustering, double[][] means)
    {
        // (re)assign each tuple to a cluster (closest mean)
        // returns false if no tuple assignments change OR
        // if the reassignment would result in a clustering where
        // one or more clusters have no tuples.

        int numClusters = means.length;
        boolean changed = false;

//        int[] newClustering = new int[clustering.length]; // proposed result
        int[] newClustering = Arrays.copyOf(clustering, clustering.length);
//        Array.Copy(clustering, newClustering, clustering.length);

        double[] distances = new double[numClusters]; // distances from curr tuple to each mean

        for (int i = 0; i < data.length; ++i) // walk thru each tuple
        {
            for (int k = 0; k < numClusters; ++k)
                distances[k] = Distance(data[i], means[k]); // compute distances from curr tuple to all k means

            int newClusterID = MinIndex(distances); // find closest mean ID
            if (newClusterID != newClustering[i])
            {
                changed = true;
                newClustering[i] = newClusterID; // update
            }
        }

        if (changed == false)
            return false; // no change so bail and don't update clustering[][]

        // check proposed clustering[] cluster counts
        int[] clusterCounts = new int[numClusters];
        for (int i = 0; i < data.length; ++i)
        {
            int cluster = newClustering[i];
            ++clusterCounts[cluster];
        }

        for (int k = 0; k < numClusters; ++k)
            if (clusterCounts[k] == 0)
                return false; // bad clustering. no change to clustering[][]

        clustering = Arrays.copyOf(newClustering, newClustering.length);
//        Array.Copy(newClustering, clustering, newClustering.length); // update
        return true; // good clustering and at least one change
    }

    private static double Distance(double[] tuple, double[] mean)
    {
        // Euclidean distance between two vectors for UpdateClustering()
        // consider alternatives such as Manhattan distance
        double sumSquaredDiffs = 0.0;
        for (int j = 0; j < tuple.length; ++j)
            sumSquaredDiffs += Math.pow((tuple[j] - mean[j]), 2);
        return Math.sqrt(sumSquaredDiffs);
    }

    private static int MinIndex(double[] distances)
    {
        // index of smallest value in array
        // helper for UpdateClustering()
        int indexOfMin = 0;
        double smallDist = distances[0];
        for (int k = 0; k < distances.length; ++k)
        {
            if (distances[k] < smallDist)
            {
                smallDist = distances[k];
                indexOfMin = k;
            }
        }
        return indexOfMin;
    }

    // ============================================================================

    // misc display helpers for demo

    static void ShowData(double[][] data, int decimals, boolean indices, boolean newLine)
    {
        for (int i = 0; i < data.length; ++i)
        {
//            if (indices) System.out.println(i.ToString().PadLeft(3) + " ");
            if (indices) System.out.print(padLeft(""+i, 3));
            for (int j = 0; j < data[i].length; ++j)
            {
                if (data[i][j] >= 0.0) System.out.print(" ");
//                System.out.println(data[i][j].ToString("F" + decimals) + " ");
                System.out.print(String.format(java.util.Locale.US,"%."+decimals+"f", data[i][j]) + " ");
            }
            System.out.println("");
        }
        if (newLine) System.out.println("");
    } // ShowData

    static void ShowVector(int[] vector, boolean newLine)
    {
        for (int i = 0; i < vector.length; ++i)
            System.out.print(vector[i] + " ");
        if (newLine) System.out.println("\n");
    }

    static void ShowClustered(double[][] data, int[] clustering, int numClusters, int decimals)
    {
        for (int k = 0; k < numClusters; ++k) {
            System.out.println("===================");
            for (int i = 0; i < data.length; ++i) {
                int clusterID = clustering[i];
                if (clusterID != k) continue;
//                TODO get what kind of string should be in output
//                System.out.println(i.ToString().PadLeft(3) + " ");
                System.out.print(padLeft(""+i, 3));
                for (int j = 0; j < data[i].length; ++j) {
                    if (data[i][j] >= 0.0) System.out.print(" ");
//                    System.out.println(data[i][j].ToString("F" + decimals) + " ");
                    System.out.print(String.format(java.util.Locale.US,"%."+decimals+"f", data[i][j]) + " ");
                }
                System.out.println("");
            }
            System.out.println("===================");
        }
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

}
