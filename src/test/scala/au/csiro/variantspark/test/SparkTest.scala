package au.csiro.variantspark.test

import org.apache.hadoop.fs.FileSystem

trait SparkTest {
  implicit val sc = TestSparkContext.sc
}