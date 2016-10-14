package main.experiments.types;

import main.distance.Distance;
import main.distance.TotalVariation;
import main.experiments.Experiment;
import main.experiments.ExperimentResult;
import main.models.NaiveMatrix;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

/**
 * Created by LoongKuan on 31/07/2016.
 **/
public class CovariateDistance extends Experiment {

    public CovariateDistance(Instances instances1, Instances instances2, int nAttributesActive, int[] attributeIndices, int sampleSize, int nTests){
        // List of 0 to n where n is the number of attributes
        super(instances1, instances2, nAttributesActive, attributeIndices, new int[]{}, sampleSize, nTests);
    }

    @Override
    public ArrayList<ExperimentResult> getResults(NaiveMatrix model1, NaiveMatrix model2, Instances allInstances, double sampleScale) {
        double[] p = new double[allInstances.size()];
        double[] q = new double[allInstances.size()];
        double[] separateDistance = new double[allInstances.size()];
        double[][] instanceValues = new double[allInstances.size()][allInstances.numAttributes()];
        for (int i = 0; i < allInstances.size(); i++) {
            p[i] = model1.findPv(allInstances.get(i));
            q[i] = model2.findPv(allInstances.get(i));
            separateDistance[i] = this.distanceMetric.findDistance(new double[]{p[i]}, new double[]{q[i]});
            instanceValues[i] = allInstances.get(i).toDoubleArray();
        }
        double finalDistance = this.distanceMetric.findDistance(p, q) * sampleScale;
        ExperimentResult finalResult = new ExperimentResult(finalDistance, separateDistance, instanceValues);
        ArrayList<ExperimentResult> returnResults = new ArrayList<>();
        returnResults.add(finalResult);
        return returnResults;
    }

    @Override
    public String[][] getResultTable() {
        return this.getResultTable(0, "NA");
    }
}
