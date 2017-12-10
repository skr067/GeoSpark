package org.datasyslab.geosparksql

import org.apache.log4j.{Level, Logger}
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.SparkSession
import org.datasyslab.geospark.serde.GeoSparkKryoRegistrator
import org.datasyslab.geosparksql.utils.GeoSparkSQLRegistrator
import org.scalatest.{BeforeAndAfterAll, FunSpec}

class constructorTestScala extends FunSpec with BeforeAndAfterAll with GeoSparkSqlTestBase {

	implicit lazy val sparkSession = {
    var tempSparkSession = SparkSession.builder().config("spark.serializer",classOf[KryoSerializer].getName).
      config("spark.kryo.registrator", classOf[GeoSparkKryoRegistrator].getName).
      master("local[*]").appName("readTestScala").getOrCreate()
		val sc = tempSparkSession.sparkContext
		Logger.getLogger("org").setLevel(Level.WARN)
		Logger.getLogger("akka").setLevel(Level.WARN)
    tempSparkSession
	}


	override def afterAll(): Unit = {
    GeoSparkSQLRegistrator.dropAll()
    sparkSession.stop
	}

	describe("GeoSpark-SQL Constructor Test") {

    GeoSparkSQLRegistrator.registerAll(sparkSession.sqlContext)

		val resourceFolder = System.getProperty("user.dir")+"/src/test/resources/"

    val mixedWktGeometryInputLocation = resourceFolder + "county_small.tsv"
    val plainPointInputLocation = resourceFolder + "testpoint.csv"
    val shapefileInputLocation = resourceFolder + "shapefiles/polygon"
    val csvPointInputLocation = resourceFolder + "arealm.csv"
    val geoJsonGeomInputLocation = resourceFolder + "testPolygon.json"

    it("Passed ST_Point")
    {
      var pointCsvDF = sparkSession.read.format("csv").option("delimiter",",").option("header","false").load(plainPointInputLocation)
      pointCsvDF.createOrReplaceTempView("pointtable")
      var pointDf = sparkSession.sql("select ST_Point(cast(pointtable._c0 as Decimal(24,20)), cast(pointtable._c1 as Decimal(24,20)), \"myPointId\") as arealandmark from pointtable")
      assert(pointDf.count()==1000)
    }

    it("Passed ST_PointFromText")
    {
      var pointCsvDF = sparkSession.read.format("csv").option("delimiter",",").option("header","false").load(csvPointInputLocation)
      pointCsvDF.createOrReplaceTempView("pointtable")
      pointCsvDF.show()
      var pointDf = sparkSession.sql("select ST_PointFromText(pointtable._c0,',', \"myPointId\") as arealandmark from pointtable")
      assert(pointDf.count()==121960)
    }

    it("Passed ST_GeomFromWKT")
    {
      var polygonWktDf = sparkSession.read.format("csv").option("delimiter",",").option("header","false").load(mixedWktGeometryInputLocation)
      polygonWktDf.createOrReplaceTempView("polygontable")
      polygonWktDf.show()
      var polygonDf = sparkSession.sql("select ST_GeomFromWKT(polygontable._c0) as countyshape from polygontable")
      assert(polygonDf.count()==100)
    }

    it("Passed ST_GeomFromGeoJSON")
    {
      var polygonJsonDf = sparkSession.read.format("csv").option("delimiter","\t").option("header","false").load(geoJsonGeomInputLocation)
      polygonJsonDf.createOrReplaceTempView("polygontable")
      polygonJsonDf.show()
      var polygonDf = sparkSession.sql("select ST_GeomFromGeoJSON(polygontable._c0) as countyshape from polygontable")
      polygonDf.show()
      assert(polygonDf.count()==1000)
    }
	}
}
