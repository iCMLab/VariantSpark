package au.csiro.variantspark.input.generate

import org.apache.commons.math3.random.GaussianRandomGenerator
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions

import au.csiro.pbdava.ssparkle.spark.SparkUtils.withBroadcast
import au.csiro.variantspark.input.FeatureSource
import au.csiro.variantspark.input.LabelSource
import breeze.linalg.DenseVector
import it.unimi.dsi.util.XorShift1024StarRandomGenerator

class EfectLabelGenerator(featureSource:FeatureSource)(zeroLevel:Int, val noiseSigma:Double, 
      effects:Map[String,Double], seed:Long = 13L) extends LabelSource {
  
  
  def logistic(d:Double) = 1.0 / (1.0 + Math.exp(-d))
  
  def getLabels(labels:Seq[String]):Array[Int] = {
    val nSamples = labels.size
    val rng = new XorShift1024StarRandomGenerator(seed)
    // this is actually very simple
    
    // generate continous variable with provided coefcients
    // then use logit transform to get class probabilties
    
    
    // ok so draw initial effect from a normal distribution
    // N(0,noiseSigma).
    // let's assume the number of incluential factors is small
    // so
    
    val influentialVariablesData = withBroadcast(featureSource.features.sparkContext)(effects){ br_effects =>
      featureSource.features.filter(f => br_effects.value.contains(f.label)).map(f => (f.label, f.toVector.values)).collectAsMap()
    }
          
    val gs = new GaussianRandomGenerator(rng)
    val globalNormalizer = DenseVector.fill(nSamples, zeroLevel.toDouble)
    val continousEffects = effects.foldLeft(DenseVector.fill[Double](nSamples)(gs.nextNormalizedDouble() *noiseSigma)) { case (a, (vi, e)) =>
      // in essence apply the effect function to the input data
      val additiveEffect = DenseVector(influentialVariablesData(vi).toArray) - globalNormalizer
      additiveEffect*=2*e
      a+=additiveEffect
    }
    
    //influentialVariablesData.foreach(println)
    val probs = continousEffects.map(logistic)
    //println(continousEffects)
    //println(probs)
    val classes = probs.map(c => if (rng.nextDouble() < c) 1 else 0)
    //println(classes)    
    // print out correlation of variables
    //val output = classes.toArray.map(_.toDouble)
    //val correlationCalc = new PearsonsCorrelation()
    //effects.map { case (v,e) => (v, correlationCalc.correlation(output, influentialVariablesData(v).toArray)) }.foreach(println)    
    classes.toArray
    
  }
  
}


object EfectLabelGenerator {
  def apply(featureSource:FeatureSource)(zeroLevel:Int, noiseSigma:Double, effects:Map[String,Double], seed:Long = 13L) = new EfectLabelGenerator(featureSource)(zeroLevel, noiseSigma, effects, seed)
}
