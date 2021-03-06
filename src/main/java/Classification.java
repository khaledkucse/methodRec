/*
Author: C M Khaled Saifullah
Software Research Lab
University of Saskatchewan, Canada
Email: khaled.saifullah@usask.ca

Project Description: This is the deelearning project for recommending argument of API usages using recurrent neural network.
The project use deeplearning4j tools developed by skymind group for reccomending top 1,3,5 and 10 arguments for a function call.
I have considered all the methods of javax.swing package and uses 6 sample project of github that uses javax.swing package.



*/


import Util.ReportWriting;
import Util.StatCalculation;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class Classification
{


    /*  arg[0] is the path of the output file
        arg[1] is the number of input or number of features
        arg[2] is the number of output or number of label
        arg[3] is the path of the training set
        arg[4] is the path of the test set

    */
    public static void LSTMBasedRNN(String[] args) throws IOException, InterruptedException
    {
        int lstmLayerSize = 1900;					//Number of units in each GravesLSTM layer
        int batchsize = 50;						    //Size of mini batch to use when  training
        int nEpoches = 500;							//Total number of training epoch

        // Output File Location
        String outputPathname = args[0];


        //Get a DataSetIterator that handles vectorization of text into something we can use to train
        // our GravesLSTM network.
        int nIn = Integer.parseInt(args[1])-1;
        int nOut = Integer.parseInt(args[2]);


        //load train data
        RecordReader rr = new CSVRecordReader();
        rr.initialize(new FileSplit(new File(args[3])));
        DataSetIterator trainset = new RecordReaderDataSetIterator(rr,batchsize,0,nOut);


        //load test data
        RecordReader rrTest = new CSVRecordReader();
        rrTest.initialize(new FileSplit(new File(args[4])));
        DataSetIterator testset = new RecordReaderDataSetIterator(rrTest,batchsize,0,nOut);



        //Set up network configuration:
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(1)
                .learningRate(0.01)
                .seed(123)
                .regularization(true)
                .l2(0.001)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.RMSPROP)
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(nIn).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(1, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(2, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(3, new GravesLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
                        .activation(Activation.TANH).build())
                .layer(4, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation(Activation.SOFTMAX)        //MCXENT + softmax for classification
                        .nIn(lstmLayerSize).nOut(nOut).build())
                .pretrain(false).backprop(true)
                .build();

        //Setting up the network
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(10));


        //Train the dataset
        for (int i = 0;i < nEpoches; i++)
        {
            net.fit(trainset);
        }

        System.out.println("Evaluate Model......");



        Evaluation evaluation= new Evaluation(nOut);


        // Store the probability of the predicted value along with the label
        ArrayList<Double> predictedvalue = new ArrayList<Double>();
        ArrayList<Integer> labelvalue = new ArrayList<Integer>();


        while(testset.hasNext())
        {
            // Testing for Top-1 Reccomendation
            DataSet temp = testset.next();
            INDArray features = temp.getFeatures();
            INDArray labels = temp.getLabels();
            INDArray predicted = net.output(features,false);
            evaluation.eval(labels,predicted);

            //Store the predicted probability
            for(int i=0;i<predicted.length();i++)
            {
                predictedvalue.add(predicted.getDouble(i));

            }

            //Store the label
            for(int i=0; i<labels.length();i++)
            {
                for (int j = i; j < (i + nOut); j++)
                {
                    if (labels.getDouble(j) > 0)
                        labelvalue.add(j - i);
                }
                i = i + nOut - 1;
            }

        }

        //Print out the statistics for the Top-1 reccomendation`
        //System.out.println(evaluation.stats());
        String report = evaluation.stats()+"\n";



        //Printing out the Top-3,5,10 Recommendation's statistics
        StatCalculation stc = new StatCalculation(predictedvalue,labelvalue,nOut);
        report = report + stc.stats(3);
        report = report + stc.stats(5);
        report = report + stc.stats(10);


        // FIle output Command
        ReportWriting rw = new ReportWriting(outputPathname);
        rw.showReport(report,args[4]);


    }
}
