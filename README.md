# Spark GDB

In the wake of the unpredictable future of User Defined Types (UDT), this is a hasty minimalist re-implementation of the [spark-gdb](https://github.com/mraad/spark-gdb) project, in such that the content of a [File GeoDatabase](https://www.esri.com/news/arcuser/0309/files/9reasons.pdf) can be mapped to a *read-only* [Spark DataFrame](https://spark.apache.org/docs/latest/sql-programming-guide.html).  It is minimalist as it only supports features with simple geometries (for now :-) with no M or Z.
In the previous implementation, a `GeometryType` was defined using the UDT framework. However in this implementation, points are stored in a field with two sub-fields x and y.  ~~Polylines and polygons are stored as a string in the [Esri JSON format](https://developers.arcgis.com/documentation/common-data-types/geometry-objects.htm).  It is not the most efficient format, but will make the interoperability with the [ArcGIS API for Python](https://developers.arcgis.com/python/) a bit seamless.~~ Polylines and Polygons shapes are stored as two sub fields, `parts` and `coords`. Parts is an array of integers, where the values are the number of points in the part. Coords is an array of doubles, where the values are a sequence of x,y pairs.

Note: This implementation does **not** support compressed file geo databases.

## Building the project

The build is based on [sbt](https://www.scala-sbt.org/):

```bash
sbt assembly
```

This will create an uber jar in the `target/scala-2.11` folder.

## Usage

The best demonstration of the usage of this implementation is with [PySpark DataFrames](https://docs.databricks.com/spark/latest/dataframes-datasets/introduction-to-dataframes-python.html) and in conjunction with the [ArcGIS API for Python](https://developers.arcgis.com/python/).

Create a Python 3 [conda](https://conda.io/docs/) environment:

```bash
conda create --name arcgis
source activate arcgis
conda install -c esri arcgis
conda install matplotlib
```

Assuming that the environment variable `SPARK_HOME` points to the location of a Spark installation, start a Jupyter notebook that is backed by PySpark:

```bash
export PATH=${SPARK_HOME}/bin:${PATH}
export PYSPARK_DRIVER_PYTHON=jupyter
export PYSPARK_DRIVER_PYTHON_OPTS='notebook --NotebookApp.iopub_data_rate_limit=10000000'
pyspark\
 --master local[*]\
 --num-executors 1\
 --driver-memory 16G\
 --executor-memory 16G\
 --packages com.esri:webmercator_2.11:1.3\
 --jars target/scala-2.11/FileGDB-0.2.jar
```

Check out the [Broadcast](Broadcast.ipynb) and [Countries](Countries.ipynb) example notebooks.

Here is yet another example in Scala:

```scala
val path = "World.gdb"
val name = "Countries"

val spark = SparkSession.builder().getOrCreate()
try
{
    spark
      .read
      .gdb(path, name)
      .createTempView(name)

    spark
      .sql(s"select CNTRY_NAME,SQKM from $name where SQKM < 10000.0 ORDER BY SQKM DESC LIMIT 10")
      .collect()
      .foreach(println)
}
finally
{
    spark.stop()
}
```

## TODO

- Write test cases !!!
- ~~Save geometry as a struct(type,xmin,ymin,xmax,ymax,parts,coords)~~
- Add the option to skip reading the geometry

### References

- https://github.com/minrk/findspark
- https://blog.sicara.com/get-started-pyspark-jupyter-guide-tutorial-ae2fe84f594f
- https://github.com/maxpoint/spylon
- https://github.com/jupyter-scala/jupyter-scala#quick-start
