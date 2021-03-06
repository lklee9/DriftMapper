package run;

import global.DriftMeasurement;
import analyse.timeline.NaiveMovingWindow;
import analyse.timeline.NaiveWindow;
import analyse.timeline.TimelineAnalysis;
import models.Model;
import models.frequency.FrequencyMaps;
import weka.core.Instances;

import java.util.ArrayList;

/**
 * Created by loongkuan on 12/12/2016.
 */
public class DriftTimeline extends main{

    // TODO : Allow different window size and drift type and name result file accordingly
    public static void DriftTimeline(String resultFolder, Instances instances, int[] windowSizes, int[] subsetLengths, boolean interval) {
        testAllWindowSizeSubsetLength(instances, windowSizes, subsetLengths, resultFolder, interval);
    }


    public static void DriftTimeline(String folder, String[] files) {
        //String folder = "synthetic_5Att_5Val";
        //files = new String[]{"stream", "n1000000_m0.7_posterior"};
        //files = new String[]{"stream", "synthetic_5Att_5Val/n1000000_none"};

        //String folder = "data_uni_antwerp";
        //files = new String[]{"stream", "water_2016"};

        folder = "";
        files = new String[]{"elecNormNew"};

        //String folder = "train_seed";
        //files = new String[]{"all", "20130419", "20131129"};

        //Instances[] dataSets = loadPairData(files[1], files[2]);
        //Instances allInstances = loadAnyDataSet("./datasets/" + folder + "/" + files[1] + ".arff");
        //allInstances.addAll(loadAnyDataSet("./datasets/" + folder + "/" + files[2] + ".arff"));
        //files[1] = files[1] + "_" + files[2];

        // Load all data sets given
        Instances allInstances = loadAnyDataSet("./datasets/" + folder + "/" + files[0] + ".arff");
        String filename_comb = files[0];
        for (int i = 1; i < files.length; i++) {
            allInstances.addAll(loadAnyDataSet("./datasets/" + folder + "/" + files[i] + ".arff"));
            filename_comb += "_" + files[i];
        }
        String filepath = createFilePath(new String[]{"./data_out", folder, filename_comb, "stream"});

        //TODO: Be able to measure different types of drift and name accordingly
        //testAllWindowSize(allInstances, new int[]{100, 500, 1000, 5000, 10000}, filepath);
        testAllWindowSizeSubsetLength(allInstances,
                getAllWindowSize(allInstances), getAllAttributeSubsetLength(allInstances), filepath, true);
    }

    private static int[] getAllWindowSize(Instances instances) {
        int currentSize = 1000;
        ArrayList<Integer> allSizes  = new ArrayList<>();
        while (currentSize < instances.size()) {
            allSizes.add(currentSize);
            currentSize = Integer.toString(currentSize).charAt(0) == '1' ? currentSize * 5 : currentSize * 2;
        }
        int[] returnSizes = new int[allSizes.size()];
        for(int i = 0; i < allSizes.size(); i++) returnSizes[i] = allSizes.get(i);
        return returnSizes;
    }

    private static void testAllWindowSizeSubsetLength(Instances instances,
                                                      int[] windowSizes, int[] subsetLength, String folder, boolean interval) {
        for (int size : windowSizes) {
            for (int length : subsetLength) {
                runExperiment(instances, length, size, folder, interval);
            }
        }
    }

    // TODO: Automate testing with different windows
    private static void runExperiment(Instances instances, int attributeSubsetLength, int windowSize, String resultFolder, boolean interval) {
        int[] attributeIndices = getAttributeIndicies(instances);

        Model referenceModel = new FrequencyMaps(instances, attributeSubsetLength, attributeIndices);
        TimelineAnalysis streamingData;
        if (interval) {
            streamingData = new NaiveWindow(windowSize, referenceModel, DriftMeasurement.values());
        }
        else {
            streamingData = new NaiveMovingWindow(windowSize, referenceModel, DriftMeasurement.values());
        }

        int percentage = 0;
        for (int i = 0; i < instances.size(); i++) {
            if (percentage != (i * 100) / instances.size() ) {
                percentage = (i * 100 )/ instances.size();
                System.out.print("\r" + percentage + "% of instance processed");
            }
            /*
            System.out.print("\r" + i + " out of " + instances.size() + " instance processed");
            */
            streamingData.addInstance(instances.get(i));
        }

        System.out.println("\rDone test of Window Size = " + windowSize + ", Subset Length = " + attributeSubsetLength);

        for (DriftMeasurement driftMeasurement : DriftMeasurement.values()) {
            String file = resultFolder + "/" + driftMeasurement.name() + "_" + windowSize + "_" + attributeSubsetLength + ".csv";
            streamingData.writeResultsToFile(file, driftMeasurement);
        }
    }
}
