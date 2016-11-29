package main.experiments;

import main.distance.Distance;
import main.distance.TotalVariation;
import main.models.frequency.FrequencyTable;
import org.apache.commons.lang3.ArrayUtils;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;

/**
 * Created by LoongKuan on 31/07/2016.
 **/
public abstract class Experiment {
    protected Map<int[], ArrayList<ExperimentResult>> resultMap;
    protected Instance sampleInstance;
    protected abstract ArrayList<ExperimentResult> getResults(FrequencyTable model1, FrequencyTable model2, int[] attributeSubset, double sampleScale);

    private int nAttributesActive;
    // TODO: Utilise given info on active covariates and class
    private int[] attributeIndices;
    private int[] classIndices;

    public Experiment(Instances instances1, Instances instances2, int nAttributesActive, int[] attributeIndices, int[] classIndices) {
        this(instances1, instances2, nAttributesActive, attributeIndices, classIndices, -1, 1);
    }

    public Experiment(Instances instances1, Instances instances2, int nAttributesActive, int[] attributeIndices, int[] classIndices, double sampleScale, int nTests) {
        // Generate base models for each data set
        FrequencyTable model1 = new FrequencyTable(instances1, nAttributesActive, attributeIndices);
        FrequencyTable model2 = new FrequencyTable(instances2, nAttributesActive, attributeIndices);

        // Generate union set of all instances in both data sets
        Instances allInstances = new Instances(instances1);
        allInstances.addAll(instances2);

        // Store needed metadata
        this.sampleInstance = allInstances.firstInstance();
        this.nAttributesActive = nAttributesActive;

        // Get number of combinations from choosing nAttributesActive of attributes from instances there are
        int nCombination = nCr(attributeIndices.length, nAttributesActive);
        resultMap = new HashMap<>();
        for (int i = 0; i < nCombination; i++) {
            System.out.print("\rRunning experiment " + (i + 1) + "/" + nCombination);
            // Get attribute subset
            int[] attributeSubset = getKthCombination(i, attributeIndices, nAttributesActive);

            ArrayList<ArrayList<ExperimentResult>> results = new ArrayList<>();
            for (int j = 0; j < nTests; j++) {
                ArrayList<ExperimentResult> tmpRes = getResults(model1, model2, attributeSubset, sampleScale);
                for (int k = 0; k < tmpRes.size(); k++) {
                    if (results.size() <= k) results.add(new ArrayList<>());
                    results.get(k).add(tmpRes.get(k));
                }
            }
            ArrayList<ExperimentResult> finalAveragedResults = new ArrayList<>();
            for (int k = 0; k < results.size(); k++) {
                if (!results.get(k).isEmpty()) {
                    finalAveragedResults.add(new ExperimentResult(results.get(k)));
                }
            }
            resultMap.put(attributeSubset, finalAveragedResults);
        }
        System.out.print("\n");
        this.resultMap = sortByValue(this.resultMap);
        this.attributeIndices = attributeIndices;
        this.classIndices = classIndices;
    }

    public String[][] getResultTable() {
        return this.getResultTable(0, "*");
    }

    protected String[][] getResultTable(int classIndex, String className) {
        int[][] attributeSubSets = this.resultMap.keySet().toArray(new int[this.resultMap.size()][this.nAttributesActive]);
       String[][] results = new String[attributeSubSets.length][9];
        for (int i = 0; i < attributeSubSets.length; i++) {
            ExperimentResult currentResult = this.resultMap.get(attributeSubSets[i]).get(classIndex);
            results[i][0] = Double.toString(currentResult.actualResult);
            results[i][1] = Double.toString(currentResult.mean);
            results[i][2] = Double.toString(currentResult.sd);
            results[i][3] = Double.toString(currentResult.maxDist);
            results[i][4] = "";
            results[i][5] = Double.toString(currentResult.minDist);
            results[i][6] = "";
            results[i][7] = "";
            for (int j = 0; j < attributeSubSets[i].length; j++) {
                results[i][7] += this.sampleInstance.attribute(attributeSubSets[i][j]).name() + "_";
            }
            results[i][7] = results[i][7].substring(0, results[i][7].length() - 1);
            results[i][8] = className;
            if (!Double.isInfinite(currentResult.actualResult)) {
                for (int j = 0; j < attributeSubSets[i].length; j++) {
                    int attributeIndex = attributeSubSets[i][j];
                    String minVal = Double.isNaN(currentResult.minValues[attributeIndex]) || (int)currentResult.minValues[attributeIndex] < 0 ? "*" :
                            this.sampleInstance.attribute(attributeIndex).value((int)currentResult.minValues[attributeIndex]);
                    String maxVal = Double.isNaN(currentResult.minValues[attributeIndex]) || (int)currentResult.maxValues[attributeIndex] < 0 ? "*" :
                            this.sampleInstance.attribute(attributeIndex).value((int)currentResult.maxValues[attributeIndex]);
                    results[i][4] += this.sampleInstance.attribute(attributeIndex).name() + "=" + maxVal + "_";
                    results[i][6] += this.sampleInstance.attribute(attributeIndex).name() + "=" + minVal + "_";
                }
                // Trim last underscore
                results[i][4] = results[i][4].substring(0, results[i][4].length() - 1);
                results[i][6] = results[i][6].substring(0, results[i][6].length() - 1);
            }
            else {
                results[i][4] = "NA";
                results[i][6] = "NA";
            }
        }
        return results;
    }

    private static int[] getKthCombination(int k, int[] elements, int choices) {
        if (choices == 0) return new int[]{};
        else if (elements.length == choices) return  elements;
        else {
            int nCombinations = nCr(elements.length - 1, choices - 1);
            if (k < nCombinations) return ArrayUtils.addAll(ArrayUtils.subarray(elements, 0, 1),
                    getKthCombination(k, ArrayUtils.subarray(elements, 1, elements.length), choices - 1));
            else return getKthCombination(k - nCombinations, ArrayUtils.subarray(elements, 1, elements.length), choices);
        }
    }

    private static int nCr(int n, int r) {
        if (r >= n /2) r = n - r;
        int ans = 1;
        for (int i = 1; i <= r; i++) {
            ans *= n - r + i;
            ans /= i;
        }
        return ans;
    }

    private static Map<int[], ArrayList<ExperimentResult>> sortByValue( Map<int[], ArrayList<ExperimentResult>> map ) {
        List<Map.Entry<int[], ArrayList<ExperimentResult>>> list = new LinkedList<>(map.entrySet());
        Collections.sort( list, new Comparator<Map.Entry<int[], ArrayList<ExperimentResult>>>() {
            public int compare( Map.Entry<int[], ArrayList<ExperimentResult>> o1, Map.Entry<int[], ArrayList<ExperimentResult>> o2 )
            {
                double value = o1.getValue().get(0).actualResult - o2.getValue().get(0).actualResult;
                if (value == 0.0f) return 0;
                else if(value < 0.0f) return -1;
                else return 1;
            }
        } );

        Map<int[], ArrayList<ExperimentResult>> result = new LinkedHashMap<>();
        for (Map.Entry<int[], ArrayList<ExperimentResult>> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }

}
