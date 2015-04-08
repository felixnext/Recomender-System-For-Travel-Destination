package core

/**
 * Represents a location object returned from elasticsearch. 
 * Description, coordinates and metadata is stored within the object.
 */
case class ElasticLocationDoc(var title: Option[String] = None,var  country: Option[List[String]] = None,var  someAs: Option[List[String]] = None,
                    var  paragraphTexts: Option[List[String]] = None,var  populationTotal: Option[Int] = None,
                    var areaTotal: Option[Double] = None,var  climate: Option[Map[String,String]] = None,var  index: Option[String] = None,
                    var  id: Option[Int] = None,var  score: Option[Double] = None, var paragraphNames :Option[List[String]] = None )
