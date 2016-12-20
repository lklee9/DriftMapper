package main.run;

import main.DriftMeasurement;
import main.analyse.timeline.NaiveChunks;
import main.models.Model;
import main.models.frequency.FrequencyMaps;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

/**
 * Created by loongkuan on 20/12/2016.
 */
public class DriftTimelineChunks {

    public static void DriftTimelineChunks(String resultFolder, Instances[] allInstances,
                                           int groupAttribute, int[] groupSizes, int[] subsetLengths) {
        if (groupAttribute >= 0) {
            ArrayList<Instances> groupedInstances = new ArrayList<>();
            double prevAttributeValue = -1;
            for (Instances instances : allInstances) {
                for (Instance instance : instances) {
                    if (instance.value(groupAttribute) != prevAttributeValue) {
                        groupedInstances.add(new Instances(allInstances[0], 0));
                        prevAttributeValue = instance.value(groupAttribute);
                    }
                    groupedInstances.get(groupedInstances.size() - 1).add(instance);
                }
            }
            allInstances = groupedInstances.toArray(new Instances[groupedInstances.size()]);
        }

        for (int groupSize : groupSizes) {
            for (int subsetLength : subsetLengths) {
                runExperiment(allInstances, subsetLength, groupSize, resultFolder);
            }
        }
    }

    private static void runExperiment(Instances[] allInstances, int subsetLength, int groupSize, String resultFolder) {
        // Create new chunks of groupSize
        Instances[] newAllInstances = new Instances[(allInstances.length) / groupSize];
        for (int i = 0; i < (allInstances.length - (allInstances.length % groupSize)); i++) {
            if (i % groupSize == 0) {
                newAllInstances[i / groupSize] = new Instances(allInstances[i]);
            }
            else {
                newAllInstances[i / groupSize].addAll(allInstances[i]);
            }
        }
        int[] attributeIndices = new int[newAllInstances[0].numAttributes() - 1];
        for (int i = 0; i < newAllInstances[0].numAttributes() - 1; i++) attributeIndices[i] = i;

        Model referenceModel = new FrequencyMaps(newAllInstances[0], subsetLength, attributeIndices);
        NaiveChunks naiveChunks = new NaiveChunks(newAllInstances, DriftMeasurement.values(), referenceModel);

        System.out.println("\rDone test of Subset Length = " + subsetLength + " and chunk size of " + groupSize);

        for (DriftMeasurement driftMeasurement : DriftMeasurement.values()) {
            String file = resultFolder + "/" + driftMeasurement.name() + "_" + groupSize + "_" + subsetLength + ".csv";
            naiveChunks.writeResultsToFile(file, driftMeasurement);
        }
    }
}
